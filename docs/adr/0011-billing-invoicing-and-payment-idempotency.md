# ADR-0011: Billing Invoicing and Payment Idempotency Architecture

## Status
Accepted

## Context
Hospital revenue cycle management consolidates clinical events (physician consultations, diagnostic laboratory orders, pharmaceutical dispensations, and inpatient bed stays) into itemized invoices (§29, §30, §31, §32, §57 Lab 5, §103 Phase 11).

Key challenges in hospital billing and payment processing include:
1. **Precision and Financial Invariants (§29, §30)**:
   - Floating-point representations (`double`, `float`) suffer from binary fractional rounding errors (e.g. $0.1 + 0.2 = 0.30000000000000004$).
   - All financial amounts must strictly use `BigDecimal` with an explicit scale of 2 decimal places and `RoundingMode.HALF_UP`. Implicit floating-point math is strictly forbidden anywhere in the system (§105).
2. **Clinical Charge Aggregation (§29)**:
   - Invoices aggregate diverse hospital services:
     - Doctor consultation fees
     - Diagnostic laboratory test panels
     - Pharmacy medication dispensations
     - Inpatient ward bed night rates
     - Procedures and miscellaneous medical services
3. **Invoice Lifecycle State Progression (§29)**:
   - States: `DRAFT` $\rightarrow$ `ISSUED` $\rightarrow$ `PARTIALLY_PAID` $\rightarrow$ `PAID`, with `CANCELLED` allowed only on non-paid invoices.
   - Calculations: $\text{Total} = \text{Subtotal} - \text{Discount} + \text{Tax}$. $\text{Balance Due} = \text{Total} - \text{Paid Amount}$.
4. **Duplicate Payment & Network Retry Idempotency (Concurrency Lab 5)**:
   - When patients or clerks submit payments, network timeouts, gateway retries, or rapid double-clicks on "Pay Now" can send identical payment requests within milliseconds.
   - The platform must support the `Idempotency-Key` header (§32, §57 Lab 5). Retried requests with the same key must return the original transaction result without duplicate debiting or invoice corruption.

## Decision
1. **Package by Feature**:
   All billing domain models, invoice line items, payments, idempotency logic, repositories, services, DTOs, controllers, and mappers reside under `com.careflow.billing`.
2. **Domain Architecture**:
   - `Invoice`: Aggregate root managing financial balance, taxes, discounts, payment status, and line items.
   - `InvoiceItem`: Line item capturing billing source (`CONSULTATION`, `LABORATORY`, `PHARMACY`, `ADMISSION`, `PROCEDURE`, `MISCELLANEOUS`), item code, quantity, unit price, and source reference ID.
   - `Payment`: Financial transaction capturing amount, payment method (`CASH`, `CREDIT_CARD`, `DEBIT_CARD`, `INSURANCE`, `BANK_TRANSFER`), transaction reference, payment status (`INITIATED`, `SUCCESS`, `FAILED`, `REFUNDED`), and unique `idempotencyKey`.
3. **Payment Idempotency & Database Concurrency (Concurrency Lab 5)**:
   - The database schema enforces `CONSTRAINT uk_payments_idempotency_key UNIQUE (idempotency_key)`.
   - When a payment request is received with an `Idempotency-Key`:
     - If a record with that key already exists, the service returns the previous payment response immediately (safe HTTP 200 OK / 201 Created idempotency).
     - If new, the invoice is locked with JPA pessimistic write locking (`LockModeType.PESSIMISTIC_WRITE` / `SELECT ... FOR UPDATE` via `invoiceRepository.findByIdForUpdate(invoiceId)`).
     - Inside the transaction, balance due is checked: `if (paymentAmount.compareTo(invoice.getBalanceDue()) > 0) throw new OverpaymentException(...)`.
     - The payment record is inserted and the invoice balance is decremented atomically.
4. **Role Clearance & RBAC**:
   - Creating invoices, modifying draft items, issuing invoices, and processing payments is restricted to `BILLING_OFFICER` and `ADMIN`.
   - Patients can view their own itemized invoices and receipts (`PATIENT` role).
   - Medical staff (`DOCTOR`, `NURSE`) can view billing status for their consultations and admissions.

## Consequences
- Zero floating-point arithmetic errors across all financial calculations.
- Complete idempotency protecting patients from accidental double-billing during network retries.
- Transparent audit trail linking invoice line items directly to source clinical events.
