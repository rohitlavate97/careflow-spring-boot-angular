# Concurrency Lab 2: Patient Queue Race Conditions & Safe Token Generation

## Objective
Demonstrate how CareFlow prevents race conditions in outpatient queue operations (§21, §92):
1. Preventing **double-calling** when multiple clinicians or receptionists simultaneously call the next patient.
2. Guaranteeing **unique, sequential daily tokens** when multiple receptionists register patients concurrently.

---

## 1. Race Condition A: Simultaneous "Call Next Patient"

### The Scenario
In a busy outpatient department with multiple consultation rooms (e.g. Cardiology), 10 doctors become available and click "Call Next" at the exact same millisecond. 

### The Naive Approach (And why it fails)
1. **Thread 1 (Dr. Strange)** runs: `SELECT * FROM queue_entries WHERE status = 'WAITING' ORDER BY priority, entry_time LIMIT 1`. Result: Patient A.
2. **Thread 2 (Dr. Fleming)** runs the exact same query before Dr. Strange commits: Result: Patient A.
3. Both doctors call Patient A into different consultation rooms simultaneously, leaving subsequent waiting patients unattended and creating chaos in the clinic.

### The CareFlow Solution: Two-Stage Candidate Locking
1. **Candidate Identification**:
   Query top waiting candidate IDs ordered by priority (`EMERGENCY` > `URGENT` > `NORMAL`) and entry time (`entry_time ASC`).
2. **Row-Level Pessimistic Lock**:
   Execute `queueEntryRepository.findByIdForUpdate(candidateId)` with JPA `LockModeType.PESSIMISTIC_WRITE` (`SELECT ... FOR UPDATE`).
3. **State Guard Verification**:
   Inside the transaction, confirm `entry.getStatus() == QueueStatus.WAITING`.
   If true, transition to `CALLED`, record `calledTime` and `doctorId`, and commit.
   If another thread already transitioned the candidate, the loop advances to the next candidate immediately.

---

## 2. Race Condition B: Concurrent Enqueue & Token Allocation

### The Scenario
Five receptionists simultaneously check in patients arriving at outpatient registration for the same department.

### The CareFlow Solution: Department-Level Row Serialization + Unique Constraint
1. **Database-Level Constraint**:
   `CONSTRAINT uk_queue_dept_date_token UNIQUE (department_id, queue_date, token_number)`
   Guarantees that at the storage level, duplicate tokens on the same date for a department are physically impossible.
2. **Pessimistic Serialization**:
   Before querying the daily maximum token, acquire a row lock on the target department: `departmentRepository.findByIdForUpdate(departmentId)`.
   - Serializes token allocation within the same department.
   - Independent departments (e.g. Cardiology vs Oncology) run concurrently without blocking each other.
   - Eliminates constraint violation retries and produces perfectly consecutive, gapless daily tokens (001, 002, 003, ...).

---

## How to run the Lab
Execute the multi-threaded concurrency suite:
```bash
mvn test -Dtest=QueueConcurrencyTest
```

This test uses:
- `ExecutorService` and `CountDownLatch(1)` starting gate releasing 10 concurrent threads simultaneously.
- Asserts that all 10 doctors receive 10 distinct patients (zero duplicate calls).
- Asserts that 5 concurrent enqueues receive 5 unique, collision-free tokens.
