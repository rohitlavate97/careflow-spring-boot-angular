# ADR-0007: Consultation Lifecycle and Clinical Records Architecture

## Status
Accepted

## Context
In hospital operations, a patient consultation (clinical encounter) represents the pivotal interaction between a healthcare provider (doctor, assisted by nursing staff) and a patient. The consultation bridges administrative workflows (appointment booking and queue check-in) with downstream clinical fulfillment (prescriptions, laboratory orders, admission, and billing).

Key challenges in consultation and clinical records architecture include:
1. **Clinical Data Immutability & Auditability**: Medical legal standards require that historical clinical entries (diagnoses, examination notes, vitals) cannot be silently overwritten or modified once a consultation is marked completed.
2. **State Machine Integrity**: A consultation transitions through clear operational states (`STARTED` -> `IN_PROGRESS` -> `COMPLETED`), with potential abandonment/cancellation (`CANCELLED`). Attempting to reopen or arbitrarily mutate finalized consultations must be rejected.
3. **Cross-Module Linkage**: Consultations may originate from an appointment (`appointment_id`) or an outpatient walk-in queue ticket (`queue_entry_id`). When a consultation transitions to `STARTED`, `IN_PROGRESS`, or `COMPLETED`, the corresponding queue and appointment states must synchronize consistently.
4. **Role Clearance & Sensitive PHI Access**: Only authorized healthcare practitioners (`DOCTOR`, `NURSE`, `ADMIN`) may view and record clinical vitals, diagnoses, and examination notes. Non-clinical staff (e.g. receptionists) should only have access to operational metadata (status, provider, timestamps).
5. **Concurrency & Lost Update Prevention**: In a multi-user clinical environment where doctors and triage nurses may concurrently input vitals and notes, optimistic locking (`@Version`) must prevent concurrent write conflicts.

## Decision
1. **Module Segregation**:
   - `com.careflow.consultation`: Encapsulates consultation encounter lifecycle (`Consultation`), encounter vitals (`ConsultationVitals`), and encounter diagnoses (`ConsultationDiagnosis`).
   - `com.careflow.clinical`: Encapsulates longitudinal patient clinical records, including persistent clinical notes (`ClinicalNote`), historical diagnoses, and patient clinical summary aggregations.
2. **Domain Model & Persistence**:
   - `Consultation` aggregate root holds references to `patientId`, `doctorId`, optional `appointmentId`, and optional `queueEntryId`.
   - Vitals are modeled as an `@Embedded` component `ConsultationVitals` to ensure atomic retrieval and persistence with the consultation aggregate, eliminating unnecessary join overhead during high-frequency clinical charts.
   - `ConsultationDiagnosis` entities represent coded diagnoses (ICD-10 code, description, type: `PRIMARY`, `SECONDARY`, `PROVISIONAL`, `DIFFERENTIAL`, and severity) associated with the consultation.
   - `ClinicalNote` aggregate root captures SOAP notes (Subjective, Objective, Assessment, Plan) and clinical progress notes tied to a patient and optional consultation.
3. **State Machine & Finalization Guard**:
   - Lifecycle: `STARTED` -> `IN_PROGRESS` -> `COMPLETED`. Consultations in `STARTED` or `IN_PROGRESS` may also transition to `CANCELLED`.
   - Once in `COMPLETED` or `CANCELLED` status, the consultation is locked: any update throws `ConsultationLockedException`.
   - Completion validation: A consultation cannot be completed without at least one primary diagnosis or comprehensive clinical summary note.
4. **Synchronization with Queue & Appointment**:
   - Starting a consultation automatically transitions the associated `QueueEntry` to `IN_CONSULTATION` and `Appointment` to `IN_CONSULTATION`.
   - Completing a consultation transitions the associated `QueueEntry` to `COMPLETED` and `Appointment` to `COMPLETED`.
5. **Auditing & Concurrency**:
   - All entities inherit from `BaseAuditEntity`, automatically capturing `createdAt`, `updatedAt`, `createdBy`, `updatedBy`, and an optimistic locking `version` column.

## Alternatives Considered
- **All-in-One Massive Consultation Entity**: Storing diagnoses and notes as unstructured JSON text inside `consultation`. *Rejected*: Prevents efficient relational querying, ICD-10 analytics, reporting, and structured validation.
- **Separate Microservice for Clinical Records**: Splitting into a standalone microservice with distributed transactions (Saga / 2PC). *Rejected*: Premature distributed system complexity; violates the modular monolith design principle.

## Consequences
- Clean transactional boundaries within the modular monolith.
- High auditability and legal defensibility of medical encounter records.
- Seamless integration with downstream Phase 8 (Prescriptions) and Phase 9 (Laboratory) modules.
