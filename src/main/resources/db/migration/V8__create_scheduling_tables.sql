-- CareFlow Enterprise Hospital Operations Platform
-- Flyway Migration V8: Doctor Schedule & Leave Management Schema (§18, §103 Phase 4)
-- Creates doctor_schedules and doctor_leaves tables, constraints, indexes, and initial demo schedules

CREATE TABLE IF NOT EXISTS doctor_schedules (
    id VARCHAR(64) NOT NULL,
    doctor_id VARCHAR(64) NOT NULL,
    day_of_week VARCHAR(20) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    break_start_time TIME NULL,
    break_end_time TIME NULL,
    slot_duration_minutes INT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,

    -- Audit & Optimistic Locking (§11)
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NULL,
    updated_by VARCHAR(100) NULL,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT pk_doctor_schedules PRIMARY KEY (id),
    CONSTRAINT fk_doctor_schedules_doctor FOREIGN KEY (doctor_id) REFERENCES staff_members (id) ON DELETE CASCADE,
    CONSTRAINT chk_schedule_times CHECK (start_time < end_time),
    CONSTRAINT chk_break_times CHECK (
        (break_start_time IS NULL AND break_end_time IS NULL) OR
        (break_start_time IS NOT NULL AND break_end_time IS NOT NULL AND break_start_time < break_end_time AND break_start_time >= start_time AND break_end_time <= end_time)
    ),
    CONSTRAINT chk_slot_duration CHECK (slot_duration_minutes > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS doctor_leaves (
    id VARCHAR(64) NOT NULL,
    doctor_id VARCHAR(64) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    reason VARCHAR(255) NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',

    -- Audit & Optimistic Locking (§11)
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NULL,
    updated_by VARCHAR(100) NULL,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT pk_doctor_leaves PRIMARY KEY (id),
    CONSTRAINT fk_doctor_leaves_doctor FOREIGN KEY (doctor_id) REFERENCES staff_members (id) ON DELETE CASCADE,
    CONSTRAINT chk_leave_dates CHECK (start_date <= end_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Query optimization indexes (§96)
CREATE INDEX idx_doctor_schedules_doc_day ON doctor_schedules (doctor_id, day_of_week, is_active);
CREATE INDEX idx_doctor_leaves_doc_dates ON doctor_leaves (doctor_id, start_date, end_date, status);

-- Seed Initial Standard Doctor Schedule for Demo Doctor (§18, §101)
-- Dr. Alexander Fleming (staff-doc-001): Monday 09:00-13:00 (break 11:00-11:30, slot 30m), Wednesday 09:00-17:00 (break 12:00-13:00, slot 30m)
INSERT INTO doctor_schedules (id, doctor_id, day_of_week, start_time, end_time, break_start_time, break_end_time, slot_duration_minutes, is_active, version)
VALUES
('sched-doc-001', 'staff-doc-001', 'MONDAY', '09:00:00', '13:00:00', '11:00:00', '11:30:00', 30, TRUE, 0),
('sched-doc-002', 'staff-doc-001', 'WEDNESDAY', '09:00:00', '17:00:00', '12:00:00', '13:00:00', 30, TRUE, 0)
ON DUPLICATE KEY UPDATE day_of_week = VALUES(day_of_week);
