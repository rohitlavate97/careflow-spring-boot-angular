# Concurrency Lab 3: Pharmacy Stock Race Conditions & Pessimistic Decrements

## Objective
Demonstrate how CareFlow prevents race conditions in outpatient pharmacy dispensing (§25, §26, §92):
1. Preventing **overselling / negative stock** when two or more pharmacists simultaneously dispense the last remaining units of a medication batch.
2. Demonstrating database-first concurrency enforcement using **pessimistic row locking** (`SELECT ... FOR UPDATE`) paired with database check constraints (`CHECK (quantity_available >= 0)`).

---

## 1. The Scenario

Consider an inventory batch for *Amoxicillin 500mg (Batch #AMX-2026-01)* with:
```text
Available stock = 1 unit
```

At peak dispensary hours:
* **Pharmacist A (Counter 1)** clicks "Dispense 1 Unit" for Patient Alice.
* **Pharmacist B (Counter 2)** clicks "Dispense 1 Unit" for Patient Bob at the exact same millisecond.

---

## 2. The Naive Approach (And why it fails)

In an uncoordinated application:
1. **Thread A (Pharmacist A)** reads `quantity_available = 1`. Check passes (`1 >= 1`).
2. **Thread B (Pharmacist B)** reads `quantity_available = 1` before Thread A commits. Check passes (`1 >= 1`).
3. **Thread A** updates: `quantity_available = 1 - 1 = 0`. Commits.
4. **Thread B** updates: `quantity_available = 1 - 1 = 0` (or `0 - 1 = -1` with atomic math). Commits.

### Result:
* **Two patients** walk away with receipts for a medication when only one unit was physically present in the bin.
* Stock discrepancy in inventory audit, potential regulatory violation, and delayed treatment for Patient Bob when the physical shelf is empty.

---

## 3. The CareFlow Solution: Two-Layer Concurrency Defense

CareFlow applies a database-first defense in depth:

```text
Concurrent Dispense Requests (Pharmacist A & Pharmacist B)
                            │
                            ▼
   [Layer 1: JPA PESSIMISTIC_WRITE Lock (SELECT ... FOR UPDATE)]
         │                                       │
         ▼                                       ▼
  Acquires Row Lock                       Blocks on Row Lock
  Reads stock = 1                         (Waits in DB queue)
  Decrements stock to 0                          │
  Inserts Dispense Record                        │
  Commits Transaction                            │
         │                                       │
         ▼                                       ▼
  Releases Row Lock ──────────────────────> Acquires Row Lock
                                            Reads fresh stock = 0
                                            Quantity < 1 Check FAILS!
                                            Throws InsufficientInventoryException (409)
                                                 │
                                                 ▼
               [Layer 2: Database Invariant Guard]
               `CHECK (quantity_available >= 0)`
               Aborts any illegal negative mutation
```

### 1. Row-Level Pessimistic Lock (`PESSIMISTIC_WRITE`)
In `PharmacyInventoryBatchRepository`:
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT b FROM PharmacyInventoryBatch b WHERE b.id = :id")
Optional<PharmacyInventoryBatch> findByIdForUpdate(@Param("id") String id);
```
* The first thread to acquire the lock reads `quantityAvailable = 1`, decrements it to `0`, records the dispense event, and commits.
* The second thread blocks until the first transaction finishes. Upon acquiring the lock, it reads the updated state (`quantityAvailable = 0`). The check `quantityAvailable < requestedQuantity` triggers, immediately throwing `InsufficientInventoryException` (HTTP 409 Conflict).

### 2. Database Check Constraint
In `V12__create_prescription_and_pharmacy_tables.sql`:
```sql
CONSTRAINT chk_pharmacy_inventory_non_negative CHECK (quantity_available >= 0)
```
Even if application-level checks were bypassed or flawed, the database engine prohibits negative quantities at the storage layer.

---

## 4. Multi-Threaded Verification Test

Execute the multi-threaded concurrency suite:
```bash
mvn test -Dtest=PharmacyConcurrencyTest
```

### Test Design:
- Initializes an inventory batch with exactly `quantityAvailable = 1`.
- Spawns 2 concurrent threads using `CountDownLatch(1)` as a starting gate to unleash simultaneous dispense invocations.
- Asserts that:
  - Exactly 1 thread completes with HTTP 200 / success.
  - Exactly 1 thread fails with `InsufficientInventoryException`.
  - The final database batch quantity is exactly `0` (never `-1`).
  - Exactly 1 `DispenseRecord` is created.
