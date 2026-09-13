-- CareFlow Enterprise Hospital Operations Platform
-- Flyway Migration V11: Consultation Encounters and Clinical Records Schema (§22, §23, §103 Phase 7)
-- Creates consultations, consultation_diagnoses, and clinical_notes tables

CREATE TABLE IF NOT EXISTS consultations (
    id VARCHAR(64) NOT NULL,
    patient_id VARCHAR(64) NOT NULL,
    doctor_id VARCHAR(64) NOT NULL,
    appointment_id VARCHAR(64) NULL,
    queue_entry_id VARCHAR(64) NULL,
    status VARCHAR(30) NOT NULL,
    started_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP NULL,
    chief_complaint TEXT NULL,
    history_of_present_illness TEXT NULL,
    physical_examination TEXT NULL,
    treatment_plan TEXT NULL,
    follow_up_date DATE NULL,
    follow_up_instructions TEXT NULL,

    -- Embedded Clinical Vitals
    systolic_bp INT NULL,
    diastolic_bp INT NULL,
    heart_rate INT NULL,
    respiratory_rate INT NULL,
    temperature_celsius DECIMAL(4, 1) NULL,
    oxygen_saturation INT NULL,
    height_cm DECIMAL(5, 1) NULL,
    weight_kg DECIMAL(5, 2) NULL,
    bmi DECIMAL(4, 1) NULL,

    -- Audit & Optimistic Locking (§11, §92)
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NULL,
    updated_by VARCHAR(100) NULL,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT pk_consultations PRIMARY KEY (id),
    CONSTRAINT fk_consultation_patient FOREIGN KEY (patient_id) REFERENCES patients (id),
    CONSTRAINT fk_consultation_doctor FOREIGN KEY (doctor_id) REFERENCES staff_members (id),
    CONSTRAINT fk_consultation_appointment FOREIGN KEY (appointment_id) REFERENCES appointments (id),
    CONSTRAINT fk_consultation_queue FOREIGN KEY (queue_entry_id) REFERENCES queue_entries (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_consultation_patient ON consultations (patient_id);
CREATE INDEX idx_consultation_doctor ON consultations (doctor_id);
CREATE INDEX idx_consultation_status ON consultations (status);
CREATE INDEX idx_consultation_started ON consultations (started_at);
CREATE INDEX idx_consultation_appointment ON consultations (appointment_id);
CREATE INDEX idx_consultation_queue ON consultations (queue_entry_id);

CREATE TABLE IF NOT EXISTS consultation_diagnoses (
    id VARCHAR(64) NOT NULL,
    consultation_id VARCHAR(64) NOT NULL,
    diagnosis_code VARCHAR(32) NOT NULL,
    diagnosis_name VARCHAR(255) NOT NULL,
    diagnosis_type VARCHAR(30) NOT NULL,
    severity VARCHAR(30) NULL,
    notes TEXT NULL,

    -- Audit & Optimistic Locking
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NULL,
    updated_by VARCHAR(100) NULL,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT pk_consultation_diagnoses PRIMARY KEY (id),
    CONSTRAINT fk_diagnosis_consultation FOREIGN KEY (consultation_id) REFERENCES consultations (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_diagnosis_consultation ON consultation_diagnoses (consultation_id);
CREATE INDEX idx_diagnosis_code ON consultation_diagnoses (diagnosis_code);

CREATE TABLE IF NOT EXISTS clinical_notes (
    id VARCHAR(64) NOT NULL,
    patient_id VARCHAR(64) NOT NULL,
    consultation_id VARCHAR(64) NULL,
    author_id VARCHAR(64) NOT NULL,
    note_type VARCHAR(30) NOT NULL,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,

    -- Audit & Optimistic Locking
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NULL,
    updated_by VARCHAR(100) NULL,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT pk_clinical_notes PRIMARY KEY (id),
    CONSTRAINT fk_clinical_note_patient FOREIGN KEY (patient_id) REFERENCES patients (id),
    CONSTRAINT fk_clinical_note_consultation FOREIGN KEY (consultation_id) REFERENCES consultations (id),
    CONSTRAINT fk_clinical_note_author FOREIGN KEY (author_id) REFERENCES staff_members (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_clinical_note_patient ON clinical_notes (patient_id);
CREATE INDEX idx_clinical_note_consultation ON clinical_notes (consultation_id);
CREATE INDEX idx_clinical_note_author ON clinical_notes (author_id);
CREATE INDEX idx_clinical_note_created ON clinical_notes (created_at);
