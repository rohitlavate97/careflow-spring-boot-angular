-- CareFlow Enterprise Hospital Operations Platform
-- Flyway Migration V10: Patient Queue Management Schema (§21, §103 Phase 6)
-- Creates queue_entries table with priority triage, daily token numbering, and queue lifecycle

CREATE TABLE IF NOT EXISTS queue_entries (
    id VARCHAR(64) NOT NULL,
    department_id VARCHAR(64) NOT NULL,
    doctor_id VARCHAR(64) NULL,
    patient_id VARCHAR(64) NOT NULL,
    appointment_id VARCHAR(64) NULL,
    queue_date DATE NOT NULL,
    token_number INT NOT NULL,
    token_display VARCHAR(32) NOT NULL,
    priority VARCHAR(20) NOT NULL,
    status VARCHAR(30) NOT NULL,
    entry_time TIMESTAMP NOT NULL,
    called_time TIMESTAMP NULL,
    consultation_start_time TIMESTAMP NULL,
    consultation_end_time TIMESTAMP NULL,
    notes VARCHAR(500) NULL,

    -- Audit & Optimistic Locking (§11, §92)
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NULL,
    updated_by VARCHAR(100) NULL,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT pk_queue_entries PRIMARY KEY (id),
    CONSTRAINT fk_queue_department FOREIGN KEY (department_id) REFERENCES departments (id),
    CONSTRAINT fk_queue_doctor FOREIGN KEY (doctor_id) REFERENCES staff_members (id),
    CONSTRAINT fk_queue_patient FOREIGN KEY (patient_id) REFERENCES patients (id),
    CONSTRAINT fk_queue_appointment FOREIGN KEY (appointment_id) REFERENCES appointments (id),
    CONSTRAINT uk_queue_dept_date_token UNIQUE (department_id, queue_date, token_number),
    CONSTRAINT chk_queue_token_number CHECK (token_number > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Query optimization indexes (§96)
CREATE INDEX idx_queue_dept_date_status ON queue_entries (department_id, queue_date, status);
CREATE INDEX idx_queue_doc_date_status ON queue_entries (doctor_id, queue_date, status);
CREATE INDEX idx_queue_patient ON queue_entries (patient_id);
CREATE INDEX idx_queue_appointment ON queue_entries (appointment_id);
