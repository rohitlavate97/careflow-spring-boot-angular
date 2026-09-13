# ADR-0010: Inpatient Admission and Bed Concurrency Architecture

## Status
Accepted

## Context
Inpatient hospital operations govern the life-cycle of admitted patients, physical ward capacity, room allocations, and bed assignments (§28, §57 Lab 4, §103 Phase 10). When a patient requires hospitalization (post-emergency, post-operative, or direct clinical admission), they are assigned to an available physical bed in an appropriate ward.

Key domain challenges in inpatient admission and bed management:
1. **Double-Bed Occupancy Race Condition (Concurrency Lab 4)**:
   During high-intake periods (e.g. emergency department surges or shift handoffs), two hospital staff members (admitting clerks, triage nurses, or bed coordinators) might simultaneously attempt to allocate the exact same physical bed to two distinct patients.
   Without strict synchronization, both admissions proceed, resulting in severe clinical confusion, logistical disruption, and patient safety hazards (§28, §92).
2. **Bed State Machine**:
   Physical hospital beds transition through distinct operational states:
   - `AVAILABLE`: Sanitized, inspected, and ready for patient occupancy.
   - `OCCUPIED`: Currently housing an admitted inpatient.
   - `RESERVED`: Temporarily held for incoming emergency or scheduled procedural transfer.
   - `MAINTENANCE`: Out of service for terminal disinfection, mechanical repair, or medical equipment maintenance.
3. **Admission Lifecycle & Transfers**:
   - Inpatients progress through: `ADMITTED` $\rightarrow$ `TRANSFERRED` (as clinical condition dictates stepping up to ICU or stepping down to general wards) $\rightarrow$ `DISCHARGED`.
   - Transfers require atomic deallocation of the source bed (`OCCUPIED` $\rightarrow$ `AVAILABLE`) and allocation of the target bed (`AVAILABLE` $\rightarrow$ `OCCUPIED`).
   - A patient must have at most one active inpatient admission at any given time.
4. **Discharge & Sanitization**:
   - On discharge, the patient's active bed is vacated (`AVAILABLE`), the admission record is finalized with clinical discharge notes, and length-of-stay metrics are locked.

## Decision
1. **Package by Feature**:
   All admission domain aggregates, bed inventory models, transfer records, repositories, services, DTOs, controllers, and mappers reside under `com.careflow.admission`.
2. **Domain Architecture**:
   - `Ward`: Inpatient department unit (code, name, department FK, ward type, total beds, active).
   - `Room`: Room container inside a ward (room number, room type, ward FK, active).
   - `Bed`: The assignable physical unit (bed number, room FK, bed status, daily room rate, active).
   - `Admission`: The inpatient aggregate root (admission number, patient FK, admitting doctor FK, current bed FK, admission status, admission reason, diagnosis, admitted at, discharged at, discharge summary).
   - `BedTransferRecord`: Immutable audit trail of every bed reallocation during the admission stay.
3. **Pessimistic Write Locking for Bed Allocation (Concurrency Lab 4)**:
   - When admitting a patient or transferring to a new bed, the destination bed is locked at the database row level using JPA `LockModeType.PESSIMISTIC_WRITE` (`SELECT ... FOR UPDATE` via `bedRepository.findByIdForUpdate(bedId)`).
   - Inside the transaction, the current state of the bed is strictly evaluated:
     `if (bed.getStatus() != BedStatus.AVAILABLE) throw new BedNotAvailableException(...)`.
   - The bed status is updated to `OCCUPIED`.
   - The transaction commits, releasing the row lock and guaranteeing that concurrent allocation attempts are serialized and deterministically rejected with HTTP 409 Conflict.
4. **Active Bed Occupancy Invariant**:
   - The database schema enforces bed safety by verifying that `current_bed_id` in `admissions` cannot be duplicated among currently active admissions.
   - A patient cannot be admitted if `admissionRepository.existsByPatientIdAndStatusIn(patientId, activeStatuses)` returns true.
5. **Atomic Transfer Coordination**:
   - Bed transfer operations lock both source and destination beds within a single database transaction, guaranteeing zero orphaned or double-occupied beds if any stage of the transfer fails.

## Consequences
- Guaranteed mathematical prevention of double-bed occupancy under concurrent load.
- Real-time visibility into hospital ward occupancy, bed turnover, and vacancy rates.
- Clean integration with downstream Phase 11 (Billing) for bed night charges and room rates.
