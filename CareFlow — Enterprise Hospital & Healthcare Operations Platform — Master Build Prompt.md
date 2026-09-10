# CAREFLOW
## Enterprise Hospital & Healthcare Operations Platform

---

# 1. ROLE

Act as my **Principal Software Architect, Senior Java/Spring Boot Engineer, Senior Angular Engineer, Database Architect, QA Engineer, DevOps Engineer, Security Engineer, and technical mentor** throughout this project.

I am building **CareFlow**, an enterprise-grade Hospital & Healthcare Operations Platform for my software-engineering portfolio and interview preparation.

This is NOT a toy CRUD application.

The application must demonstrate how a real enterprise engineering team would design, implement, test, debug, secure, monitor, deploy, and maintain a complex healthcare operations platform.

The project must prioritize:

- clean architecture
- modular design
- maintainability
- correctness
- transactional integrity
- concurrency control
- security
- performance
- observability
- testability
- realistic business workflows
- production engineering practices
- strong interview value

Do not simplify complex engineering problems merely to make implementation easier.

When a realistic enterprise problem exists, implement the realistic solution and explain the engineering trade-offs.

---

# 2. PROJECT OBJECTIVE

Build:

**CareFlow — Enterprise Hospital & Healthcare Operations Platform**

The platform manages the operational lifecycle of:

- staff
- doctors
- departments
- patients
- appointments
- doctor schedules
- patient queues
- consultations
- clinical records
- prescriptions
- pharmacy inventory
- laboratory orders
- laboratory results
- admissions
- beds
- billing
- payments
- insurance claims
- medical documents
- notifications
- audit records
- reporting
- administration

The system should model realistic hospital workflows rather than disconnected CRUD screens.

---

# 3. IMPORTANT DOMAIN DISCLAIMER

CareFlow is a **software engineering portfolio/training project**.

It must NOT claim to provide:

- medical diagnosis
- clinical decision-making
- treatment recommendations
- emergency medical advice
- automated medical judgment

Use **synthetic/demo patient data only**.

Never use real patient information.

Clinical fields should represent information entered or reviewed by authorized healthcare staff rather than AI-generated medical decisions.

---

# 4. PRIMARY TECHNOLOGY STACK

## Backend

Use:

- Java 21
- Spring Boot 3.x
- Spring Web
- Spring Security
- Spring Data JPA
- Hibernate ORM
- Bean Validation
- Spring Transactions
- Flyway
- MySQL 8+
- Maven

Do NOT use MyBatis.

Hibernate/JPA must be a major learning objective of the project.

---

# 5. FRONTEND

Use:

- Angular
- TypeScript
- Angular Router
- Angular Reactive Forms
- HttpClient
- RxJS
- Angular Guards
- HTTP Interceptors
- reusable components
- responsive layouts

The frontend must communicate with the backend through REST APIs.

Do NOT build the frontend as a collection of hard-coded mock screens.

---

# 6. INFRASTRUCTURE

Introduce infrastructure progressively when justified:

- Redis
- Apache Kafka
- Docker
- Docker Compose
- GitHub Actions
- Prometheus
- Grafana
- OpenTelemetry
- centralized structured logging

Do not add infrastructure merely because it is popular.

For every infrastructure component, explain:

1. What problem it solves
2. Why it is needed
3. Why the selected technology is appropriate
4. What happens when it fails
5. How the application behaves without it

---

# 7. ARCHITECTURE

Use a:

## Modular Monolith

Do NOT immediately create microservices.

The system should have clear domain modules with explicit boundaries.

Example:

```text
careflow/
│
├── identity/
├── staff/
├── patient/
├── department/
├── scheduling/
├── appointment/
├── queue/
├── consultation/
├── clinical/
├── prescription/
├── pharmacy/
├── laboratory/
├── admission/
├── billing/
├── insurance/
├── document/
├── notification/
├── audit/
├── reporting/
└── administration/
```

Each module should own its business logic.

Avoid creating a giant shared service layer.

Avoid excessive cross-module coupling.

---

# 8. PACKAGE STRUCTURE

Prefer:

```text
com.careflow
    ├── identity
    │   ├── controller
    │   ├── service
    │   ├── repository
    │   ├── domain
    │   ├── dto
    │   ├── mapper
    │   └── exception
    │
    ├── patient
    ├── appointment
    ├── scheduling
    ├── pharmacy
    ├── laboratory
    └── ...
```

Use package-by-feature/module rather than package-by-layer across the entire application.

---

# 9. DATABASE

Use:

**MySQL**

Use Flyway for every schema change.

NEVER use:

```properties
spring.jpa.hibernate.ddl-auto=update
```

NEVER use:

```properties
spring.jpa.hibernate.ddl-auto=create
```

Production-style configuration should use:

```properties
spring.jpa.hibernate.ddl-auto=validate
```

All schema changes must be represented through Flyway migrations.

Example:

```text
V1__create_users.sql
V2__create_roles.sql
V3__create_patients.sql
V4__create_departments.sql
V5__create_doctors.sql
```

---

# 10. DATABASE DESIGN PRINCIPLES

Use:

- primary keys
- foreign keys
- unique constraints
- indexes
- check constraints where appropriate
- NOT NULL constraints
- audit timestamps
- optimistic locking where appropriate

Think about:

- indexing
- cardinality
- query patterns
- uniqueness
- concurrency
- data integrity

Do not rely entirely on Java validation for database integrity.

Important business constraints must also be enforced at the database level where appropriate.

---

# 11. JPA/HIBERNATE REQUIREMENTS

Hibernate/JPA must be deliberately used to demonstrate advanced ORM knowledge.

The project must demonstrate:

- `@Entity`
- `@Id`
- generated IDs
- `@ManyToOne`
- `@OneToMany`
- `@OneToOne`
- `@ManyToMany` only when genuinely appropriate
- `@Embedded`
- `@Embeddable`
- enum mapping
- optimistic locking using `@Version`
- lazy loading
- entity graphs
- fetch joins
- DTO projections
- pagination
- sorting
- specifications where useful
- dirty checking
- persistence context
- transaction boundaries
- cascade behavior
- orphan removal where appropriate

Default relationships to:

```text
FetchType.LAZY
```

Do not casually use:

```text
FetchType.EAGER
```

Avoid:

- bidirectional relationships everywhere
- massive entity graphs
- exposing entities directly through REST
- unnecessary cascading
- Open Session in View as a solution to poor transaction design

REST APIs must return DTOs.

---

# 12. HIBERNATE PERFORMANCE LEARNING

The project must intentionally include interview-relevant Hibernate problems.

Demonstrate and fix:

### N+1 query problem

Example:

```text
GET /patients
```

must not accidentally execute:

```text
1 query for patients
+ N queries for appointments
+ N queries for doctors
+ N queries for departments
```

Use appropriate solutions such as:

- fetch joins
- entity graphs
- DTO projections
- batch fetching

Explain why each solution is appropriate.

---

# 13. TRANSACTION MANAGEMENT

Use:

```java
@Transactional
```

at appropriate service boundaries.

Clearly understand:

- transaction propagation
- rollback behavior
- isolation
- read-only transactions
- transaction boundaries
- persistence context lifecycle

Never place `@Transactional` blindly on every method.

For every important transaction, explain:

```text
What must succeed together?
What may fail independently?
What must be retried?
What must be idempotent?
```

---

# 14. CORE DOMAIN MODULES

Implement the following modules progressively.

---

## MODULE 1 — IDENTITY & ACCESS MANAGEMENT

Entities:

- User
- Role
- Permission
- Staff
- Department

Roles:

- ADMIN
- DOCTOR
- NURSE
- RECEPTIONIST
- LAB_TECHNICIAN
- PHARMACIST
- BILLING_OFFICER
- PATIENT

Implement:

- registration
- login
- password hashing
- JWT authentication
- role-based authorization
- method-level security
- account status
- account locking
- permission checks
- password policies

Use:

```text
BCrypt/Argon2
```

Do not store plain-text passwords.

---

# 15. RESOURCE-LEVEL AUTHORIZATION

Healthcare systems require stronger authorization than simple roles.

Example:

A doctor should not automatically be able to access every patient in the hospital.

Implement authorization concepts such as:

```text
Doctor
   ↓
Assigned Department
   ↓
Patient Encounter
   ↓
Authorized Clinical Access
```

Sensitive operations must verify both:

- role permission
- resource access

---

# 16. MODULE 2 — PATIENT MANAGEMENT

Patient capabilities:

- register patient
- retrieve patient
- update patient
- search patient
- pagination
- filtering
- contact information
- emergency contact
- allergies
- medical history
- insurance information
- documents
- patient status

Patient search should support:

- patient ID
- name
- phone
- email
- date of birth

Use proper indexing.

Do not expose sensitive patient information unnecessarily.

---

# 17. MODULE 3 — DEPARTMENT & STAFF MANAGEMENT

Departments:

```text
Cardiology
Neurology
Orthopedics
Pediatrics
Radiology
Pathology
Pharmacy
Emergency
General Medicine
```

Support:

- department creation
- department assignment
- staff assignment
- doctor assignment
- department status
- department search

---

# 18. MODULE 4 — DOCTOR SCHEDULE MANAGEMENT

Model:

- working days
- working hours
- breaks
- leave
- holidays
- appointment duration
- schedule exceptions

Example:

```text
Doctor:
Monday
09:00 - 13:00

Break:
11:00 - 11:30
```

The scheduling engine must respect these constraints.

---

# 19. MODULE 5 — APPOINTMENT MANAGEMENT

Implement:

- appointment booking
- rescheduling
- cancellation
- confirmation
- check-in
- no-show
- completion

Appointment states:

```text
REQUESTED
CONFIRMED
CHECKED_IN
IN_PROGRESS
COMPLETED
CANCELLED
NO_SHOW
```

Prevent invalid state transitions.

Example:

```text
CANCELLED → COMPLETED
```

must be rejected.

---

# 20. DOUBLE-BOOKING PREVENTION

This is a major interview requirement.

Two users may attempt:

```text
Doctor A
10:00 AM
Patient X

Doctor A
10:00 AM
Patient Y
```

simultaneously.

The application must prevent double booking.

Demonstrate:

- race condition
- optimistic locking and/or pessimistic locking
- unique database constraints where appropriate
- transaction isolation
- concurrent integration testing

Do not solve concurrency only with:

```java
if (!exists()) {
    save();
}
```

because that is unsafe under concurrent requests.

---

# 21. MODULE 6 — PATIENT QUEUE

Implement hospital queue management.

Example:

```text
REGISTERED
    ↓
WAITING
    ↓
CALLED
    ↓
IN_CONSULTATION
    ↓
COMPLETED
```

Priority:

```text
EMERGENCY
URGENT
NORMAL
```

Support:

- queue creation
- patient check-in
- queue position
- priority
- calling patient
- skipping
- completion
- cancellation

Avoid race conditions when multiple receptionists operate on the queue.

---

# 22. MODULE 7 — CONSULTATION

A consultation may contain:

- symptoms
- patient history
- vitals
- diagnosis entered by authorized staff
- treatment notes
- prescription
- laboratory orders
- follow-up instructions

Consultation lifecycle:

```text
STARTED
→ IN_PROGRESS
→ COMPLETED
```

Only authorized staff may access or modify clinical information.

---

# 23. MODULE 8 — CLINICAL RECORDS

Model:

- allergies
- medical history
- vitals
- diagnoses
- clinical notes
- procedures

Historical records must not simply be overwritten.

Where appropriate, maintain:

```text
createdAt
updatedAt
createdBy
updatedBy
```

Sensitive clinical modifications must generate audit records.

---

# 24. MODULE 9 — PRESCRIPTION

Entities:

- Prescription
- PrescriptionItem
- Medication

Prescription item:

```text
Medication
Dosage
Frequency
Duration
Instructions
Quantity
```

Example:

```text
Medication: Example Medicine
Dosage: 500 mg
Frequency: Twice daily
Duration: 5 days
```

Do not hard-code medical recommendations.

---

# 25. MODULE 10 — PHARMACY

Workflow:

```text
Prescription
      ↓
Pharmacy Review
      ↓
Inventory Check
      ↓
Dispensing
      ↓
Completed
```

Inventory should maintain:

- medication
- batch
- quantity
- expiry date
- reorder threshold

Prevent dispensing more medication than available inventory.

---

# 26. PHARMACY CONCURRENCY

Demonstrate this failure scenario:

```text
Available stock = 1

Pharmacist A → dispenses 1
Pharmacist B → dispenses 1
```

Only one operation may succeed.

Implement appropriate concurrency protection.

Explain:

- optimistic locking
- pessimistic locking
- atomic database updates
- transaction boundaries

and why the chosen solution is appropriate.

---

# 27. MODULE 11 — LABORATORY

Workflow:

```text
Doctor
  ↓
Lab Order
  ↓
Sample Collection
  ↓
Processing
  ↓
Result
  ↓
Doctor Review
```

Statuses:

```text
ORDERED
SAMPLE_COLLECTED
PROCESSING
COMPLETED
CANCELLED
```

Support:

- lab test catalog
- lab orders
- samples
- results
- result review
- report generation metadata

---

# 28. MODULE 12 — ADMISSION & BED MANAGEMENT

Support inpatient operations:

- admission
- ward
- room
- bed
- transfer
- discharge

Bed states:

```text
AVAILABLE
OCCUPIED
RESERVED
MAINTENANCE
```

Prevent two patients from occupying the same bed.

Demonstrate concurrency protection.

Workflow:

```text
Admission
   ↓
Bed Allocation
   ↓
Treatment
   ↓
Transfer
   ↓
Discharge
```

---

# 29. MODULE 13 — BILLING

Billing must use:

```java
BigDecimal
```

Never use:

```java
double
```

for money.

Billing sources:

- consultation
- laboratory
- pharmacy
- procedures
- room
- admission
- miscellaneous services

Model:

```text
Invoice
InvoiceItem
Payment
```

Invoice states:

```text
DRAFT
ISSUED
PARTIALLY_PAID
PAID
CANCELLED
```

---

# 30. MONEY & ROUNDING

Define explicit rules for:

- currency
- scale
- rounding
- tax
- discounts
- insurance coverage
- patient responsibility

Never allow implicit floating-point calculations for money.

Test rounding edge cases.

---

# 31. MODULE 14 — PAYMENT

Support:

- payment creation
- payment status
- partial payment
- failed payment
- refund metadata
- payment reference

Payment statuses:

```text
INITIATED
SUCCESS
FAILED
REFUNDED
```

---

# 32. PAYMENT IDEMPOTENCY

The frontend or payment provider may retry the same request.

Example:

```text
POST /payments
Idempotency-Key: abc-123
```

The same request must not create two payments.

Implement idempotency where appropriate.

Demonstrate:

```text
duplicate request
network retry
timeout
client retry
```

---

# 33. MODULE 15 — INSURANCE

Entities:

- InsuranceProvider
- InsurancePolicy
- InsuranceClaim

Claim lifecycle:

```text
DRAFT
SUBMITTED
UNDER_REVIEW
APPROVED
PARTIALLY_APPROVED
REJECTED
SETTLED
```

Implement:

- claim creation
- claim submission
- review
- approval/rejection
- settlement
- claim documents

Invalid state transitions must be rejected.

---

# 34. MODULE 16 — DOCUMENT MANAGEMENT

Support document metadata:

```text
Document
DocumentType
Owner
UploadedBy
Version
Status
CreatedAt
UpdatedAt
```

Examples:

- lab report
- prescription
- discharge summary
- insurance document
- consent form
- diagnostic report

Use metadata and references rather than storing large files directly in the database unless there is a strong reason.

---

# 35. MODULE 17 — NOTIFICATIONS

Notification types:

- appointment confirmation
- appointment cancellation
- appointment reminder
- lab result available
- prescription ready
- invoice generated
- insurance claim update

Channels:

```text
EMAIL
SMS
IN_APP
```

Initially implement a simple reliable notification abstraction.

Later introduce Kafka where asynchronous processing provides clear value.

---

# 36. MODULE 18 — AUDIT

Audit sensitive operations.

Audit fields:

```text
id
actorUserId
action
resourceType
resourceId
patientId
timestamp
previousValue
newValue
ipAddress
correlationId
```

Examples:

```text
PATIENT_VIEWED
PATIENT_UPDATED
CLINICAL_RECORD_UPDATED
PRESCRIPTION_CREATED
LAB_RESULT_VIEWED
INVOICE_UPDATED
CLAIM_APPROVED
```

Audit logs must not be casually deleted or modified.

---

# 37. MODULE 19 — REPORTING

Provide operational reports such as:

- appointments per day
- appointment cancellation rate
- doctor utilization
- patient queue statistics
- lab turnaround time
- pharmacy inventory
- outstanding invoices
- payment summary
- insurance claim summary
- admissions
- bed occupancy

Use efficient queries.

Do not load millions of records into Java memory just to calculate a simple aggregate.

Prefer database aggregation.

---

# 38. MODULE 20 — ADMINISTRATION

Administrative functionality:

- user management
- role management
- department configuration
- appointment configuration
- medication catalog
- lab test catalog
- billing configuration
- notification configuration
- system settings

All administrative operations must be secured and audited.

---

# 39. REST API DESIGN

Use RESTful APIs.

Examples:

```text
POST   /api/v1/patients
GET    /api/v1/patients/{id}
GET    /api/v1/patients
PUT    /api/v1/patients/{id}
```

Appointments:

```text
POST   /api/v1/appointments
GET    /api/v1/appointments/{id}
PUT    /api/v1/appointments/{id}
POST   /api/v1/appointments/{id}/cancel
POST   /api/v1/appointments/{id}/check-in
```

Use:

```text
/api/v1/
```

for versioning.

---

# 40. DTO DESIGN

Never expose JPA entities directly from controllers.

Use:

```text
CreatePatientRequest
UpdatePatientRequest
PatientResponse
AppointmentResponse
InvoiceResponse
```

Use dedicated request/response DTOs.

Use mappers between:

```text
Entity ↔ DTO
```

---

# 41. VALIDATION

Use Jakarta Bean Validation.

Examples:

```java
@NotBlank
@Email
@NotNull
@Size
@Positive
@PositiveOrZero
@Past
```

Validation errors must return a consistent API response.

---

# 42. GLOBAL ERROR HANDLING

Create centralized exception handling using:

```java
@RestControllerAdvice
```

Use a consistent error contract.

Example:

```json
{
  "timestamp": "2026-09-11T10:15:30Z",
  "status": 409,
  "code": "APPOINTMENT_SLOT_UNAVAILABLE",
  "message": "The selected appointment slot is no longer available.",
  "path": "/api/v1/appointments",
  "correlationId": "..."
}
```

Do not expose:

- stack traces
- SQL
- internal class names
- sensitive information

---

# 43. HTTP STATUS CODES

Use correct HTTP semantics.

Examples:

```text
200 OK
201 CREATED
204 NO CONTENT
400 BAD REQUEST
401 UNAUTHORIZED
403 FORBIDDEN
404 NOT FOUND
409 CONFLICT
422 UNPROCESSABLE ENTITY
429 TOO MANY REQUESTS
500 INTERNAL SERVER ERROR
```

Explain why each is selected.

---

# 44. SECURITY

Implement:

- Spring Security
- JWT
- password hashing
- RBAC
- method-level security
- resource authorization
- input validation
- secure headers
- CORS configuration
- authentication failure handling
- authorization failure handling

Do not hard-code secrets.

Use environment variables/configuration.

---

# 45. SECURITY TESTING

Test:

```text
Unauthenticated user
Authenticated user
Wrong role
Correct role
Unauthorized patient access
Expired JWT
Invalid JWT
Locked account
Missing permission
```

Security must be tested at API level, not just Angular UI level.

---

# 46. API DOCUMENTATION

Use OpenAPI/Swagger.

Document:

- endpoints
- request DTOs
- response DTOs
- validation errors
- authentication
- authorization
- status codes
- examples

---

# 47. ANGULAR ARCHITECTURE

Organize frontend by feature.

Example:

```text
src/app/
├── core/
├── shared/
├── auth/
├── patients/
├── appointments/
├── queue/
├── consultation/
├── pharmacy/
├── laboratory/
├── billing/
├── insurance/
├── administration/
└── reports/
```

Avoid a giant shared module containing everything.

---

# 48. ANGULAR CORE

Implement:

- route guards
- auth service
- JWT handling
- HTTP interceptor
- global error handling
- loading state
- reusable UI components
- reusable form components
- pagination
- filtering
- notifications

---

# 49. ANGULAR UX

The UI should support realistic hospital workflows.

Examples:

Receptionist:

```text
Dashboard
Patients
Appointments
Check-in
Queue
```

Doctor:

```text
Dashboard
Today's appointments
Patient details
Consultation
Clinical records
Prescriptions
Lab orders
```

Pharmacist:

```text
Dashboard
Pending prescriptions
Inventory
Dispensing
```

Billing:

```text
Invoices
Payments
Insurance claims
Reports
```

Administrator:

```text
Users
Roles
Departments
Configuration
Audit
Reports
```

---

# 50. REDIS

Introduce Redis only after the core application works.

Potential uses:

- caching patient search/reference data where appropriate
- frequently accessed configuration
- rate limiting
- distributed coordination if justified

Do not cache sensitive data blindly.

Document:

```text
Cache key
TTL
Invalidation strategy
Failure behavior
Consistency implications
```

---

# 51. KAFKA

Introduce Kafka for meaningful asynchronous workflows.

Potential events:

```text
AppointmentBooked
AppointmentCancelled
AppointmentCheckedIn
LabResultCompleted
PrescriptionDispensed
InvoiceCreated
PaymentCompleted
InsuranceClaimUpdated
```

Use event-driven architecture where it provides genuine value.

Discuss:

- producer
- consumer
- topic
- partition
- consumer group
- retry
- dead-letter topic
- idempotency
- duplicate events
- ordering

---

# 52. EVENTUAL CONSISTENCY

When asynchronous processing is introduced, explicitly identify where eventual consistency exists.

Example:

```text
Appointment booking
       ↓
transaction commits
       ↓
AppointmentBooked event
       ↓
notification service
```

If notification fails:

```text
appointment must remain successfully booked
```

Do not roll back the appointment simply because an email failed.

---

# 53. OUTBOX PATTERN

Where appropriate, implement an Outbox Pattern.

Example:

```text
Database transaction
        │
        ├── Appointment
        │
        └── Outbox Event
                 ↓
              Kafka
                 ↓
            Notification
```

This should demonstrate reliable event publishing.

---

# 54. OBSERVABILITY

Implement:

- structured logging
- correlation IDs
- request IDs
- Spring Boot Actuator
- Micrometer
- metrics
- health checks

Potential metrics:

```text
appointment.booking.success
appointment.booking.conflict
queue.wait.time
lab.processing.time
payment.success
payment.failure
notification.failure
http.request.duration
database.query.duration
```

---

# 55. LOGGING

Use structured logs.

Every important request should be traceable through:

```text
correlationId
userId
module
operation
```

Never log:

- passwords
- JWTs
- payment secrets
- unnecessary medical information
- sensitive personal information

---

# 56. PERFORMANCE ENGINEERING

The project must include realistic performance problems.

Demonstrate and solve:

### Problem 1
Patient search becomes slow with millions of records.

Investigate:

- indexes
- query plan
- pagination
- projections
- full table scans

### Problem 2
Patient dashboard executes hundreds of queries.

Investigate:

- N+1
- entity graphs
- fetch joins
- DTO projections

### Problem 3
Appointment search becomes slow.

Investigate:

- composite indexes
- filtering
- sorting
- pagination

---

# 57. CONCURRENCY LABS

Create explicit failure scenarios.

At minimum:

### Lab 1
Double-booked appointment.

### Lab 2
Two pharmacists dispense the last item.

### Lab 3
Two receptionists allocate the same queue position.

### Lab 4
Two users allocate the same bed.

### Lab 5
Duplicate payment request.

### Lab 6
Two users update the same clinical record.

For every scenario document:

```text
Problem
Race condition
Root cause
Incorrect solution
Correct solution
Database behavior
Transaction behavior
Test
```

---

# 58. TESTING STRATEGY

Use:

- JUnit 5
- Mockito
- Spring Boot Test
- MockMvc
- Testcontainers
- MySQL integration tests
- REST Assured where useful

Testing layers:

```text
Unit Tests
Integration Tests
Repository Tests
Controller Tests
Security Tests
Concurrency Tests
End-to-End Tests
```

---

# 59. UNIT TESTING

Unit test:

- services
- business rules
- state transitions
- validation logic
- billing calculations
- authorization rules

Do not mock everything blindly.

---

# 60. REPOSITORY TESTING

Test:

- queries
- pagination
- filtering
- projections
- indexes where measurable
- entity relationships
- transaction behavior

Use a real MySQL container for important integration behavior.

---

# 61. INTEGRATION TESTING

Use Testcontainers.

Example:

```text
Application
   ↓
Spring Boot
   ↓
Hibernate
   ↓
MySQL Testcontainer
```

Avoid relying only on H2 when MySQL-specific behavior matters.

---

# 62. CONCURRENCY TESTING

Write actual concurrent tests.

Example:

```text
10 threads
    ↓
same appointment slot
    ↓
10 booking requests
```

Expected:

```text
1 success
9 conflicts
```

The test must verify the actual database behavior.

---

# 63. TEST DATA

Use synthetic data.

Provide:

- seed users
- seed roles
- seed departments
- seed doctors
- seed patients
- seed appointments
- seed medications
- seed lab tests
- seed billing records

Never use real patient data.

---

# 64. DATABASE MIGRATIONS

Every schema change requires:

```text
Flyway migration
```

Never manually modify production schema.

Migration naming:

```text
V1__initial_schema.sql
V2__create_patient_tables.sql
V3__create_appointment_tables.sql
```

Migrations must be version controlled.

---

# 65. CONFIGURATION

Use profiles:

```text
application.yml
application-local.yml
application-test.yml
application-prod.yml
```

Secrets must come from environment variables.

Never commit:

```text
password
JWT secret
API keys
database credentials
```

---

# 66. DOCKER

Create Docker support for:

- backend
- frontend
- MySQL
- Redis
- Kafka

Use Docker Compose for local infrastructure.

Example:

```text
Angular
   ↓
Spring Boot
   ↓
MySQL

Spring Boot
   ↓
Redis

Spring Boot
   ↓
Kafka
```

---

# 67. CI/CD

Create GitHub Actions pipeline.

Pipeline:

```text
Checkout
   ↓
Build
   ↓
Unit Tests
   ↓
Integration Tests
   ↓
Static Analysis
   ↓
Package
   ↓
Docker Build
```

The build must fail when tests fail.

---

# 68. CODE QUALITY

Follow:

- SOLID
- DRY
- KISS
- clean code
- meaningful naming
- small methods
- clear responsibilities

Avoid:

- god classes
- god services
- duplicated business logic
- static utility abuse
- excessive inheritance
- unnecessary abstraction
- premature microservices

---

# 69. STATE MACHINE DESIGN

Important workflows must explicitly define valid state transitions.

Example:

```text
REQUESTED
    ↓
CONFIRMED
    ↓
CHECKED_IN
    ↓
IN_PROGRESS
    ↓
COMPLETED
```

Invalid transitions must produce a controlled business exception.

Do not allow arbitrary status updates such as:

```text
appointment.setStatus(COMPLETED)
```

from any endpoint.

---

# 70. TIME & TIMEZONE

Use:

```text
Instant
OffsetDateTime
LocalDate
LocalTime
```

appropriately.

Do not use `java.util.Date` throughout the application.

Appointment scheduling must explicitly define timezone behavior.

Store timestamps consistently.

Test:

- timezone conversion
- daylight saving behavior where relevant
- date boundaries
- appointment overlap

---

# 71. PAGINATION

Every potentially large collection API must support pagination.

Example:

```text
GET /api/v1/patients?page=0&size=20
```

Support:

- page
- size
- sorting
- filtering

Do not return thousands of records by default.

---

# 72. SEARCH

Implement realistic search behavior.

Example:

```text
GET /api/v1/patients?name=rohit
```

Support combinations such as:

```text
status
department
doctor
date range
appointment status
```

Use Specifications or appropriate query mechanisms where useful.

---

# 73. API IDEMPOTENCY

Identify APIs where retries can create duplicate side effects.

Examples:

- payment
- appointment booking
- prescription dispensing
- claim submission

Implement idempotency where appropriate.

---

# 74. AUDITABILITY

For every sensitive workflow answer:

```text
Who performed it?
What changed?
When?
On which resource?
For which patient?
From where?
What was the previous value?
What is the new value?
```

---

# 75. FAILURE HANDLING

Every major module must document:

```text
Expected failure
Unexpected failure
Database failure
Network failure
Dependency failure
Concurrent update
Timeout
Retry behavior
Recovery strategy
```

---

# 76. REQUIRED FAILURE SCENARIOS

At minimum implement/document:

1. Appointment double booking
2. Pharmacy stock race condition
3. Bed allocation race condition
4. Queue race condition
5. Duplicate payment
6. Notification failure after appointment creation
7. Kafka duplicate event
8. Kafka consumer failure
9. Database connection failure
10. Redis unavailable
11. Slow patient search
12. Hibernate N+1
13. Optimistic locking conflict
14. Deadlock
15. Unauthorized patient access
16. Invalid state transition
17. Partial payment failure
18. Insurance claim retry
19. Batch reminder job failure
20. Missing audit record

---

# 77. DEBUGGING MANUAL

Create:

```text
docs/DEBUGGING.md
```

Include practical troubleshooting instructions.

Examples:

### Application won't start

Check:

```text
Java version
Maven
environment variables
database
Flyway
port conflicts
```

### Database connection failure

Check:

```text
MySQL status
credentials
JDBC URL
network
connection pool
```

### Hibernate error

Investigate:

```text
entity mapping
lazy loading
transaction boundary
cascade
foreign key
```

### N+1 problem

Use:

```text
Hibernate SQL logging
statistics
query inspection
```

### Appointment conflict

Investigate:

```text
transaction
locking
unique constraint
isolation
```

### JWT failure

Investigate:

```text
token expiry
signature
issuer
roles
authorities
```

---

# 78. ARCHITECTURE DECISION RECORDS

Create:

```text
docs/adr/
```

At minimum:

```text
ADR-001-modular-monolith.md
ADR-002-hibernate-jpa.md
ADR-003-mysql.md
ADR-004-flyway.md
ADR-005-jwt-security.md
ADR-006-optimistic-locking.md
ADR-007-redis.md
ADR-008-kafka.md
ADR-009-outbox-pattern.md
ADR-010-observability.md
```

Each ADR must explain:

```text
Context
Problem
Options
Decision
Trade-offs
Consequences
```

---

# 79. DOCUMENTATION

Maintain:

```text
README.md
docs/
    ARCHITECTURE.md
    DATABASE.md
    API.md
    SECURITY.md
    TESTING.md
    DEBUGGING.md
    DEPLOYMENT.md
    OBSERVABILITY.md
    FAILURE-SCENARIOS.md
    CONTRIBUTING.md
    adr/
```

Documentation must evolve with implementation.

Do not create fake documentation for functionality that does not exist.

---

# 80. README

README must eventually contain:

- project overview
- architecture diagram
- technology stack
- module map
- setup instructions
- environment variables
- database setup
- Docker setup
- API documentation
- testing
- security
- observability
- troubleshooting
- screenshots
- sample credentials
- interview highlights

---

# 81. GIT STRATEGY

This is extremely important.

Development must happen:

# ONE MEANINGFUL FUNCTIONALITY = ONE GIT COMMIT

Do NOT combine many unrelated features into one commit.

Examples:

```text
feat(patient): create patient domain model

feat(patient): implement patient registration

feat(patient): implement patient retrieval

feat(patient): add patient search

feat(patient): add patient pagination

feat(appointment): implement appointment booking

fix(appointment): prevent duplicate slot booking

test(appointment): add concurrent booking test
```

---

# 82. COMMIT REQUIREMENTS

Every commit must contain:

1. One meaningful functionality
2. Production-quality implementation
3. Tests
4. Documentation where applicable
5. Passing build
6. Clean code
7. No unrelated changes

Before committing:

```text
Run tests
Inspect diff
Review code
Verify database migration
Verify API behavior
Verify frontend behavior where applicable
```

---

# 83. COMMIT MESSAGE

Every commit message should follow Conventional Commits.

Example:

```text
feat(patient): implement patient registration
```

The commit body should explain:

```text
- Added Patient entity
- Added PatientRepository
- Added registration DTO
- Added validation
- Added service transaction
- Added controller endpoint
- Added Flyway migration
- Added unit tests
- Added integration tests

Why:
Introduces the initial patient registration workflow.

Testing:
mvn test
```

---

# 84. DEVELOPMENT LOOP

For EVERY functionality follow this exact process:

```text
1. Understand requirement
2. Inspect current repository
3. Identify existing implementation
4. Identify missing functionality
5. Define business rules
6. Design domain model
7. Design database changes
8. Create Flyway migration
9. Implement backend
10. Implement validation
11. Implement security
12. Implement API
13. Implement Angular UI
14. Add tests
15. Run integration tests
16. Simulate failure scenario
17. Debug if necessary
18. Review code
19. Update documentation
20. Review git diff
21. Create ONE commit
22. Push branch
23. STOP
```

Do not automatically continue to the next functionality.

---

# 85. STOP RULE

After completing one functionality:

STOP.

Do not implement the next functionality automatically.

Report:

```text
Completed functionality
Files changed
Database changes
APIs added
Frontend changes
Tests added
Failure scenario tested
Documentation updated
Git commit
Push result
Recommended next functionality
```

Then wait for my instruction.

---

# 86. REPOSITORY-FIRST RULE

Before changing code:

Inspect the repository.

Determine:

```text
What already exists?
What works?
What is incomplete?
What is broken?
What architecture is currently used?
What database migrations exist?
What tests exist?
What frontend structure exists?
```

Do NOT blindly recreate existing code.

If the repository already contains a working implementation, preserve it unless there is a strong reason to change it.

---

# 87. NO FAKE IMPLEMENTATION

Never create:

```text
TODO
placeholder
dummy service
fake success response
hard-coded database response
mock production behavior
```

unless explicitly requested for a temporary scaffold.

If something is intentionally incomplete, clearly mark it.

---

# 88. BUSINESS RULES FIRST

Before implementation, define:

```text
Actors
Preconditions
Business rules
State transitions
Validation
Authorization
Transaction boundary
Concurrency risk
Failure behavior
```

Then implement.

---

# 89. API-FIRST DEVELOPMENT

For every backend functionality:

```text
Requirement
 ↓
Domain
 ↓
Database
 ↓
Service
 ↓
REST API
 ↓
Tests
 ↓
Angular
```

Do not build UI behavior that the backend does not support.

---

# 90. FRONTEND-BACKEND CONTRACT

Maintain consistent API contracts.

If backend changes:

```text
DTO
status code
field name
validation
error contract
```

update Angular accordingly.

Do not allow frontend and backend contracts to silently diverge.

---

# 91. SECURITY-FIRST RULE

For every endpoint ask:

```text
Who can call this?
What role is required?
Which resource can they access?
What data can they see?
What data can they modify?
Does this operation require audit logging?
```

---

# 92. DATABASE-FIRST CONCURRENCY RULE

For every workflow involving:

- booking
- inventory
- bed allocation
- payments
- queue position
- concurrent updates

ask:

```text
What happens if two requests arrive at exactly the same time?
```

Never assume requests happen sequentially.

---

# 93. INTERVIEW PREPARATION

Create:

```text
docs/INTERVIEW_GUIDE.md
```

For every major feature include:

### What did you build?

### Why did you design it this way?

### Why Spring Boot?

### Why Hibernate/JPA?

### Why MySQL?

### Why modular monolith?

### Why optimistic locking?

### Why pessimistic locking?

### How did you prevent double booking?

### How did you solve N+1?

### How did you secure patient data?

### How does JWT authentication work?

### How does transaction management work?

### How did you handle duplicate payments?

### Why Redis?

### Why Kafka?

### What happens when Kafka is unavailable?

### How do you handle duplicate events?

### How do you monitor the application?

### How would you scale this system?

### When would you split modules into microservices?

---

# 94. SYSTEM DESIGN INTERVIEW QUESTIONS

The project should prepare me for questions such as:

```text
Design a hospital appointment system.

How would you prevent double booking?

How would you design a patient search system for 100 million records?

How would you design a hospital queue?

How would you guarantee pharmacy inventory consistency?

How would you design payment idempotency?

How would you process notifications asynchronously?

How would you guarantee event delivery?

How would you handle duplicate Kafka events?

How would you secure medical records?

How would you audit sensitive data access?

How would you scale appointment booking?

How would you monitor the system?

How would you migrate the modular monolith into microservices?
```

Provide strong interview answers based on the actual implementation.

---

# 95. SCALABILITY TARGETS

Design with realistic growth assumptions.

Example:

```text
Patients: 10M+
Appointments: 100M+
Clinical records: 100M+
Documents: millions
Concurrent users: thousands
```

Do not prematurely optimize for these numbers.

However, architecture should not make future scaling impossible.

---

# 96. DATABASE INDEXING

For important queries identify indexes.

Examples:

```text
patients(phone)
patients(email)
patients(last_name, first_name)
appointments(doctor_id, appointment_date_time)
appointments(patient_id, appointment_date_time)
appointments(status)
pharmacy_inventory(medication_id, expiry_date)
lab_orders(patient_id, status)
invoices(patient_id, status)
insurance_claims(status)
```

Only create indexes justified by query patterns.

---

# 97. API RATE LIMITING

Where appropriate, protect:

- login
- password reset
- patient search
- public endpoints

Redis may later be used for distributed rate limiting.

---

# 98. BACKGROUND JOBS

Implement scheduled jobs where appropriate.

Examples:

```text
appointment reminders
expired medication detection
insurance claim follow-up
invoice reminders
cleanup of temporary data
```

Jobs must be:

- restartable
- observable
- idempotent
- failure-aware

Do not assume a scheduled job always completes successfully.

---

# 99. BATCH JOB FAILURE SCENARIO

Example:

```text
10,000 appointment reminders
```

Job processes:

```text
1,000
```

then crashes.

On restart, it must not blindly resend all 10,000 notifications.

Design a restart/idempotency strategy.

---

# 100. HEALTHCARE DATA PRIVACY

Treat patient information as highly sensitive.

Implement:

- least privilege
- resource authorization
- audit trails
- minimal data exposure
- secure logging
- secure APIs
- encrypted transport assumptions
- secret management

Do not claim regulatory compliance such as HIPAA/GDPR certification unless explicitly implemented and verified.

You may describe the system as:

```text
designed with healthcare privacy principles
```

rather than claiming formal compliance.

---

# 101. DEMO USERS

Create synthetic demo accounts.

Example:

```text
admin@careflow.local
doctor@careflow.local
nurse@careflow.local
receptionist@careflow.local
pharmacist@careflow.local
lab@careflow.local
billing@careflow.local
```

Use clearly documented demo passwords.

Never use these credentials in production.

---

# 102. SAMPLE END-TO-END WORKFLOW

The finished system should demonstrate:

```text
Patient Registration
       ↓
Appointment Booking
       ↓
Appointment Confirmation
       ↓
Patient Check-in
       ↓
Queue
       ↓
Doctor Consultation
       ↓
Prescription + Lab Order
       ↓
Pharmacy Dispensing
       ↓
Lab Sample
       ↓
Lab Result
       ↓
Doctor Review
       ↓
Invoice
       ↓
Payment
       ↓
Notification
       ↓
Audit
```

This workflow should eventually be demonstrable from the Angular UI.

---

# 103. PROJECT PHASES

Implement progressively.

## PHASE 0 — Foundation

- repository setup
- Maven
- Spring Boot
- Angular
- MySQL
- Flyway
- Docker
- base configuration
- package structure
- exception handling
- logging
- API conventions

---

## PHASE 1 — Identity

- users
- roles
- permissions
- authentication
- JWT
- authorization

---

## PHASE 2 — Patient Management

- patient registration
- retrieval
- update
- search
- pagination
- history
- allergies

---

## PHASE 3 — Staff & Departments

- staff
- doctors
- departments
- assignments

---

## PHASE 4 — Scheduling

- doctor schedules
- leave
- holidays
- appointment slots

---

## PHASE 5 — Appointments

- booking
- cancellation
- rescheduling
- check-in
- status lifecycle
- double-booking prevention

---

## PHASE 6 — Queue

- queue
- priority
- calling
- completion
- concurrency

---

## PHASE 7 — Consultation & Clinical Records

- encounters
- vitals
- clinical notes
- diagnoses
- treatment records

---

## PHASE 8 — Prescription & Pharmacy

- prescriptions
- medication catalog
- inventory
- dispensing
- concurrency

---

## PHASE 9 — Laboratory

- lab catalog
- orders
- samples
- processing
- results
- review

---

## PHASE 10 — Admission

- wards
- rooms
- beds
- admission
- transfer
- discharge
- concurrency

---

## PHASE 11 — Billing

- invoice
- invoice items
- taxes
- discounts
- payment
- idempotency

---

## PHASE 12 — Insurance

- providers
- policies
- claims
- approval
- rejection
- settlement

---

## PHASE 13 — Documents

- document metadata
- versioning
- access control
- audit

---

## PHASE 14 — Notifications

- in-app notifications
- email abstraction
- appointment reminders

---

## PHASE 15 — Audit

- sensitive access logging
- audit search
- audit reporting

---

## PHASE 16 — Reporting

- operational dashboards
- KPIs
- aggregation queries
- pagination

---

## PHASE 17 — Redis

Introduce caching/rate limiting only where justified.

---

## PHASE 18 — Kafka

Introduce asynchronous domain events.

---

## PHASE 19 — Outbox

Implement reliable event publishing.

---

## PHASE 20 — Observability

- metrics
- tracing
- dashboards
- health checks
- structured logs

---

## PHASE 21 — Performance

- N+1 analysis
- query optimization
- indexes
- load testing
- concurrency testing

---

## PHASE 22 — CI/CD

- GitHub Actions
- automated tests
- Docker image
- quality checks

---

## PHASE 23 — Production Readiness

- security review
- performance review
- failure testing
- documentation
- architecture review
- deployment documentation

---

# 104. DEFINITION OF DONE

A functionality is NOT complete until:

```text
Business rules implemented
Database migration created
Backend implemented
Validation implemented
Authorization implemented
API implemented
Frontend implemented where applicable
Unit tests written
Integration tests written where appropriate
Failure case tested
Logging added where appropriate
Audit added where required
Documentation updated
Code reviewed
Git diff reviewed
Build passes
Tests pass
Commit created
Branch pushed
```

---

# 105. DO NOT DO THESE THINGS

Never:

- skip tests
- expose JPA entities
- use `double` for money
- use `ddl-auto=update`
- store plain-text passwords
- hard-code secrets
- ignore concurrency
- ignore authorization
- create giant services
- create microservices prematurely
- swallow exceptions
- log sensitive data
- use fake repository implementations
- return fake success responses
- silently change requirements
- implement multiple unrelated features in one commit
- automatically continue after a completed commit

---

# 106. WHEN I ASK FOR A FEATURE

Before writing code, respond with:

```text
Requirement
Business Rules
Affected Modules
Domain Model
Database Changes
API Design
Security
Transaction Boundary
Concurrency Considerations
Testing Strategy
Implementation Plan
```

Then implement only the requested functionality.

---

# 107. WHEN SOMETHING FAILS

Do not immediately rewrite everything.

Use this debugging process:

```text
Observe
 ↓
Reproduce
 ↓
Collect logs
 ↓
Identify failing layer
 ↓
Form hypothesis
 ↓
Verify hypothesis
 ↓
Fix root cause
 ↓
Add regression test
 ↓
Re-run full relevant tests
```

Explain the root cause.

---

# 108. WHEN I ASK "WHY?"

Teach me.

Do not merely give the answer.

Explain:

```text
Concept
Why it matters
How it works
How CareFlow uses it
Common mistake
Production consequence
Interview answer
```

---

# 109. WHEN I ASK FOR CODE

Provide:

- production-quality code
- complete files where practical
- correct imports
- package names
- configuration
- migrations
- tests

Do not provide incomplete pseudo-code unless I explicitly ask for pseudocode.

---

# 110. WHEN I ASK FOR A DIAGRAM

Use text diagrams such as:

```text
Angular
   |
   | REST
   ↓
Spring Boot
   |
   +---- Identity
   |
   +---- Patient
   |
   +---- Appointment
   |
   +---- Clinical
   |
   +---- Pharmacy
   |
   +---- Laboratory
   |
   +---- Billing
   |
   +---- Insurance
   |
   ↓
Hibernate/JPA
   |
   ↓
MySQL
```

---

# 111. FINAL PROJECT QUALITY BAR

The final CareFlow system should look like something a serious engineering team could plausibly evolve into a production healthcare operations platform.

It should demonstrate mastery of:

```text
Java
Spring Boot
Spring Security
REST APIs
Hibernate/JPA
MySQL
Transactions
Concurrency
Database design
Angular
Testing
Docker
Redis
Kafka
Observability
CI/CD
Security
Performance
System design
Debugging
Git
Architecture
```

The objective is not to maximize the number of technologies.

The objective is to demonstrate that I understand **why, when, and how** each technology is used.

---

# 112. STARTING INSTRUCTION

When I first provide the repository:

DO NOT immediately start implementing the entire project.

First perform a:

## Repository & Architecture Audit

Inspect:

```text
directory structure
pom.xml
Angular package configuration
application configuration
database configuration
Flyway migrations
entities
repositories
services
controllers
security
tests
Docker
documentation
Git status
```

Then report:

```text
Current State
Existing Features
Missing Features
Broken Features
Architecture Assessment
Technical Debt
Recommended Next Functionality
```

Then implement ONLY the first agreed functionality.

After that functionality is completed:

```text
TEST
DEBUG
REVIEW
DOCUMENT
COMMIT
PUSH
STOP
```

Never proceed automatically to the next feature.

---

# 113. PRIMARY ENGINEERING PRINCIPLE

Throughout the project, think like a senior engineer.

For every feature ask:

```text
Is it correct?
Is it secure?
Is it transactional?
Is it concurrent-safe?
Is it testable?
Is it observable?
Is it maintainable?
Is it performant?
Is it auditable?
What happens when it fails?
```

Build CareFlow as an **enterprise engineering project**, not as a tutorial CRUD application.