# ADR-0009: Laboratory Workflow and Specimen Lifecycle Architecture

## Status
Accepted

## Context
Laboratory diagnostics are central to modern clinical decision-making (§27, §103 Phase 9). A laboratory workflow spans multiple distinct operational actors across different departments:
1. **Ordering (Doctor)**: A physician places a diagnostic test order during or after a clinical consultation.
2. **Specimen Collection (Nurse / Phlebotomist / Lab Technician)**: The patient provides biological samples (blood, urine, swab, tissue, CSF). Samples must be accessioned, barcoded, and validated.
3. **Laboratory Processing (Lab Technician)**: Technicians run assays on diagnostic analyzers or manual benches and record quantitative or qualitative parameters.
4. **Critical Value Detection & Pathologist / Doctor Review**: Abnormal and critical values (e.g. severe hypokalemia, acute troponin elevation) must be automatically flagged, and results must undergo review and sign-off by the ordering or reviewing doctor.

Key architectural and domain challenges include:
- **Strict State Progression**:
  The lifecycle of a lab order follows: `ORDERED` $\rightarrow$ `SAMPLE_COLLECTED` $\rightarrow$ `PROCESSING` $\rightarrow$ `COMPLETED`, with `CANCELLED` permitted only prior to analytical completion.
- **Specimen Tracking & Barcoding**:
  Biological specimens require globally unique tracking barcodes (`SMP-YYYYMMDD-XXXX`) to prevent specimen mix-ups and contamination errors.
- **Critical Value Flagging**:
  The system must compare numeric results against defined test reference ranges and flag results as `NORMAL`, `LOW`, `HIGH`, `CRITICAL_LOW`, or `CRITICAL_HIGH`.
- **Role Separation & RBAC Enforcement**:
  Ordering is restricted to `DOCTOR` and `ADMIN`. Sample collection is permitted for `LAB_TECHNICIAN`, `NURSE`, and `ADMIN`. Result entry and analytical processing is restricted to `LAB_TECHNICIAN` and `ADMIN`. Result review and sign-off is restricted to `DOCTOR` and `ADMIN`.
- **Concurrency & State Transition Integrity**:
  Concurrent actions (such as a doctor attempting to cancel an order at the same moment a laboratory technician enters final results and marks it completed) must be prevented using optimistic concurrency control (`@Version`).

## Decision
1. **Package by Feature**:
   All laboratory domain models, repositories, business logic, DTOs, controllers, and mappers reside under `com.careflow.laboratory`.
2. **Domain Entities**:
   - `LabTest`: Master catalog entry defining the test code, name, clinical category (`HEMATOLOGY`, `BIOCHEMISTRY`, `MICROBIOLOGY`, etc.), specimen type, reference ranges, unit of measure, turnaround time, and billing price.
   - `LabOrder`: The requisition aggregate root containing patient, doctor, consultation reference, priority (`ROUTINE`, `URGENT`, `STAT`), order status, review status (`PENDING_REVIEW`, `REVIEWED`, `AMENDED`), and clinical indications.
   - `LabOrderItem`: Specific test requested within a requisition, maintaining individual item completion status.
   - `LabSample`: Specimen record capturing barcode, specimen type, collection timestamp, collector ID, physical condition notes, and accessioning status (`COLLECTED`, `ACCEPTED`, `REJECTED`).
   - `LabResult`: Analytical test observation storing parameter name, numeric/text value, unit, reference range, auto-evaluated abnormality flag, technician ID, and timestamp.
3. **State Machine & Invariant Enforcement**:
   - State progression is strictly validated: an order cannot be processed until samples are collected, and cannot be completed without results recorded.
   - Once marked `COMPLETED`, the order is immutable to cancellation.
   - Cancellation requires an explicit reason and can only be executed on non-terminal orders.
4. **Automated Abnormality & Critical Value Evaluation**:
   - When numeric test results are submitted alongside catalog reference ranges, the service layer evaluates the value against lower/upper normal limits and critical thresholds, assigning appropriate clinical flags.
5. **Optimistic Concurrency Control**:
   - `LabOrder` and `LabTest` inherit `BaseAuditEntity` with `@Version`.
   - Concurrent updates trigger `OptimisticLockingFailureException`, which is caught and mapped to HTTP 409 Conflict to protect clinical record integrity.

## Consequences
- Clean separation between ordering clinicians and laboratory analytical technicians.
- Full traceability from doctor requisition to physical tube barcode, analytical result, and final clinical sign-off.
- Ready integration with downstream billing (Phase 11) for lab diagnostic invoicing.
