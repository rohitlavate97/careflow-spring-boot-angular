-- CareFlow Enterprise Hospital Operations Platform
-- Flyway Migration V13: Laboratory Management Schema (§27, §103 Phase 9)
-- Creates lab_tests, lab_orders, lab_order_items, lab_samples, lab_results, indexes, and demo catalog

-- 1. Ensure Demo Lab Technician Staff Member Exists
INSERT INTO staff_members (id, staff_code, user_id, department_id, first_name, last_name, email, phone, staff_type, status, date_of_joining, created_at, updated_at, version)
VALUES ('staff-lab-001', 'LAB-TECH-001', 'user-lab-001', 'dept-path-001', 'Marie', 'Curie', 'lab@careflow.local', '+1-555-0106', 'LAB_TECHNICIAN', 'ACTIVE', '2023-04-01', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
ON DUPLICATE KEY UPDATE staff_code = VALUES(staff_code);

-- 2. Master Lab Test Catalog
CREATE TABLE IF NOT EXISTS lab_tests (
    id VARCHAR(64) NOT NULL,
    code VARCHAR(32) NOT NULL,
    name VARCHAR(150) NOT NULL,
    category VARCHAR(50) NOT NULL,
    specimen_type VARCHAR(50) NOT NULL,
    reference_range VARCHAR(100) NULL,
    unit VARCHAR(50) NULL,
    turnaround_hours INT NOT NULL DEFAULT 24,
    price DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    active BOOLEAN NOT NULL DEFAULT TRUE,

    -- Audit & Optimistic Locking (§11)
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NULL,
    updated_by VARCHAR(100) NULL,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT pk_lab_tests PRIMARY KEY (id),
    CONSTRAINT uk_lab_tests_code UNIQUE (code),
    CONSTRAINT chk_lab_tests_turnaround CHECK (turnaround_hours > 0),
    CONSTRAINT chk_lab_tests_price CHECK (price >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_lab_tests_category ON lab_tests (category);
CREATE INDEX idx_lab_tests_active ON lab_tests (active);

-- 3. Lab Orders (Diagnostic Requisitions)
CREATE TABLE IF NOT EXISTS lab_orders (
    id VARCHAR(64) NOT NULL,
    order_number VARCHAR(64) NOT NULL,
    patient_id VARCHAR(64) NOT NULL,
    ordering_doctor_id VARCHAR(64) NOT NULL,
    encounter_id VARCHAR(64) NULL,
    priority VARCHAR(20) NOT NULL DEFAULT 'ROUTINE',
    status VARCHAR(30) NOT NULL DEFAULT 'ORDERED',
    review_status VARCHAR(30) NOT NULL DEFAULT 'PENDING_REVIEW',
    clinical_notes TEXT NULL,
    cancellation_reason TEXT NULL,
    ordered_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP NULL,
    reviewed_by_id VARCHAR(64) NULL,
    reviewed_at TIMESTAMP NULL,
    review_notes TEXT NULL,

    -- Audit & Optimistic Locking
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NULL,
    updated_by VARCHAR(100) NULL,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT pk_lab_orders PRIMARY KEY (id),
    CONSTRAINT uk_lab_orders_number UNIQUE (order_number),
    CONSTRAINT fk_lab_order_patient FOREIGN KEY (patient_id) REFERENCES patients (id),
    CONSTRAINT fk_lab_order_doctor FOREIGN KEY (ordering_doctor_id) REFERENCES staff_members (id),
    CONSTRAINT fk_lab_order_encounter FOREIGN KEY (encounter_id) REFERENCES consultations (id) ON DELETE SET NULL,
    CONSTRAINT fk_lab_order_reviewer FOREIGN KEY (reviewed_by_id) REFERENCES staff_members (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_lab_orders_patient ON lab_orders (patient_id);
CREATE INDEX idx_lab_orders_doctor ON lab_orders (ordering_doctor_id);
CREATE INDEX idx_lab_orders_encounter ON lab_orders (encounter_id);
CREATE INDEX idx_lab_orders_status ON lab_orders (status);
CREATE INDEX idx_lab_orders_review_status ON lab_orders (review_status);
CREATE INDEX idx_lab_orders_ordered_at ON lab_orders (ordered_at);

-- 4. Lab Order Items (Specific test items inside an order)
CREATE TABLE IF NOT EXISTS lab_order_items (
    id VARCHAR(64) NOT NULL,
    lab_order_id VARCHAR(64) NOT NULL,
    lab_test_id VARCHAR(64) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ORDERED',
    notes VARCHAR(500) NULL,

    -- Audit & Optimistic Locking
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NULL,
    updated_by VARCHAR(100) NULL,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT pk_lab_order_items PRIMARY KEY (id),
    CONSTRAINT fk_lab_item_order FOREIGN KEY (lab_order_id) REFERENCES lab_orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_lab_item_test FOREIGN KEY (lab_test_id) REFERENCES lab_tests (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_lab_order_items_order ON lab_order_items (lab_order_id);
CREATE INDEX idx_lab_order_items_test ON lab_order_items (lab_test_id);
CREATE INDEX idx_lab_order_items_status ON lab_order_items (status);

-- 5. Lab Samples / Specimens (§27)
CREATE TABLE IF NOT EXISTS lab_samples (
    id VARCHAR(64) NOT NULL,
    sample_barcode VARCHAR(64) NOT NULL,
    lab_order_id VARCHAR(64) NOT NULL,
    specimen_type VARCHAR(50) NOT NULL,
    collected_by_id VARCHAR(64) NOT NULL,
    collected_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    condition_notes VARCHAR(500) NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'COLLECTED',
    rejection_reason VARCHAR(500) NULL,

    -- Audit & Optimistic Locking
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NULL,
    updated_by VARCHAR(100) NULL,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT pk_lab_samples PRIMARY KEY (id),
    CONSTRAINT uk_lab_samples_barcode UNIQUE (sample_barcode),
    CONSTRAINT fk_lab_sample_order FOREIGN KEY (lab_order_id) REFERENCES lab_orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_lab_sample_collector FOREIGN KEY (collected_by_id) REFERENCES staff_members (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_lab_samples_order ON lab_samples (lab_order_id);
CREATE INDEX idx_lab_samples_status ON lab_samples (status);

-- 6. Lab Results (§27)
CREATE TABLE IF NOT EXISTS lab_results (
    id VARCHAR(64) NOT NULL,
    order_item_id VARCHAR(64) NOT NULL,
    sample_id VARCHAR(64) NULL,
    test_parameter VARCHAR(100) NOT NULL,
    result_value VARCHAR(255) NOT NULL,
    numeric_value DECIMAL(12, 4) NULL,
    unit VARCHAR(50) NULL,
    reference_range VARCHAR(100) NULL,
    abnormality_flag VARCHAR(30) NOT NULL DEFAULT 'NORMAL',
    performed_by_id VARCHAR(64) NOT NULL,
    performed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    technician_notes TEXT NULL,

    -- Audit & Optimistic Locking
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NULL,
    updated_by VARCHAR(100) NULL,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT pk_lab_results PRIMARY KEY (id),
    CONSTRAINT fk_lab_result_item FOREIGN KEY (order_item_id) REFERENCES lab_order_items (id) ON DELETE CASCADE,
    CONSTRAINT fk_lab_result_sample FOREIGN KEY (sample_id) REFERENCES lab_samples (id) ON DELETE SET NULL,
    CONSTRAINT fk_lab_result_technician FOREIGN KEY (performed_by_id) REFERENCES staff_members (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_lab_results_item ON lab_results (order_item_id);
CREATE INDEX idx_lab_results_sample ON lab_results (sample_id);
CREATE INDEX idx_lab_results_flag ON lab_results (abnormality_flag);

-- 7. Seed Diagnostic Lab Test Catalog (§27, §101)
INSERT INTO lab_tests (id, code, name, category, specimen_type, reference_range, unit, turnaround_hours, price, active, created_at, updated_at, version)
VALUES
    ('test-cbc-001', 'CBC', 'Complete Blood Count', 'HEMATOLOGY', 'BLOOD', '4.5-11.0 WBC, 12.0-16.0 Hgb', NULL, 4, 18.00, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    ('test-bmp-001', 'BMP', 'Basic Metabolic Panel', 'BIOCHEMISTRY', 'BLOOD', '70-99 Glucose, 136-145 Na, 3.5-5.0 K', 'mg/dL', 6, 25.00, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    ('test-lipid-001', 'LIPID', 'Lipid Panel', 'BIOCHEMISTRY', 'BLOOD', '<200 Total Chol, <100 LDL, >40 HDL', 'mg/dL', 8, 30.00, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    ('test-lft-001', 'LFT', 'Liver Function Tests', 'BIOCHEMISTRY', 'BLOOD', '7-56 ALT, 10-40 AST, 0.2-1.2 Bili', 'U/L', 8, 28.00, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    ('test-tsh-001', 'TSH', 'Thyroid Stimulating Hormone', 'IMMUNOLOGY', 'SERUM', '0.4-4.0', 'mIU/L', 12, 35.00, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    ('test-hba1c-001', 'HBA1C', 'Hemoglobin A1c', 'BIOCHEMISTRY', 'BLOOD', '<5.7 Normal, 5.7-6.4 Pre-diabetes', '%', 6, 26.00, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    ('test-urin-001', 'URIN', 'Urinalysis Routine & Microscopy', 'URINALYSIS', 'URINE', 'Normal pH 5.0-8.0, Negative protein/glucose', NULL, 2, 15.00, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
ON DUPLICATE KEY UPDATE name = VALUES(name);
