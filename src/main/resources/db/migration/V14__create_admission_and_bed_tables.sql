-- CareFlow Enterprise Hospital Operations Platform
-- Flyway Migration V14: Inpatient Admission & Bed Management Schema (§28, §57 Lab 4, §103 Phase 10)
-- Creates wards, rooms, beds, admissions, bed_transfers tables, constraints, indexes, and demo bed inventory

-- 1. Hospital Wards
CREATE TABLE IF NOT EXISTS wards (
    id VARCHAR(64) NOT NULL,
    ward_code VARCHAR(32) NOT NULL,
    name VARCHAR(100) NOT NULL,
    department_id VARCHAR(64) NOT NULL,
    ward_type VARCHAR(50) NOT NULL,
    floor VARCHAR(50) NULL,
    total_beds INT NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,

    -- Audit & Optimistic Locking (§11)
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NULL,
    updated_by VARCHAR(100) NULL,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT pk_wards PRIMARY KEY (id),
    CONSTRAINT uk_wards_code UNIQUE (ward_code),
    CONSTRAINT fk_ward_department FOREIGN KEY (department_id) REFERENCES departments (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_wards_department ON wards (department_id);
CREATE INDEX idx_wards_type ON wards (ward_type);
CREATE INDEX idx_wards_active ON wards (active);

-- 2. Ward Rooms
CREATE TABLE IF NOT EXISTS rooms (
    id VARCHAR(64) NOT NULL,
    room_number VARCHAR(32) NOT NULL,
    ward_id VARCHAR(64) NOT NULL,
    room_type VARCHAR(50) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,

    -- Audit & Optimistic Locking
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NULL,
    updated_by VARCHAR(100) NULL,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT pk_rooms PRIMARY KEY (id),
    CONSTRAINT uk_rooms_ward_number UNIQUE (ward_id, room_number),
    CONSTRAINT fk_room_ward FOREIGN KEY (ward_id) REFERENCES wards (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_rooms_ward ON rooms (ward_id);
CREATE INDEX idx_rooms_type ON rooms (room_type);

-- 3. Inpatient Beds (§28)
CREATE TABLE IF NOT EXISTS beds (
    id VARCHAR(64) NOT NULL,
    bed_number VARCHAR(32) NOT NULL,
    room_id VARCHAR(64) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'AVAILABLE',
    daily_rate DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    active BOOLEAN NOT NULL DEFAULT TRUE,

    -- Audit & Optimistic Locking (§11, §92)
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NULL,
    updated_by VARCHAR(100) NULL,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT pk_beds PRIMARY KEY (id),
    CONSTRAINT uk_beds_room_number UNIQUE (room_id, bed_number),
    CONSTRAINT fk_bed_room FOREIGN KEY (room_id) REFERENCES rooms (id) ON DELETE CASCADE,
    CONSTRAINT chk_bed_rate CHECK (daily_rate >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_beds_room ON beds (room_id);
CREATE INDEX idx_beds_status ON beds (status);
CREATE INDEX idx_beds_active ON beds (active);

-- 4. Inpatient Admissions (§28)
CREATE TABLE IF NOT EXISTS admissions (
    id VARCHAR(64) NOT NULL,
    admission_number VARCHAR(64) NOT NULL,
    patient_id VARCHAR(64) NOT NULL,
    admitting_doctor_id VARCHAR(64) NOT NULL,
    current_bed_id VARCHAR(64) NULL,
    encounter_id VARCHAR(64) NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ADMITTED',
    admission_reason TEXT NOT NULL,
    admitting_diagnosis TEXT NULL,
    admitted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    discharged_at TIMESTAMP NULL,
    discharge_summary TEXT NULL,

    -- Audit & Optimistic Locking
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NULL,
    updated_by VARCHAR(100) NULL,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT pk_admissions PRIMARY KEY (id),
    CONSTRAINT uk_admissions_number UNIQUE (admission_number),
    CONSTRAINT fk_admission_patient FOREIGN KEY (patient_id) REFERENCES patients (id),
    CONSTRAINT fk_admission_doctor FOREIGN KEY (admitting_doctor_id) REFERENCES staff_members (id),
    CONSTRAINT fk_admission_bed FOREIGN KEY (current_bed_id) REFERENCES beds (id) ON DELETE SET NULL,
    CONSTRAINT fk_admission_encounter FOREIGN KEY (encounter_id) REFERENCES consultations (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_admissions_patient ON admissions (patient_id);
CREATE INDEX idx_admissions_doctor ON admissions (admitting_doctor_id);
CREATE INDEX idx_admissions_bed ON admissions (current_bed_id);
CREATE INDEX idx_admissions_status ON admissions (status);
CREATE INDEX idx_admissions_admitted_at ON admissions (admitted_at);

-- 5. Bed Transfer Audit History (§28)
CREATE TABLE IF NOT EXISTS bed_transfers (
    id VARCHAR(64) NOT NULL,
    admission_id VARCHAR(64) NOT NULL,
    from_bed_id VARCHAR(64) NULL,
    to_bed_id VARCHAR(64) NOT NULL,
    transferred_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    transfer_reason VARCHAR(500) NULL,
    transferred_by_id VARCHAR(64) NOT NULL,

    -- Audit & Optimistic Locking
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NULL,
    updated_by VARCHAR(100) NULL,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT pk_bed_transfers PRIMARY KEY (id),
    CONSTRAINT fk_transfer_admission FOREIGN KEY (admission_id) REFERENCES admissions (id) ON DELETE CASCADE,
    CONSTRAINT fk_transfer_from_bed FOREIGN KEY (from_bed_id) REFERENCES beds (id),
    CONSTRAINT fk_transfer_to_bed FOREIGN KEY (to_bed_id) REFERENCES beds (id),
    CONSTRAINT fk_transfer_staff FOREIGN KEY (transferred_by_id) REFERENCES staff_members (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_bed_transfers_admission ON bed_transfers (admission_id);
CREATE INDEX idx_bed_transfers_to_bed ON bed_transfers (to_bed_id);
CREATE INDEX idx_bed_transfers_transferred_at ON bed_transfers (transferred_at);

-- 6. Seed Demo Wards, Rooms, and Beds (§101)
INSERT INTO wards (id, ward_code, name, department_id, ward_type, floor, total_beds, active, created_at, updated_at, version)
VALUES
    ('ward-icu-001', 'WARD-ICU-01', 'Intensive Care Unit (ICU)', 'dept-emer-001', 'ICU', 'Floor 2', 4, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    ('ward-gen-001', 'WARD-GEN-01', 'General Medical Ward', 'dept-genm-001', 'GENERAL', 'Floor 3', 6, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO rooms (id, room_number, ward_id, room_type, active, created_at, updated_at, version)
VALUES
    ('room-icu-101', 'ICU-101', 'ward-icu-001', 'PRIVATE', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    ('room-icu-102', 'ICU-102', 'ward-icu-001', 'ISOLATION', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    ('room-gen-301', 'GEN-301', 'ward-gen-001', 'SEMI_PRIVATE', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    ('room-gen-302', 'GEN-302', 'ward-gen-001', 'GENERAL_WARD', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
ON DUPLICATE KEY UPDATE room_type = VALUES(room_type);

INSERT INTO beds (id, bed_number, room_id, status, daily_rate, active, created_at, updated_at, version)
VALUES
    ('bed-icu-101-a', 'BED-ICU-101-A', 'room-icu-101', 'AVAILABLE', 500.00, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    ('bed-icu-101-b', 'BED-ICU-101-B', 'room-icu-101', 'AVAILABLE', 500.00, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    ('bed-icu-102-a', 'BED-ICU-102-A', 'room-icu-102', 'AVAILABLE', 650.00, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    ('bed-icu-102-b', 'BED-ICU-102-B', 'room-icu-102', 'AVAILABLE', 650.00, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    ('bed-gen-301-a', 'BED-GEN-301-A', 'room-gen-301', 'AVAILABLE', 150.00, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    ('bed-gen-301-b', 'BED-GEN-301-B', 'room-gen-301', 'AVAILABLE', 150.00, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    ('bed-gen-302-a', 'BED-GEN-302-A', 'room-gen-302', 'AVAILABLE', 100.00, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    ('bed-gen-302-b', 'BED-GEN-302-B', 'room-gen-302', 'AVAILABLE', 100.00, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    ('bed-gen-302-c', 'BED-GEN-302-C', 'room-gen-302', 'AVAILABLE', 100.00, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    ('bed-gen-302-d', 'BED-GEN-302-D', 'room-gen-302', 'AVAILABLE', 100.00, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
ON DUPLICATE KEY UPDATE daily_rate = VALUES(daily_rate);
