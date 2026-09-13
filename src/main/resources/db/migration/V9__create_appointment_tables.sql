-- CareFlow Enterprise Hospital Operations Platform
-- Flyway Migration V9: Appointment Management & Double-Booking Prevention Schema (§19, §20, §103 Phase 5)
-- Creates appointments table with state machine lifecycle and unique active slot constraint for race condition prevention

CREATE TABLE IF NOT EXISTS appointments (
    id VARCHAR(64) NOT NULL,
    patient_id VARCHAR(64) NOT NULL,
    doctor_id VARCHAR(64) NOT NULL,
    department_id VARCHAR(64) NOT NULL,
    appointment_date_time DATETIME NOT NULL,
    duration_minutes INT NOT NULL,
    status VARCHAR(20) NOT NULL,
    active_slot_flag INT NULL,
    reason_for_visit VARCHAR(500) NULL,
    cancellation_reason VARCHAR(500) NULL,

    -- Audit & Optimistic Locking (§11)
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NULL,
    updated_by VARCHAR(100) NULL,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT pk_appointments PRIMARY KEY (id),
    CONSTRAINT fk_appointments_patient FOREIGN KEY (patient_id) REFERENCES patients (id),
    CONSTRAINT fk_appointments_doctor FOREIGN KEY (doctor_id) REFERENCES staff_members (id),
    CONSTRAINT fk_appointments_department FOREIGN KEY (department_id) REFERENCES departments (id),
    CONSTRAINT chk_appointment_duration CHECK (duration_minutes > 0),

    -- Critical Double-Booking Prevention (§20, §92):
    -- active_slot_flag = 1 for active statuses (REQUESTED, CONFIRMED, CHECKED_IN, IN_PROGRESS, COMPLETED)
    -- active_slot_flag = NULL for cancelled/no-show statuses.
    -- Unique constraint ensures at most ONE active appointment per (doctor_id, appointment_date_time).
    CONSTRAINT uk_active_appointment_slot UNIQUE (doctor_id, appointment_date_time, active_slot_flag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Query optimization indexes (§96)
CREATE INDEX idx_appointments_patient ON appointments (patient_id, appointment_date_time);
CREATE INDEX idx_appointments_doctor ON appointments (doctor_id, appointment_date_time);
CREATE INDEX idx_appointments_status ON appointments (status);
CREATE INDEX idx_appointments_department ON appointments (department_id);
