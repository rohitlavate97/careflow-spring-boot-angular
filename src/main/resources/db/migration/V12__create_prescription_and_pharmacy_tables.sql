-- CareFlow Enterprise Hospital Operations Platform
-- Flyway Migration V12: Prescription Issuance & Pharmacy Inventory Management Schema (§24, §25, §26, §103 Phase 8)
-- Creates medications, pharmacy_inventory_batches, prescriptions, prescription_items, and dispense_records tables

-- 1. Medication Master Catalog (§24)
CREATE TABLE IF NOT EXISTS medications (
    id VARCHAR(64) NOT NULL,
    code VARCHAR(32) NOT NULL,
    name VARCHAR(200) NOT NULL,
    generic_name VARCHAR(200) NOT NULL,
    form VARCHAR(30) NOT NULL,
    strength VARCHAR(50) NOT NULL,
    unit_price DECIMAL(10, 2) NOT NULL,
    reorder_threshold INT NOT NULL DEFAULT 10,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

    -- Audit & Optimistic Locking (§11, §92)
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NULL,
    updated_by VARCHAR(100) NULL,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT pk_medications PRIMARY KEY (id),
    CONSTRAINT uk_medication_code UNIQUE (code),
    CONSTRAINT chk_medication_price CHECK (unit_price >= 0.00),
    CONSTRAINT chk_medication_reorder CHECK (reorder_threshold >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_medication_code ON medications (code);
CREATE INDEX idx_medication_name ON medications (name);
CREATE INDEX idx_medication_status ON medications (status);

-- 2. Pharmacy Inventory Batches (§25, §26)
CREATE TABLE IF NOT EXISTS pharmacy_inventory_batches (
    id VARCHAR(64) NOT NULL,
    medication_id VARCHAR(64) NOT NULL,
    batch_number VARCHAR(64) NOT NULL,
    expiry_date DATE NOT NULL,
    quantity_available INT NOT NULL,
    reorder_threshold INT NOT NULL DEFAULT 10,

    -- Audit & Optimistic Locking
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NULL,
    updated_by VARCHAR(100) NULL,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT pk_pharmacy_inventory_batches PRIMARY KEY (id),
    CONSTRAINT fk_batch_medication FOREIGN KEY (medication_id) REFERENCES medications (id),
    CONSTRAINT uk_batch_medication_number UNIQUE (medication_id, batch_number),
    CONSTRAINT chk_pharmacy_inventory_non_negative CHECK (quantity_available >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_inventory_med_expiry ON pharmacy_inventory_batches (medication_id, expiry_date);
CREATE INDEX idx_inventory_batch_num ON pharmacy_inventory_batches (batch_number);

-- 3. Prescriptions Aggregate Root (§24)
CREATE TABLE IF NOT EXISTS prescriptions (
    id VARCHAR(64) NOT NULL,
    patient_id VARCHAR(64) NOT NULL,
    doctor_id VARCHAR(64) NOT NULL,
    consultation_id VARCHAR(64) NULL,
    status VARCHAR(30) NOT NULL,
    notes TEXT NULL,
    prescribed_at TIMESTAMP NOT NULL,
    dispensed_at TIMESTAMP NULL,

    -- Audit & Optimistic Locking
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NULL,
    updated_by VARCHAR(100) NULL,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT pk_prescriptions PRIMARY KEY (id),
    CONSTRAINT fk_prescription_patient FOREIGN KEY (patient_id) REFERENCES patients (id),
    CONSTRAINT fk_prescription_doctor FOREIGN KEY (doctor_id) REFERENCES staff_members (id),
    CONSTRAINT fk_prescription_consultation FOREIGN KEY (consultation_id) REFERENCES consultations (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_prescription_patient ON prescriptions (patient_id);
CREATE INDEX idx_prescription_doctor ON prescriptions (doctor_id);
CREATE INDEX idx_prescription_consultation ON prescriptions (consultation_id);
CREATE INDEX idx_prescription_status ON prescriptions (status);

-- 4. Prescription Line Items (§24)
CREATE TABLE IF NOT EXISTS prescription_items (
    id VARCHAR(64) NOT NULL,
    prescription_id VARCHAR(64) NOT NULL,
    medication_id VARCHAR(64) NOT NULL,
    dosage VARCHAR(100) NOT NULL,
    frequency VARCHAR(100) NOT NULL,
    duration VARCHAR(100) NOT NULL,
    quantity_prescribed INT NOT NULL,
    quantity_dispensed INT NOT NULL DEFAULT 0,
    instructions TEXT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',

    -- Audit & Optimistic Locking
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NULL,
    updated_by VARCHAR(100) NULL,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT pk_prescription_items PRIMARY KEY (id),
    CONSTRAINT fk_item_prescription FOREIGN KEY (prescription_id) REFERENCES prescriptions (id) ON DELETE CASCADE,
    CONSTRAINT fk_item_medication FOREIGN KEY (medication_id) REFERENCES medications (id),
    CONSTRAINT chk_prescription_qty_prescribed CHECK (quantity_prescribed > 0),
    CONSTRAINT chk_prescription_qty_dispensed CHECK (quantity_dispensed >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_prescription_item_prescription ON prescription_items (prescription_id);
CREATE INDEX idx_prescription_item_medication ON prescription_items (medication_id);

-- 5. Dispense Audit Records (§25)
CREATE TABLE IF NOT EXISTS dispense_records (
    id VARCHAR(64) NOT NULL,
    prescription_id VARCHAR(64) NOT NULL,
    prescription_item_id VARCHAR(64) NOT NULL,
    inventory_batch_id VARCHAR(64) NOT NULL,
    pharmacist_id VARCHAR(64) NOT NULL,
    quantity_dispensed INT NOT NULL,
    dispensed_at TIMESTAMP NOT NULL,
    notes TEXT NULL,

    -- Audit & Optimistic Locking
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NULL,
    updated_by VARCHAR(100) NULL,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT pk_dispense_records PRIMARY KEY (id),
    CONSTRAINT fk_dispense_prescription FOREIGN KEY (prescription_id) REFERENCES prescriptions (id),
    CONSTRAINT fk_dispense_item FOREIGN KEY (prescription_item_id) REFERENCES prescription_items (id),
    CONSTRAINT fk_dispense_batch FOREIGN KEY (inventory_batch_id) REFERENCES pharmacy_inventory_batches (id),
    CONSTRAINT fk_dispense_pharmacist FOREIGN KEY (pharmacist_id) REFERENCES staff_members (id),
    CONSTRAINT chk_dispense_quantity CHECK (quantity_dispensed > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_dispense_prescription ON dispense_records (prescription_id);
CREATE INDEX idx_dispense_item ON dispense_records (prescription_item_id);
CREATE INDEX idx_dispense_batch ON dispense_records (inventory_batch_id);
CREATE INDEX idx_dispense_pharmacist ON dispense_records (pharmacist_id);

-- 6. Seed Demo Medications and Inventory Batches (§101)
INSERT INTO medications (id, code, name, generic_name, form, strength, unit_price, reorder_threshold, status, created_at, updated_at, version)
VALUES
    ('med-amox-500', 'MED-AMOX-500', 'Amoxicillin', 'Amoxicillin Trihydrate', 'CAPSULE', '500 mg', 12.50, 20, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    ('med-para-500', 'MED-PARA-500', 'Paracetamol', 'Acetaminophen', 'TABLET', '500 mg', 5.00, 50, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    ('med-metf-850', 'MED-METF-850', 'Metformin', 'Metformin Hydrochloride', 'TABLET', '850 mg', 8.25, 30, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    ('med-ator-020', 'MED-ATOR-020', 'Atorvastatin', 'Atorvastatin Calcium', 'TABLET', '20 mg', 18.00, 15, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
ON DUPLICATE KEY UPDATE updated_at = CURRENT_TIMESTAMP;

INSERT INTO pharmacy_inventory_batches (id, medication_id, batch_number, expiry_date, quantity_available, reorder_threshold, created_at, updated_at, version)
VALUES
    ('batch-amox-001', 'med-amox-500', 'AMX-2026-01', '2027-12-31', 100, 20, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    ('batch-para-001', 'med-para-500', 'PAR-2026-01', '2028-06-30', 250, 50, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    ('batch-metf-001', 'med-metf-850', 'MET-2026-01', '2027-10-15', 150, 30, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    ('batch-ator-001', 'med-ator-020', 'ATR-2026-01', '2028-01-31', 80, 15, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
ON DUPLICATE KEY UPDATE updated_at = CURRENT_TIMESTAMP;
