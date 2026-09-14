# CareFlow — Enterprise Hospital & Healthcare Operations Platform

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.3-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Flyway](https://img.shields.io/badge/Flyway-11.3.4-red.svg)](https://flywaydb.org/)
[![Architecture](https://img.shields.io/badge/Architecture-Modular%20Monolith-blue.svg)](#architecture)

CareFlow is an enterprise-grade Hospital and Healthcare Operations Platform engineered to model complex clinical, administrative, and operational workflows across 20 distinct domain modules.

> **Important Domain Disclaimer**: CareFlow is an enterprise software engineering portfolio and training project. It makes no medical claims, provides no clinical diagnoses or treatment recommendations, and operates exclusively with synthetic test data (§3).

---

## Architecture Overview

CareFlow is designed as a **Modular Monolith** organized strictly by domain feature boundaries (`com.careflow.<module>`). Each domain module encapsulates its own business logic, domain entities, repositories, and API controllers.

```text
com.careflow
├── common/             <-- Shared foundation (auditing, correlation, security, error handling)
├── identity/           <-- Authentication, RBAC, user management
├── staff/              <-- Hospital staff, doctor profiles
├── patient/            <-- Patient registration, records, search
├── department/         <-- Hospital departments and unit hierarchy
├── scheduling/         <-- Doctor shifts, leave, appointment slots
├── appointment/        <-- Lifecycle, double-booking prevention
├── queue/              <-- Outpatient queue orchestration
├── consultation/       <-- Doctor-patient clinical encounters
├── clinical/           <-- Vitals, diagnoses, allergy history
├── prescription/       <-- Prescription issuance
├── pharmacy/           <-- Inventory tracking, stock decrement, dispensing
├── laboratory/         <-- Lab catalog, test orders, specimen tracking, results
├── admission/          <-- Inpatient wards, rooms, bed management
├── billing/            <-- Invoice generation, line items, BigDecimal calculations
├── insurance/          <-- Claims, pre-authorization, settlement
├── document/           <-- Medical document metadata and references
├── notification/       <-- Multi-channel communication events
├── audit/              <-- Tamper-evident audit trail for PHI/PII access
├── reporting/          <-- Operational aggregates, KPI dashboards
└── administration/     <-- System settings, catalog configuration
```

---

## Technology Stack

- **Runtime**: Java 21 LTS
- **Core Framework**: Spring Boot 3.4.3 (Spring Web, Spring Security, Spring Data JPA, Bean Validation, Actuator)
- **Database**: MySQL 8+ with HikariCP connection pooling
- **Migrations**: Flyway (schema validation enforced with `spring.jpa.hibernate.ddl-auto=validate`)
- **Frontend**: Angular + TypeScript (client workspace)
- **Observability**: SLF4J + MDC Request Correlation Tracking (`X-Correlation-ID`)
- **Testing**: JUnit 5, Mockito, Spring Boot Test, MockMvc, Testcontainers

---

## Getting Started

### Prerequisites
- JDK 21+
- Apache Maven 3.9+
- MySQL 8.0+ (for local development profile)

### Building the Project
```bash
mvn clean compile
```

### Running Tests
```bash
mvn test
```

### Running Locally
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

### Health & Diagnostic Endpoints
- **Health Check**: `GET http://localhost:8080/actuator/health`
- **System Ping**: `GET http://localhost:8080/api/v1/system/ping`
- **Response**:
  ```json
  {
    "status": "UP",
    "service": "CareFlow Enterprise Platform",
    "timestamp": "2026-09-11T01:05:00.000Z",
    "correlationId": "4a7b9c2d-8e1f-4a3b-9c2d-8e1f4a3b9c2d"
  }
  ```

---

## Demo Accounts (§101)

The database is provisioned via Flyway (`V3__seed_demo_accounts.sql`) with synthetic demo credentials for evaluation and testing:

| Role | Username / Email | Password | Primary Clearance |
|---|---|---|---|
| **Administrator** | `admin@careflow.local` | `Admin@123` | Full system settings, audit logs, and administration |
| **Doctor** | `doctor@careflow.local` | `Doctor@123` | Patient encounters, prescriptions, and lab orders |
| **Nurse** | `nurse@careflow.local` | `Nurse@123` | Patient vitals, triage, and queue check-ins |
| **Receptionist** | `receptionist@careflow.local` | `Receptionist@123` | Patient registration, queue allocation, scheduling |
| **Pharmacist** | `pharmacist@careflow.local` | `Pharmacist@123` | Inventory decrements and prescription dispensing |
| **Lab Technician**| `lab@careflow.local` | `Lab@123` | Specimen collection, lab processing, and results |
| **Billing Officer**| `billing@careflow.local` | `Billing@123` | Invoices, fee schedules, and payment processing |

---

## Architectural Decision Records (ADRs)

Key architectural decisions are documented under [`docs/adr/`](docs/adr/):
- [`ADR-0001: Modular Monolith Architecture`](docs/adr/0001-modular-monolith-architecture.md)
- [`ADR-0005: Stateless JWT Authentication & RBAC Architecture`](docs/adr/0005-jwt-stateless-authentication.md)
- [`ADR-0007: Consultation Lifecycle and Clinical Records Architecture`](docs/adr/0007-consultation-and-clinical-records-architecture.md)
- [`ADR-0008: Pharmacy Inventory and Concurrency Control Architecture`](docs/adr/0008-pharmacy-inventory-and-concurrency-control.md)
- [`ADR-0009: Laboratory Workflow and Specimen Lifecycle Architecture`](docs/adr/0009-laboratory-workflow-and-specimen-lifecycle.md)
- [`ADR-0010: Inpatient Admission and Bed Concurrency Architecture`](docs/adr/0010-inpatient-admission-and-bed-concurrency.md)
- [`ADR-0011: Billing Invoicing and Payment Idempotency Architecture`](docs/adr/0011-billing-invoicing-and-payment-idempotency.md)
- [`ADR-0012: Insurance Policy and Claims Adjudication Architecture`](docs/adr/0012-insurance-policy-and-claims-adjudication.md)
- [`ADR-0013: Medical Document Management and Metadata Storage Architecture`](docs/adr/0013-medical-document-management-and-metadata-storage.md)
- [`ADR-0014: Multi-Channel Notification Center and Event Abstraction Architecture`](docs/adr/0014-multi-channel-notification-center-and-event-abstraction.md)
- [`ADR-0015: Immutable Audit Logging and HIPAA Compliance Tracking Architecture`](docs/adr/0015-audit-logging-immutability-and-compliance-tracking.md)
- [`ADR-0016: Operational Reporting, Clinical Throughput Analytics, and Pushdown Database Aggregations`](docs/adr/0016-operational-reporting-and-database-aggregations.md)
- [`ADR-0017: Hospital System Administration, Dynamic Configuration Management, and Governance Architecture`](docs/adr/0017-administration-system-configuration-and-governance.md)
- [`ADR-0018: Redis Caching and Resilient Infrastructure with Graceful Degradation`](docs/adr/0018-redis-caching-and-graceful-degradation.md)

---

## Concurrency Labs (§57, §92)

CareFlow features six dedicated multi-threaded concurrency labs demonstrating race conditions and their enterprise-grade solutions:
- [`Lab 1: Double-Booked Appointment Prevention`](docs/concurrency/lab-1-double-booked-appointment.md)
- [`Lab 2: Patient Queue State Transitions`](docs/concurrency/lab-2-patient-queue-race-conditions.md)
- [`Lab 3: Pharmacy Inventory Stock Dispensation`](docs/concurrency/lab-3-pharmacy-inventory-concurrency.md)
- [`Lab 4: Inpatient Bed Allocation Protection`](docs/concurrency/lab-4-bed-allocation-concurrency.md)
- [`Lab 5: Payment Idempotency & Duplicate Charge Prevention`](docs/concurrency/lab-5-payment-idempotency-concurrency.md)

