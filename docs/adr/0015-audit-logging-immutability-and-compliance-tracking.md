# ADR-0015: Immutable Audit Logging and HIPAA Compliance Tracking Architecture

## Status
Accepted

## Context
CareFlow operates as an enterprise hospital and healthcare platform managing Protected Health Information (PHI), controlled clinical substances, and financial transactions (§36, §74, §103 Phase 15).

Key regulatory and architectural drivers:
1. **HIPAA §164.312(b) Audit Controls Requirement**:
   - The platform must maintain a detailed audit trail of all accesses and modifications to patient records, diagnostic test results, prescriptions, and clinical notes to fulfill HIPAA accounting of disclosures requirements.
2. **Absolute Immutability & Anti-Tampering (§36, §105)**:
   - Audit records must be append-only.
   - Once written, an audit entry must never be updated, edited, or deleted by any user or automated routine.
   - Any attempt to modify an audit log constitutes a regulatory and security violation.
3. **Auditability Context (§74)**:
   - For every sensitive workflow, the system must definitively record:
     - **Who** performed it (`actorUserId`)
     - **What** changed (`action`, `previousValue`, `newValue`)
     - **When** it occurred (`timestamp`)
     - **Which resource** was impacted (`resourceType`, `resourceId`)
     - **Which patient** was associated (`patientId`)
     - **From where** it originated (`ipAddress`)
     - **Correlation trace** across system services (`correlationId`)
     - **Execution outcome** (`status`, e.g. `SUCCESS`, `FAILURE`, `ACCESS_DENIED`)
4. **Autonomous Transaction Boundary (`Propagation.REQUIRES_NEW`)**:
   - When a clinical, financial, or security operation fails, aborts, or is denied due to an authorization violation, the business transaction rolls back.
   - However, the audit record of that attempt (especially failed logins, access violations, or bad payloads) **must not be rolled back**.
   - Audit writes must run in an independent database transaction (`REQUIRES_NEW`) to guarantee persistence.
5. **High-Throughput Concurrency (§57, §92)**:
   - Audit logging occurs across all application threads on every sensitive read and write.
   - Writing audit logs must not introduce row lock contention, deadlocks, or bottleneck business transactions.
6. **Strict RBAC & Segregation of Duties (§91)**:
   - Querying audit records, viewing patient access histories, and generating compliance summaries is restricted strictly to administrators (`ROLE_ADMIN`) and compliance auditors.

## Decision
1. **Package by Feature**:
   All audit models, repositories, services, DTOs, controllers, and mappers reside under `com.careflow.audit`.
2. **Domain Architecture**:
   - `AuditLog`: Immutable aggregate root containing all metadata required by §36 and §74.
   - Immutability enforcement: Domain entity defines JPA lifecycle listeners (`@PreUpdate` and `@PreRemove`) that throw `UnsupportedOperationException`, preventing any JPA-managed entity modification or removal.
   - Standardized Enums: `AuditAction`, `AuditResourceType`, and `AuditStatus`.
3. **Database Schema & Indexing (Flyway V19)**:
   - Dedicated table `audit_logs` created with `TEXT` columns for `previous_value` and `new_value`.
   - Tuned B-tree indexes for compliance queries:
     - `idx_audit_timestamp`: descending time queries
     - `idx_audit_actor_timestamp`: actor activity timelines
     - `idx_audit_patient_timestamp`: patient disclosure accounting
     - `idx_audit_resource`: entity modification history
     - `idx_audit_action_timestamp`: event type aggregations
     - `idx_audit_correlation_id`: cross-service distributed tracing
4. **Context Enrichment (`AuditContextHelper`)**:
   - Transparently extracts actor username from Spring Security's `SecurityContextHolder`.
   - Resolves originating client IP address (evaluating `X-Forwarded-For` header for reverse proxies/load balancers).
   - Extracts distributed trace correlation identifier from SLF4J `MDC`.
5. **Transactional Strategy**:
   - `recordEvent` and `recordSensitiveAccess` execute with `Propagation.REQUIRES_NEW`, isolating audit persistence from caller transaction rollbacks.
6. **REST API Contract**:
   - `POST /api/v1/audit/events`: Records audit events (`@PreAuthorize("isAuthenticated()")`).
   - `GET /api/v1/audit/logs`: Multi-criteria dynamic specification search (`ROLE_ADMIN`).
   - `GET /api/v1/audit/logs/{id}`: Single event lookup (`ROLE_ADMIN`).
   - `GET /api/v1/audit/patients/{patientId}`: Patient disclosure history (`ROLE_ADMIN`).
   - `GET /api/v1/audit/resources/{resourceType}/{resourceId}`: Resource audit trail (`ROLE_ADMIN`).
   - `GET /api/v1/audit/actors/{actorUserId}`: Actor timeline (`ROLE_ADMIN`).
   - `GET /api/v1/audit/reports/summary`: Compliance aggregate dashboard report (`ROLE_ADMIN`).
   - Strictly **no PUT, PATCH, or DELETE endpoints** exposed.

## Consequences
- Full compliance with HIPAA §164.312(b) audit trail requirements.
- Zero row contention: append-only design allows concurrent multi-threaded writes without locking.
- Complete separation between transactional business logic and tamper-evident auditing.
