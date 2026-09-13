# Concurrency Lab 5: Payment Idempotency & Duplicate Charge Prevention

## Objective
Demonstrate how CareFlow prevents duplicate payments and race conditions during invoice settlement (§31, §32, §57 Lab 5, §92):
1. Preventing **double-charging / duplicate debit transactions** when a user rapidly double-clicks "Submit Payment", a payment gateway webhook replays events, or a mobile client retries an unacknowledged HTTP request.
2. Demonstrating database-first concurrency enforcement using **Idempotency Keys** paired with unique constraints and **pessimistic row locking** (`SELECT ... FOR UPDATE`).

---

## 1. Problem
Consider an invoice with a balance due of **$150.00**.
Patient Alice clicks "Pay $150.00 with Credit Card".
Due to network lag on mobile or a slow WiFi connection, the browser request hangs for 1.5 seconds.
Alice clicks "Pay Now" a second time, or the browser's automatic HTTP client retries the request with the identical `Idempotency-Key: pay-idemp-98765`.

---

## 2. Race Condition
```text
Time   Thread 1 (Request 1: Key=pay-123)            Thread 2 (Request 2: Key=pay-123)
 │
 t1    Read invoice balance = $150.00               Read invoice balance = $150.00
       Check passes: $150 <= $150                   Check passes: $150 <= $150
 │
 t2    Process card charge: $150.00                 Process card charge: $150.00
 │
 t3    Insert payment: $150.00                      Insert payment: $150.00
       Update invoice: paid=$150, balance=$0        Update invoice: paid=$300, balance=-$150
       COMMIT;                                      COMMIT;
 ▼
```
Alice's credit card is charged **$300.00** instead of $150.00. The invoice balance becomes negative, creating accounting reconciliations issues, card dispute chargebacks, and patient distress.

---

## 3. Root Cause
1. **Lack of Idempotent Request Identification**:
   Treating every incoming HTTP request as a distinct financial operation instead of identifying request replays.
2. **Missing Database-Enforced Key Uniqueness**:
   Without a database unique constraint on the idempotency key, both threads insert separate payment rows.
3. **Unprotected Invoice Balance Mutation**:
   Updating invoice `paid_amount` without serializing concurrent transactions.

---

## 4. Incorrect Solutions

### Incorrect Solution A: In-Memory Set / Distributed Cache Deduplication Alone
*Why it fails:*
Storing processed keys in a Redis or JVM `ConcurrentHashMap` with a TTL is vulnerable to race conditions if the key is checked before being atomically set (`SETNX`), or if network partitions cause cache drops. If Redis restarts or drops keys under eviction policies, duplicate payments bypass the check.

### Incorrect Solution B: Client-Side Button Disabling Only
*Why it fails:*
Disabling the "Pay" button with JavaScript `button.disabled = true` prevents accidental UI double-clicks, but offers zero protection against network retry timeouts, browser page refreshes, automated gateway webhooks, or API consumers (e.g. mobile apps or automated billing scripts).

---

## 5. Correct Solution: Unique Idempotency Key & Pessimistic Invoice Lock

CareFlow combines two database-enforced mechanisms:

1. **Database Unique Constraint on Idempotency Key**:
   ```sql
   CONSTRAINT uk_payments_idempotency_key UNIQUE (idempotency_key)
   ```
2. **Pessimistic Invoice Row Lock (`SELECT ... FOR UPDATE`)**:
   ```java
   @Lock(LockModeType.PESSIMISTIC_WRITE)
   @Query("SELECT i FROM Invoice i WHERE i.id = :id")
   Optional<Invoice> findByIdForUpdate(@Param("id") String id);
   ```

### Execution Flow:
```text
Concurrent Duplicate Payment Requests (Key = pay-abc-123)
                             │
                             ▼
               [Check: Idempotency Key Exists?]
                     /                \
          Yes (Replay)                 No (First time)
              /                         \
    Return existing payment       Acquire PESSIMISTIC_WRITE Lock on Invoice
    response idempotently         Verify balance >= payment amount
    (No state change!)            Insert Payment (holds unique key)
                                  Update Invoice paidAmount & balance
                                  Commit Transaction
```

When two identical requests arrive simultaneously:
- Thread 1 acquires the row lock on the invoice, inserts the payment with `idempotency_key = 'pay-abc-123'`, updates the invoice, and commits.
- Thread 2 (competing simultaneously) waits on the row lock. Upon acquiring the lock, it observes that `pay-abc-123` already exists in `payments` (or is rejected by `uk_payments_idempotency_key`), aborts duplicate processing, and returns the existing payment record idempotently!

---

## 6. Database Behavior
* In MySQL InnoDB, the unique index on `idempotency_key` guarantees that no two transactions can commit identical keys.
* `SELECT ... FOR UPDATE` on `invoices` serializes transactions targeting the same invoice, ensuring balance calculations are deterministic.

---

## 7. Transaction Behavior
1. `PaymentServiceImpl.processPayment(...)` is annotated with `@Transactional`.
2. First, queries `paymentRepository.findByIdempotencyKey(key)`. If found, returns existing `PaymentResponse`.
3. If not found, locks invoice: `invoiceRepository.findByIdForUpdate(invoiceId)`.
4. Checks again if key was inserted during lock acquisition.
5. Verifies `invoice.getStatus() != InvoiceStatus.PAID` and `paymentAmount <= invoice.getBalanceDue()`.
6. Creates and saves `Payment`.
7. Updates `invoice.recordPayment(payment.getAmount())`.
8. Commits transaction and releases locks.

---

## 8. Test Verification
The multi-threaded test `PaymentIdempotencyConcurrencyTest.java` executes:
* 5 concurrent threads submitting payment with the **exact same idempotency key** simultaneously.
* **Assertion 1**: Exactly 1 payment record is created in the database.
* **Assertion 2**: Invoice `paidAmount` is incremented by exactly one payment amount (never duplicated).
* **Assertion 3**: All 5 threads return successful responses referencing the exact same payment ID.
