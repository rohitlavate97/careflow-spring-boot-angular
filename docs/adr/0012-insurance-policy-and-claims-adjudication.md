# ADR-0012: Insurance Policy and Claims Adjudication Architecture

## Status
Accepted

## Context
Hospital billing systems interface with health insurance providers (third-party payers) to facilitate coverage verification, claims submission, Electronic Data Interchange (EDI) processing, adjudication, and financial settlement (§33, §103 Phase 12).

Key challenges in healthcare insurance management include:
1. **Policy Verification and Validity Window (§33)**:
   - Patients present insurance coverage from diverse commercial and public payers (Medicare, BlueCross, Aetna, etc.).
   - A policy must be validated against the clinical date of service to ensure coverage was active (`coverageStartDate <= serviceDate <= coverageEndDate`).
   - Co-pays, deductibles, and coverage percentages determine patient responsibility vs. insurance liability.
2. **Claim Lifecycle State Machine (§33)**:
   - Claims undergo rigorous administrative and payer workflows:
     `DRAFT` $\rightarrow$ `SUBMITTED` $\rightarrow$ `UNDER_REVIEW` $\rightarrow$ `APPROVED` | `PARTIALLY_APPROVED` | `REJECTED` $\rightarrow$ `SETTLED`.
   - Invalid status transitions (e.g. attempting to settle an unadjudicated claim or modifying a settled claim) must be strictly prevented.
3. **Financial Precision and Settlement Integrity (§29, §30)**:
   - All claimed amounts, approved amounts, deductibles, co-pays, and patient responsibility balances must strictly use `BigDecimal` with 2 decimal places and `RoundingMode.HALF_UP`.
   - Upon claim settlement, approved insurance payouts can be credited against the patient's itemized invoice as a formal `Payment` record with method `INSURANCE`.
4. **Concurrency & Payer Webhook Safety (§92)**:
   - Payer adjudication responses (via Clearinghouse webhooks or batched Electronic Remittance Advice - ERA 835) can arrive concurrently.
   - Claims must be protected using JPA pessimistic locking (`findByIdForUpdate`) to guarantee deterministic adjudication and settlement.

## Decision
1. **Package by Feature**:
   All insurance providers, policies, claims, claim items, repositories, services, DTOs, controllers, and mappers reside under `com.careflow.insurance`.
2. **Domain Architecture**:
   - `InsuranceProvider`: Third-party payer directory maintaining provider code, name, Payer ID, and contact details.
   - `InsurancePolicy`: Patient coverage contract maintaining policy number, group number, policyholder relationship, co-pay, coverage percentage, deductible, and validity date range.
   - `InsuranceClaim`: Aggregate root tracking claim number, policy, patient, invoice reference, claim lifecycle status, total claimed amount, approved amount, patient responsibility, denial reason, and timestamps.
   - `ClaimItem`: Service line item breakdown tracking CPT/service codes, claimed amount, approved amount, and line adjudication notes.
3. **Adjudication & Settlement Rules**:
   - Adjudication requires `UNDER_REVIEW` or `SUBMITTED` state.
   - `APPROVED`: `approvedAmount = totalClaimedAmount`, `patientResponsibility = 0.00`.
   - `PARTIALLY_APPROVED`: `0 < approvedAmount < totalClaimedAmount`, `patientResponsibility = totalClaimedAmount - approvedAmount`.
   - `REJECTED`: `approvedAmount = 0.00`, `patientResponsibility = totalClaimedAmount`, mandatory denial reason.
   - `SETTLED`: final state reached only from `APPROVED` or `PARTIALLY_APPROVED`.
4. **Role Clearance & RBAC**:
   - Full claim lifecycle management restricted to `BILLING_OFFICER` and `ADMIN`.
   - Patients have read-only access to their own policies and claim summaries (`PATIENT` role).
   - Medical staff (`DOCTOR`) can view claim status for their clinical encounters.

## Consequences
- Clean separation between internal hospital invoices and external third-party payer claims.
- Strict state machine preventing claim tampering once submitted or settled.
- Direct mathematical reconciliation between claim approvals and invoice balances.
