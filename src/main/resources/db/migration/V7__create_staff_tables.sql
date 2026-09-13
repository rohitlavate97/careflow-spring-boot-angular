-- CareFlow Enterprise Hospital Operations Platform
-- Flyway Migration V7: Staff & Doctor Profile Management Schema (§17, §103 Phase 3)
-- Creates staff_members and doctor_profiles tables, constraints, indexes, and demo staff seeds

CREATE TABLE IF NOT EXISTS staff_members (
    id VARCHAR(64) NOT NULL,
    staff_code VARCHAR(32) NOT NULL,
    user_id VARCHAR(64) NULL,
    department_id VARCHAR(64) NOT NULL,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    staff_type VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    date_of_joining DATE NOT NULL,

    -- Audit & Optimistic Locking (§11)
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NULL,
    updated_by VARCHAR(100) NULL,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT pk_staff_members PRIMARY KEY (id),
    CONSTRAINT uk_staff_members_staff_code UNIQUE (staff_code),
    CONSTRAINT uk_staff_members_user_id UNIQUE (user_id),
    CONSTRAINT uk_staff_members_email UNIQUE (email),
    CONSTRAINT fk_staff_members_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT fk_staff_members_department FOREIGN KEY (department_id) REFERENCES departments (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS doctor_profiles (
    id VARCHAR(64) NOT NULL,
    staff_id VARCHAR(64) NOT NULL,
    specialization VARCHAR(100) NOT NULL,
    qualifications VARCHAR(255) NOT NULL,
    medical_license_number VARCHAR(100) NOT NULL,
    consultation_fee DECIMAL(10, 2) NOT NULL,
    consultation_room VARCHAR(50) NULL,
    bio VARCHAR(1000) NULL,

    -- Audit & Optimistic Locking (§11)
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NULL,
    updated_by VARCHAR(100) NULL,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT pk_doctor_profiles PRIMARY KEY (id),
    CONSTRAINT uk_doctor_profiles_staff_id UNIQUE (staff_id),
    CONSTRAINT uk_doctor_profiles_license UNIQUE (medical_license_number),
    CONSTRAINT fk_doctor_profiles_staff FOREIGN KEY (staff_id) REFERENCES staff_members (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Query optimization indexes (§96)
CREATE INDEX idx_staff_members_department ON staff_members (department_id);
CREATE INDEX idx_staff_members_status ON staff_members (status);
CREATE INDEX idx_staff_members_staff_type ON staff_members (staff_type);
CREATE INDEX idx_staff_members_name ON staff_members (last_name, first_name);
CREATE INDEX idx_doctor_profiles_specialization ON doctor_profiles (specialization);

-- Seed Demo Staff & Doctor Profiles (§63, §101)
INSERT INTO staff_members (id, staff_code, user_id, department_id, first_name, last_name, email, phone, staff_type, status, date_of_joining, version)
VALUES
('staff-doc-001', 'DOC-CARD-001', 'user-doctor-001', 'dept-card-001', 'Alexander', 'Fleming', 'doctor@careflow.local', '+1-555-0102', 'DOCTOR', 'ACTIVE', '2023-01-15', 0),
('staff-nur-001', 'NUR-EMER-001', 'user-nurse-001', 'dept-emer-001', 'Florence', 'Nightingale', 'nurse@careflow.local', '+1-555-0103', 'NURSE', 'ACTIVE', '2023-02-01', 0),
('staff-rec-001', 'REC-GENM-001', 'user-receptionist-001', 'dept-genm-001', 'Clara', 'Barton', 'receptionist@careflow.local', '+1-555-0104', 'RECEPTIONIST', 'ACTIVE', '2023-03-10', 0)
ON DUPLICATE KEY UPDATE staff_code = VALUES(staff_code);

INSERT INTO doctor_profiles (id, staff_id, specialization, qualifications, medical_license_number, consultation_fee, consultation_room, bio, version)
VALUES
('doc-prof-001', 'staff-doc-001', 'Cardiology', 'MBBS, MD (Cardiology), FACC', 'MED-LIC-00123', 150.00, 'Room 204, Building A', 'Senior consultant cardiologist specializing in cardiovascular interventions and preventive care.', 0)
ON DUPLICATE KEY UPDATE medical_license_number = VALUES(medical_license_number);
