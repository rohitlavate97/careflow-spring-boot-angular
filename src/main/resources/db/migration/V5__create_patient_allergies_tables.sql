-- CareFlow Enterprise Hospital Operations Platform
-- Flyway Migration V5: Patient Allergies Schema (§16, §96)
-- Creates patient_allergies table with categorical classifications, clinical severity, and query indexes

CREATE TABLE IF NOT EXISTS patient_allergies (
    id VARCHAR(64) NOT NULL,
    patient_id VARCHAR(64) NOT NULL,
    allergen VARCHAR(100) NOT NULL,
    category VARCHAR(30) NOT NULL,
    severity VARCHAR(30) NOT NULL,
    reaction VARCHAR(255) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    notes VARCHAR(1000) NULL,
    diagnosed_date DATE NULL,

    -- Audit & Optimistic Locking (§11)
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NULL,
    updated_by VARCHAR(100) NULL,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT pk_patient_allergies PRIMARY KEY (id),
    CONSTRAINT fk_pa_patient FOREIGN KEY (patient_id) REFERENCES patients (id) ON DELETE CASCADE,
    CONSTRAINT uk_patient_active_allergen UNIQUE (patient_id, allergen, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Query optimization indexes (§96)
CREATE INDEX idx_pa_patient_id ON patient_allergies (patient_id);
CREATE INDEX idx_pa_patient_status ON patient_allergies (patient_id, status);
CREATE INDEX idx_pa_severity ON patient_allergies (severity);
CREATE INDEX idx_pa_category ON patient_allergies (category);
