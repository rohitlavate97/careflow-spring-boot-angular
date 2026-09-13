# ADR-0008: Pharmacy Inventory and Concurrency Control Architecture

## Status
Accepted

## Context
In hospital operations, the dispensing of prescription medications represents a mission-critical workflow with severe clinical, legal, and financial ramifications (§24, §25, §26). When a physician issues a prescription, it is reviewed and fulfilled by the pharmacy department.

Key challenges in prescription and pharmacy inventory management include:
1. **Concurrency and Overselling**: Multiple dispensary counters or automated pharmacy kiosks may simultaneously attempt to dispense the final available units of a medication batch. Without strict concurrency control, race conditions lead to negative inventory (selling ghost stock), dispensing unallocated medications, and inventory audit discrepancies (§26).
2. **Batch & Expiry Management (FEFO)**: Pharmaceutical safety requires strict batch tracking with expiration dates. Dispensing should prioritize batches that expire first (First-Expiring, First-Out - FEFO) while strictly refusing to dispense expired medications.
3. **Prescription Lifecycle & Partial Dispensing**: A prescription may contain multiple line items. A pharmacy may dispense all items at once or dispense items incrementally if certain stocks are awaiting restock. The state machine must track transitions cleanly (`DRAFT` -> `PENDING_DISPENSE` -> `PARTIALLY_DISPENSED` -> `DISPENSED`, or `CANCELLED`).
4. **Role Clearance & Separation of Duties**: Prescribing is strictly restricted to medical practitioners (`DOCTOR`, `ADMIN`). Reviewing stock, managing inventory batches, and dispensing medications is restricted to licensed pharmacists (`PHARMACIST`, `ADMIN`).
5. **Database Integrity as Final Safeguard**: Application logic checks alone are vulnerable to concurrency races. The database schema must enforce inventory non-negativity via check constraints (`CHECK (quantity_available >= 0)`).

## Decision
1. **Module Segregation**:
   - `com.careflow.prescription`: Manages the prescription lifecycle (`Prescription`), individual medication orders (`PrescriptionItem`), dosage instructions, and dispensing status.
   - `com.careflow.pharmacy`: Manages medication catalogs (`Medication`), inventory batches (`PharmacyInventoryBatch`), and audit logs of fulfilled dispensations (`DispenseRecord`).
2. **Pessimistic Write Locking for Stock Decrement**:
   - During dispensing, the target inventory batch is acquired using JPA `LockModeType.PESSIMISTIC_WRITE` (`SELECT ... FOR UPDATE` via `pharmacyInventoryBatchRepository.findByIdForUpdate(batchId)`).
   - The lock guarantees that concurrent dispensing requests on the same batch are serialized at the database row level.
   - Inside the transaction, available quantity is re-verified: `if (batch.getQuantityAvailable() < requestedQuantity) throw new InsufficientInventoryException(...)`.
   - The batch quantity is decremented, and the dispense event is recorded atomically.
3. **Database-Level Invariant Constraint**:
   - `CONSTRAINT chk_pharmacy_inventory_non_negative CHECK (quantity_available >= 0)` is placed on `pharmacy_inventory_batches`.
   - Any transaction attempting to breach this invariant will be aborted by the database engine, guaranteeing zero negative stock even under extreme edge cases.
4. **FEFO (First-Expiring, First-Out) Automated Batch Allocation**:
   - When querying available inventory for a medication, query order defaults to `ORDER BY expiry_date ASC, created_at ASC`, ensuring older stock is exhausted before newer stock.
   - Batches with `expiry_date <= CURRENT_DATE` are excluded from active dispensing queries.
5. **Prescription State Progression**:
   - Upon creation by a doctor, the prescription enters `PENDING_DISPENSE`.
   - As items are dispensed, the prescription status advances to `PARTIALLY_DISPENSED` or `DISPENSED` once all prescribed line items are satisfied.

## Alternatives Considered
- **Optimistic Locking (`@Version`) Alone for Inventory**:
  - *Rejected*: In high-throughput pharmacy counters during peak hours, multiple pharmacists dispensing popular medications (e.g. Paracetamol, Amoxicillin) would encounter high rates of `OptimisticLockException` rollback retries. Pessimistic row locking on the specific inventory batch serializes requests cleanly with minimal latency and deterministic queueing.
- **In-Memory Cache (Redis) Counter Decrements**:
  - *Rejected*: Redis decrements (`DECRBY`) introduce distributed transaction complexity and eventual consistency lag between Redis and MySQL, violating the modular monolith transactional integrity requirement. MySQL row-locking on InnoDB provides sub-millisecond serialization with ACID guarantees.

## Consequences
- Zero probability of overselling or negative inventory.
- High auditability of who dispensed which batch to which patient.
- Clean integration with downstream Phase 11 (Billing) for pharmacy charge generation.
