# ADR-0016: Operational Reporting, Clinical Throughput Analytics, and Pushdown Database Aggregations

## Status
Accepted

## Context
CareFlow operates as an enterprise hospital and healthcare operations platform orchestrating high volumes of outpatient visits, inpatient admissions, laboratory diagnostics, pharmacy dispenses, and billing transactions (§37, §103 Phase 16).

Key architectural and performance challenges addressed:
1. **Memory Protection & Query Pushdown (§37, §86, §105)**:
   - Naive reporting architectures load thousands or millions of entity records into application heap memory to compute counts, sums, and averages in Java streams. This quickly leads to heap exhaustion (`OutOfMemoryError`), heavy GC pauses, and poor scalability.
   - All statistical metrics, volume breakdowns, and financial tallies must be executed as direct database aggregations (`COUNT`, `SUM`, `AVG`, `GROUP BY`, conditional `CASE WHEN`) at the SQL layer, returning scalar or tuple projection results directly to DTOs.
2. **Comprehensive Hospital Operations Visibility**:
   - Hospital executives, clinical directors, and department supervisors require cross-domain operational intelligence:
     - **Executive Dashboard**: Real-time snapshot of today's appointment load, active inpatient admissions, bed occupancy percentage, outpatient queue waiting counts, pending diagnostic lab orders, critical pharmacy stock alerts, and financial billing vs collection totals.
     - **Outpatient Appointments**: Daily appointment volume trends, cancellation rates, no-show rates, department volume distribution, and doctor schedule utilization.
     - **Outpatient Queue Flow**: Throughput by lifecycle status, queue priority stratification (NORMAL, URGENT, EMERGENCY), and average waiting time (minutes elapsed from queue entry to consultation call).
     - **Inpatient Admissions & Beds**: Inpatient census, active vs discharged counts, total bed capacity, overall bed occupancy rate, and ward-by-ward bed occupancy metrics.
     - **Diagnostic Laboratory**: Order volume, processing vs completion distribution, average test turnaround time (TAT in hours from order creation to completion), and top requested lab test profiles.
     - **Pharmacy Inventory & Alerts**: Total batch count, total medication units in stock, expired batches, batches nearing expiration (within 30 days), low stock alerts, stockouts, and critical batch lists.
     - **Revenue Cycle Management**: Total invoiced, total paid, total outstanding balances, collection efficiency percentage, invoice status distributions, insurance claim submission counts, claimed amounts, adjudicated paid amounts, and claim approval/denial ratios.
3. **Execution Auditing & Governance (§37, §74)**:
   - Generation of operational reports must be tracked for performance monitoring, compliance, and auditing.
   - The platform must record who requested each report, which parameters were applied, execution duration in milliseconds, and terminal completion status.
4. **Strict Role-Based Access Control (RBAC, §91)**:
   - Reporting endpoints access cross-domain operational and financial data. Access must be segregated by institutional role:
     - Executive Dashboard: `ROLE_ADMIN`, `ROLE_DOCTOR`
     - Appointment Analytics: `ROLE_ADMIN`, `ROLE_DOCTOR`, `ROLE_RECEPTIONIST`
     - Outpatient Queue Analytics: `ROLE_ADMIN`, `ROLE_DOCTOR`, `ROLE_NURSE`, `ROLE_RECEPTIONIST`
     - Inpatient Bed & Ward Analytics: `ROLE_ADMIN`, `ROLE_DOCTOR`, `ROLE_NURSE`
     - Diagnostic Laboratory Analytics: `ROLE_ADMIN`, `ROLE_DOCTOR`, `ROLE_LAB_TECHNICIAN`
     - Pharmacy Inventory Analytics: `ROLE_ADMIN`, `ROLE_PHARMACIST`
     - Financial & Billing Analytics: `ROLE_ADMIN`, `ROLE_BILLING_OFFICER`
     - Report Execution Audit History: `ROLE_ADMIN`

## Decision
1. **Package by Feature**:
   All reporting domain models, repositories, services, DTOs, controllers, and mappers reside under `com.careflow.reporting`.
2. **Domain Aggregate & Audit Entity**:
   - `ReportExecution`: Extends `BaseAuditEntity` to track report execution metadata (`reportType`, `requestedBy`, `parameters`, `executionTimeMs`, `status`).
   - `ReportType` enum: Standardizes report categories (`EXECUTIVE_DASHBOARD`, `APPOINTMENT_ANALYTICS`, `QUEUE_ANALYTICS`, `INPATIENT_ANALYTICS`, `LABORATORY_ANALYTICS`, `PHARMACY_ANALYTICS`, `FINANCIAL_ANALYTICS`).
3. **Database Schema & Indexing (Flyway V20)**:
   - Table `report_executions` created with indexes:
     - `idx_report_exec_type_created`: Filtering executions by report type and chronological ordering.
     - `idx_report_exec_requested_by`: Auditing reports requested by specific users.
4. **Repository Pushdown Aggregations**:
   - Implemented JPQL and projection queries across existing domain repositories without modifying transaction semantics:
     - `AppointmentRepository`: `countAppointmentsInRange`, `countAppointmentsInRangeByStatus`, `countAppointmentsByDepartmentInRange`, `countDoctorUtilizationInRange`, `findAppointmentsTimelineInRange`.
     - `QueueEntryRepository`: `countQueueByStatus`, `countQueueByPriority`, `findQueueWaitTimes`.
     - `BedRepository` & `AdmissionRepository`: `countBedsByStatus`, `countWardBedOccupancy`, `countAdmissionsByStatus`.
     - `LabOrderRepository`: `countLabOrdersByStatus`, `findCompletedLabOrderTimes`, `findTopRequestedLabTests`.
     - `PharmacyInventoryBatchRepository`: `getPharmacyInventoryAggregates`, `findCriticalStockBatches`, `countStockAlerts`.
     - `InvoiceRepository` & `InsuranceClaimRepository`: `getInvoiceFinancialTotals`, `countInvoicesByStatus`, `sumTotalInvoicedInRange`, `sumTotalPaidInRange`, `getInsuranceClaimAggregates`.
5. **Service Layer Analytics Engine (`ReportingServiceImpl`)**:
   - Aggregates multi-repository query results into strongly-typed immutable records (`DashboardSummaryResponse`, `AppointmentReportResponse`, `QueueReportResponse`, `InpatientReportResponse`, `LabReportResponse`, `PharmacyReportResponse`, `FinancialReportResponse`).
   - Computes percentages (cancellation rate, no-show rate, bed occupancy rate, collection rate, claim approval rate) using BigDecimal and double rounding to two decimal places, guarded against division-by-zero errors.
   - Enforces date chronological validation (`startDate <= endDate`) with `BusinessRuleException`.
   - Records execution audit trails asynchronously within the reporting transaction boundary.
6. **REST API Contract (`ReportingController`)**:
   - `GET /api/v1/reporting/dashboard`: Executive operational dashboard.
   - `GET /api/v1/reporting/appointments`: Outpatient appointment trends and doctor utilization.
   - `GET /api/v1/reporting/queue`: Outpatient queue throughput and wait time analysis.
   - `GET /api/v1/reporting/admissions`: Inpatient bed occupancy and ward capacity breakdown.
   - `GET /api/v1/reporting/laboratory`: Lab test turnaround time and top requested profiles.
   - `GET /api/v1/reporting/pharmacy`: Pharmacy stock valuation, expiration, and stockout alerts.
   - `GET /api/v1/reporting/financial`: Revenue cycle, invoice collection, and insurance claim metrics.
   - `GET /api/v1/reporting/history`: Paginated execution history of generated reports.

## Consequences
- **Positive**:
  - Zero memory overhead: Aggregations execute entirely inside MySQL 8+ engine.
  - Sub-second dashboard generation even with high volumes of hospital activity.
  - Complete operational visibility across all 19 CareFlow healthcare modules.
  - Robust governance: Every report generation attempt is audited with actor and execution latency.
- **Negative / Trade-offs**:
  - Date and time aggregations rely on indexed database columns; queries must use index-friendly date ranges rather than arbitrary unindexed function calls.
