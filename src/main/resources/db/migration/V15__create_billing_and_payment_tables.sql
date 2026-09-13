-- CareFlow Enterprise Hospital Operations Platform
-- Flyway Migration V15: Billing, Invoicing & Payment Idempotency Schema (§29, §30, §31, §32, §57 Lab 5, §103 Phase 11)
-- Creates invoices, invoice_items, payments tables, constraints, indexes, and billing staff seeds

-- 1. Ensure Demo Billing Officer Staff Member Exists
INSERT INTO staff_members (id, staff_code, user_id, department_id, first_name, last_name, email, phone, staff_type, status, date_of_joining, created_at, updated_at, version)
VALUES ('staff-bill-001', 'BILL-OFF-001', 'user-billing-001', 'dept-genm-001', 'Ada', 'Lovelace', 'billing@careflow.local', '+1-555-0107', 'BILLING_OFFICER', 'ACTIVE', '2023-05-01', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
ON DUPLICATE KEY UPDATE staff_code = VALUES(staff_code);

-- 2. Invoices Aggregate (§29, §30)
CREATE TABLE IF NOT EXISTS invoices (
    id VARCHAR(64) NOT NULL,
    invoice_number VARCHAR(64) NOT NULL,
    patient_id VARCHAR(64) NOT NULL,
    encounter_id VARCHAR(64) NULL,
    admission_id VARCHAR(64) NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    currency VARCHAR(10) NOT NULL DEFAULT 'USD',
    subtotal DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    discount_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    tax_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    total_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    paid_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    balance_due DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    notes TEXT NULL,
    due_date DATE NULL,
    issued_at TIMESTAMP NULL,
    paid_at TIMESTAMP NULL,

    -- Audit & Optimistic Locking (§11, §92)
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NULL,
    updated_by VARCHAR(100) NULL,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT pk_invoices PRIMARY KEY (id),
    CONSTRAINT uk_invoices_number UNIQUE (invoice_number),
    CONSTRAINT fk_invoice_patient FOREIGN KEY (patient_id) REFERENCES patients (id),
    CONSTRAINT fk_invoice_encounter FOREIGN KEY (encounter_id) REFERENCES consultations (id) ON DELETE SET NULL,
    CONSTRAINT fk_invoice_admission FOREIGN KEY (admission_id) REFERENCES admissions (id) ON DELETE SET NULL,
    CONSTRAINT chk_invoice_subtotal CHECK (subtotal >= 0),
    CONSTRAINT chk_invoice_discount CHECK (discount_amount >= 0),
    CONSTRAINT chk_invoice_tax CHECK (tax_amount >= 0),
    CONSTRAINT chk_invoice_total CHECK (total_amount >= 0),
    CONSTRAINT chk_invoice_paid CHECK (paid_amount >= 0),
    CONSTRAINT chk_invoice_balance CHECK (balance_due >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_invoices_patient ON invoices (patient_id);
CREATE INDEX idx_invoices_encounter ON invoices (encounter_id);
CREATE INDEX idx_invoices_admission ON invoices (admission_id);
CREATE INDEX idx_invoices_status ON invoices (status);
CREATE INDEX idx_invoices_created_at ON invoices (created_at);

-- 3. Invoice Items (Clinical Service Line Items) (§29)
CREATE TABLE IF NOT EXISTS invoice_items (
    id VARCHAR(64) NOT NULL,
    invoice_id VARCHAR(64) NOT NULL,
    billing_source VARCHAR(50) NOT NULL,
    item_code VARCHAR(50) NOT NULL,
    description VARCHAR(255) NOT NULL,
    unit_price DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    quantity INT NOT NULL DEFAULT 1,
    total_price DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    source_reference_id VARCHAR(64) NULL,

    -- Audit & Optimistic Locking
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NULL,
    updated_by VARCHAR(100) NULL,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT pk_invoice_items PRIMARY KEY (id),
    CONSTRAINT fk_item_invoice FOREIGN KEY (invoice_id) REFERENCES invoices (id) ON DELETE CASCADE,
    CONSTRAINT chk_item_unit_price CHECK (unit_price >= 0),
    CONSTRAINT chk_item_quantity CHECK (quantity > 0),
    CONSTRAINT chk_item_total_price CHECK (total_price >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_invoice_items_invoice ON invoice_items (invoice_id);
CREATE INDEX idx_invoice_items_source ON invoice_items (billing_source);
CREATE INDEX idx_invoice_items_code ON invoice_items (item_code);

-- 4. Payments with Unique Idempotency Key (§31, §32, §57 Lab 5)
CREATE TABLE IF NOT EXISTS payments (
    id VARCHAR(64) NOT NULL,
    payment_number VARCHAR(64) NOT NULL,
    invoice_id VARCHAR(64) NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    payment_method VARCHAR(50) NOT NULL,
    amount DECIMAL(12, 2) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'INITIATED',
    transaction_reference VARCHAR(100) NULL,
    notes TEXT NULL,
    processed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Audit & Optimistic Locking
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NULL,
    updated_by VARCHAR(100) NULL,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT pk_payments PRIMARY KEY (id),
    CONSTRAINT uk_payments_number UNIQUE (payment_number),
    CONSTRAINT uk_payments_idempotency_key UNIQUE (idempotency_key),
    CONSTRAINT fk_payment_invoice FOREIGN KEY (invoice_id) REFERENCES invoices (id) ON DELETE CASCADE,
    CONSTRAINT chk_payment_amount CHECK (amount > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_payments_invoice ON payments (invoice_id);
CREATE INDEX idx_payments_idempotency ON payments (idempotency_key);
CREATE INDEX idx_payments_status ON payments (status);
CREATE INDEX idx_payments_processed_at ON payments (processed_at);
