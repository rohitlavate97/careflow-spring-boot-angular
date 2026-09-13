# Concurrency Lab 4: Bed Allocation & Double-Occupancy Prevention

## Objective
Demonstrate how CareFlow prevents race conditions in hospital inpatient admissions and bed allocation (§28, §57 Lab 4, §92):
1. Preventing **double-bed allocation / concurrent room collision** when two admission coordinators simultaneously assign the same available physical bed to two distinct patients.
2. Demonstrating database-first concurrency enforcement using **pessimistic row-level locking** (`SELECT ... FOR UPDATE`) paired with database integrity constraints.

---

## 1. Problem
During peak intake (e.g. emergency surges, surgical post-op arrivals, or morning admitting shifts), multiple admitting officers access the bed management dashboard simultaneously.
If *Bed #ICU-101-A* is flagged as `AVAILABLE`, two independent clerks (or automated admission queues) may attempt to admit Patient Alice and Patient Bob into *Bed #ICU-101-A* at the exact same millisecond.

---

## 2. Race Condition
```text
Time   Thread A (Patient Alice)               Thread B (Patient Bob)
 │
 t1    SELECT * FROM beds WHERE id='bed-1'    SELECT * FROM beds WHERE id='bed-1'
       status is AVAILABLE                   status is AVAILABLE
 │
 t2    Validate: AVAILABLE -> OK              Validate: AVAILABLE -> OK
 │
 t3    UPDATE beds SET status='OCCUPIED'
       INSERT INTO admissions (Alice)
       COMMIT;
 │
 t4                                           UPDATE beds SET status='OCCUPIED'
                                              INSERT INTO admissions (Bob)
                                              COMMIT;
 ▼
```
Both admissions succeed. Two patients arrive at the same physical hospital bed with valid admission paperwork.

---

## 3. Root Cause
1. **Non-Atomic Read-Modify-Write**:
   Reading bed availability and writing `status = 'OCCUPIED'` occurs across separate SQL statements.
2. **Missing Concurrency Synchronization**:
   Without row-level locking at the database level, the database read in Thread B operates on a snapshot taken before Thread A committed, leading to a phantom available state.

---

## 4. Incorrect Solutions

### Incorrect Solution A: Application-Level In-Memory Locks (Java `synchronized` / `ReentrantLock`)
*Why it fails:*
In modern hospital deployments, the backend runs across multiple load-balanced application instances or containers. In-memory Java locks only synchronize threads within a single JVM process. Two requests hitting different server nodes will still collide in the database.

### Incorrect Solution B: Optimistic Locking (`@Version`) Alone
*Why it is suboptimal:*
While optimistic locking prevents data overwrites by throwing `OptimisticLockException`, in a high-stress hospital emergency department, users receive cryptic "Row modified by another user" errors. The user experience is disjointed and requires full form re-entry. Pessimistic row locking cleanly serializes competing admission requests at the database engine level with deterministic rejection of the second entrant.

---

## 5. Correct Solution: Database Pessimistic Row Locking (`SELECT ... FOR UPDATE`)

CareFlow utilizes JPA pessimistic row-level locking directly on the `Bed` entity:

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT b FROM Bed b WHERE b.id = :id")
Optional<Bed> findByIdForUpdate(@Param("id") String id);
```

### Execution Flow:
```text
Concurrent Admission Requests (Clerk A for Alice & Clerk B for Bob)
                             │
                             ▼
     [JPA PESSIMISTIC_WRITE Lock (SELECT ... FOR UPDATE on Bed)]
          │                                       │
          ▼                                       ▼
   Acquires Row Lock                       Blocks on Row Lock
   Reads status = AVAILABLE                (Waits in DB engine queue)
   Updates status to OCCUPIED                     │
   Inserts Admission for Alice                    │
   Commits Transaction                            │
          │                                       │
          ▼                                       ▼
   Releases Row Lock ──────────────────────> Acquires Row Lock
                                             Reads fresh status = OCCUPIED
                                             Status == AVAILABLE Check FAILS!
                                             Throws BedNotAvailableException (409)
                                                  │
                                                  ▼
                                             Transaction Rollback
                                             Clerk B informed: "Bed already occupied"
```

---

## 6. Database Behavior
* In MySQL InnoDB, `SELECT ... FOR UPDATE` acquires an exclusive `X` row lock on the target primary key row in `beds`.
* Any competing transaction attempting to execute `SELECT ... FOR UPDATE` or `UPDATE` on that same row is suspended in `LOCK WAIT` state until the holding transaction issues `COMMIT` or `ROLLBACK`.
* Upon release, the second transaction reads the committed row state (`status = 'OCCUPIED'`) under standard Read Committed / Repeatable Read isolation.

---

## 7. Transaction Behavior
1. `AdmissionServiceImpl.admitPatient(...)` is annotated with `@Transactional`.
2. Step 1: Destination bed is acquired via `bedRepository.findByIdForUpdate(bedId)`.
3. Step 2: Verification `if (bed.getStatus() != BedStatus.AVAILABLE) throw new BedNotAvailableException(...)`.
4. Step 3: Bed status set to `OCCUPIED`.
5. Step 4: Admission aggregate instantiated and persisted.
6. Step 5: Initial `BedTransferRecord` inserted for audit trail.
7. Step 6: Commit transaction, releasing the lock.

---

## 8. Test Verification
The multi-threaded concurrency test in `BedAllocationConcurrencyTest.java` executes:
* 5 concurrent threads attempting to admit different patients into the exact same physical bed simultaneously using `CountDownLatch` and `ExecutorService`.
* **Assertion 1**: Exactly 1 thread succeeds (returns HTTP 201 Created).
* **Assertion 2**: Exactly 4 threads fail with `BedNotAvailableException` (HTTP 409 Conflict).
* **Assertion 3**: Exactly 1 active admission exists for the bed in the database.
* **Assertion 4**: The bed status in the database is strictly `OCCUPIED`.
