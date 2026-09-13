# ADR-0013: Medical Document Management and Metadata Storage Architecture

## Status
Accepted

## Context
Healthcare workflows generate and consume diverse electronic medical documents, including laboratory reports, physician prescriptions, inpatient discharge summaries, diagnostic imaging reports, patient consent forms, referral letters, and insurance claims documentation (§34, §103 Phase 13).

Key architectural considerations for medical document management include:
1. **Separation of Binary Payloads from Relational Metadata (§34)**:
   - Storing large binary objects (BLOBs) directly within relational database tables (MySQL) degrades buffer pool caching efficiency, inflates transaction log (InnoDB redo/undo) volumes, and complicates database backups.
   - Clinical documents should be stored in dedicated object/file storage, while relational tables strictly manage immutable metadata, search indices, ownership relationships, access permissions, and integrity checksums.
2. **Document Versioning and Lineage Control (§34, §103)**:
   - Clinical documents undergo iterative revisions (e.g. preliminary lab results revised to final, discharge summaries updated with follow-up recommendations).
   - The platform must maintain a tamper-evident revision history (`parentDocumentId`, `documentVersion`, and lifecycle status transitions) so that previous revisions remain retrievable while the latest active document is clearly designated.
3. **Data Integrity & Cryptographic Checksums**:
   - Medical documents require strict integrity verification to prevent silent corruption or tampering.
   - A cryptographic SHA-256 digest must be computed at ingestion time, stored in metadata, and verified upon retrieval.
4. **Access Control & Patient Privacy (PHI/PII & HIPAA Compliance - §91)**:
   - Medical records contain Protected Health Information (PHI).
   - Document access, uploads, and downloads must be strictly governed by Role-Based Access Control (RBAC) and patient-ownership isolation (`ROLE_PATIENT` restricted to own documents).
   - Every document download and metadata query must emit structured audit events.

## Decision
1. **Package by Feature**:
   All document metadata models, storage abstractions, repositories, services, DTOs, controllers, and mappers reside under `com.careflow.document`.
2. **Domain Architecture**:
   - `MedicalDocument`: Aggregate root managing document number, title, document type (`LAB_REPORT`, `PRESCRIPTION`, `DISCHARGE_SUMMARY`, `INSURANCE_DOCUMENT`, `CONSENT_FORM`, `DIAGNOSTIC_REPORT`, `REFERRAL_LETTER`, `OTHER`), patient owner, uploader ID, file metadata (name, MIME type, size, SHA-256 checksum), storage URI, version counter, parent document reference, and lifecycle status (`ACTIVE`, `ARCHIVED`, `DELETED`).
   - Extends `BaseAuditEntity` for auditing timestamps (`createdAt`, `updatedAt`, `createdBy`, `updatedBy`) and optimistic concurrency locking.
3. **Storage Abstraction (`StorageService`)**:
   - Pluggable `StorageService` interface defining contract for binary storage operations (`store`, `loadAsResource`, `delete`, `exists`).
   - `LocalStorageService` default implementation partitioning stored files by date (`{baseDir}/{yyyy}/{MM}/{uuid}_{sanitizedName}`) with path traversal prevention and streaming SHA-256 calculation.
4. **Versioning Workflow**:
   - Initial upload creates revision `1` with `parentDocumentId = null` and status `ACTIVE`.
   - New version upload links to the root document (`parentDocumentId = root.id`), sets revision to `latestVersion + 1`, and transitions the previous active version to `ARCHIVED`.
   - Full version history can be inspected via `/api/v1/documents/{id}/history`.
5. **Role Clearance & RBAC**:
   - Upload & Versioning: `DOCTOR`, `NURSE`, `ADMIN`, `LAB_TECHNICIAN`, `BILLING_OFFICER`.
   - Read & Download: Clinical and administrative staff with valid role clearances, plus `PATIENT` (enforcing strict patient-ID matching).
   - Archive & Soft Delete: Restricted to `DOCTOR` and `ADMIN`.

## Consequences
- Relational schema remains fast, lean, and easily indexable.
- Storage layer can be swapped (e.g. from local filesystem to AWS S3, MinIO, or Azure Blob) without touching business logic or domain models.
- Complete version lineage ensures clinical and legal defensibility.
- Strict SHA-256 verification guarantees bit-level file integrity.
