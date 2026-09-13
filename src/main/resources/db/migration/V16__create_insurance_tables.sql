-- CareFlow Enterprise Hospital Operations Platform
-- Flyway Migration V16: Insurance Providers, Patient Policies & Claims Schema (§33, §103 Phase 12)
-- Creates insurance_providers, insurance_policies, insurance_claims, claim_items tables and demo payer seeds

-- 1. Insurance Providers (Third-Party Payers)
CREATE TABLE IF NOT EXISTS insurance_providers (
    id VARCHAR(64) NOT NULL,
    provider_code VARCHAR(50) NOT NULL,
    name VARCHAR(150) NOT NULL,
    payer_id VARCHAR(64) NOT NULL,
    contact_email VARCHAR(100) NULL,
    contact_phone VARCHAR(30) NULL,
    address VARCHAR(255) NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,

    -- Audit & Optimistic Locking (§11, §92)
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NULL,
    updated_by VARCHAR(100) NULL,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT pk_insurance_providers PRIMARY KEY (id),
    CONSTRAINT uk_insurance_providers_code UNIQUE (provider_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_insurance_providers_payer_id ON insurance_providers (payer_id);
CREATE INDEX idx_insurance_providers_active ON insurance_providers (active);

-- 2. Patient Insurance Policies
CREATE TABLE IF NOT EXISTS insurance_policies (
    id VARCHAR(64) NOT NULL,
    policy_number VARCHAR(64) NOT NULL,
    group_number VARCHAR(64) NULL,
    patient_id VARCHAR(64) NOT NULL,
    provider_id VARCHAR(64) NOT NULL,
    policy_holder_name VARCHAR(150) NOT NULL,
    relationship VARCHAR(50) NOT NULL DEFAULT 'SELF',
    coverage_start_date DATE NOT NULL,
    coverage_end_date DATE NOT NULL,
    co_pay_amount DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    coverage_percentage DECIMAL(5, 2) NOT NULL DEFAULT 80.00,
    deductible DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    active BOOLEAN NOT NULL DEFAULT TRUE,

    -- Audit & Optimistic Locking
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NULL,
    updated_by VARCHAR(100) NULL,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT pk_insurance_policies PRIMARY KEY (id),
    CONSTRAINT fk_policy_patient FOREIGN KEY (patient_id) REFERENCES patients (id),
    CONSTRAINT fk_policy_provider FOREIGN KEY (provider_id) REFERENCES insurance_providers (id),
    CONSTRAINT chk_policy_copay CHECK (co_pay_amount >= 0),
    CONSTRAINT chk_policy_percentage CHECK (coverage_percentage >= 0 AND coverage_percentage <= 100),
    CONSTRAINT chk_policy_deductible CHECK (deductible >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_insurance_policies_patient ON insurance_policies (patient_id);
CREATE INDEX idx_insurance_policies_provider ON insurance_policies (provider_id);
CREATE INDEX idx_insurance_policies_number ON insurance_policies (policy_number);
CREATE INDEX idx_insurance_policies_active ON insurance_policies (active);

-- 3. Insurance Claims (Aggregate Root)
CREATE TABLE IF NOT EXISTS insurance_claims (
    id VARCHAR(64) NOT NULL,
    claim_number VARCHAR(64) NOT NULL,
    policy_id VARCHAR(64) NOT NULL,
    patient_id VARCHAR(64) NOT NULL,
    invoice_id VARCHAR(64) NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    total_claimed_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    approved_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    patient_responsibility DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    denial_reason TEXT NULL,
    adjudication_notes TEXT NULL,
    submitted_at TIMESTAMP NULL,
    adjudicated_at TIMESTAMP NULL,
    settled_at TIMESTAMP NULL,

    -- Audit & Optimistic Locking
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NULL,
    updated_by VARCHAR(100) NULL,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT pk_insurance_claims PRIMARY KEY (id),
    CONSTRAINT uk_insurance_claims_number UNIQUE (claim_number),
    CONSTRAINT fk_claim_policy FOREIGN KEY (policy_id) REFERENCES insurance_policies (id),
    CONSTRAINT fk_claim_patient FOREIGN KEY (patient_id) REFERENCES patients (id),
    CONSTRAINT fk_claim_invoice FOREIGN KEY (invoice_id) REFERENCES invoices (id) ON DELETE SET NULL,
    CONSTRAINT chk_claim_total CHECK (total_claimed_amount >= 0),
    CONSTRAINT chk_claim_approved CHECK (approved_amount >= 0),
    CONSTRAINT chk_claim_patient_resp CHECK (patient_responsibility >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_insurance_claims_policy ON insurance_claims (policy_id);
CREATE INDEX idx_insurance_claims_patient ON insurance_claims (patient_id);
CREATE INDEX idx_insurance_claims_invoice ON insurance_claims (invoice_id);
CREATE INDEX idx_insurance_claims_status ON insurance_claims (status);
CREATE INDEX idx_insurance_claims_created_at ON insurance_claims (created_at);

-- 4. Claim Line Items
CREATE TABLE IF NOT EXISTS claim_items (
    id VARCHAR(64) NOT NULL,
    claim_id VARCHAR(64) NOT NULL,
    invoice_item_id VARCHAR(64) NULL,
    service_code VARCHAR(50) NOT NULL,
    description VARCHAR(255) NOT NULL,
    claimed_amount DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    approved_amount DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    rejection_reason VARCHAR(255) NULL,

    -- Audit & Optimistic Locking
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NULL,
    updated_by VARCHAR(100) NULL,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT pk_claim_items PRIMARY KEY (id),
    CONSTRAINT fk_claim_item_claim FOREIGN KEY (claim_id) REFERENCES insurance_claims (id) ON DELETE CASCADE,
    CONSTRAINT fk_claim_item_invoice_item FOREIGN KEY (invoice_item_id) REFERENCES invoice_items (id) ON DELETE SET NULL,
    CONSTRAINT chk_claim_item_claimed CHECK (claimed_amount >= 0),
    CONSTRAINT chk_claim_item_approved CHECK (approved_amount >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_claim_items_claim ON claim_items (claim_id);
CREATE INDEX idx_claim_items_service ON claim_items (service_code);

-- 5. Seed Synthetic Demo Payers
INSERT INTO insurance_providers (id, provider_code, name, payer_id, contact_email, contact_phone, address, active, created_at, updated_at, version)
VALUES 
    ('prov-bcbs-001', 'PAYER-BCBS', 'BlueCross BlueShield Premier', 'PAYER-60054', 'claims@bcbs.demo', '+1-800-555-0191', '100 Blue Shield Ave, Chicago, IL', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    ('prov-aetna-001', 'PAYER-AETNA', 'Aetna Health Care Advantage', 'PAYER-60055', 'claims@aetna.demo', '+1-800-555-0192', '151 Farmington Ave, Hartford, CT', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    ('prov-medicare-001', 'PAYER-MEDICARE', 'Centers for Medicare & Medicaid Services', 'PAYER-00100', 'claims@medicare.demo', '+1-800-555-0193', '7500 Security Blvd, Baltimore, MD', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
ON DUPLICATE KEY UPDATE provider_code = VALUES(provider_code);
