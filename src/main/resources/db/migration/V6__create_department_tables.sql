-- CareFlow Enterprise Hospital Operations Platform
-- Flyway Migration V6: Department Management Schema & Standard Hospital Departments (§17, §96)
-- Creates departments table with unique codes, contact coordinates, and initial clinical department seeds

CREATE TABLE IF NOT EXISTS departments (
    id VARCHAR(64) NOT NULL,
    code VARCHAR(20) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500) NULL,
    contact_phone VARCHAR(20) NULL,
    contact_email VARCHAR(100) NULL,
    location VARCHAR(100) NULL,
    head_of_department_id VARCHAR(64) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

    -- Audit & Optimistic Locking (§11)
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NULL,
    updated_by VARCHAR(100) NULL,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT pk_departments PRIMARY KEY (id),
    CONSTRAINT uk_departments_code UNIQUE (code),
    CONSTRAINT uk_departments_name UNIQUE (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Query optimization indexes (§96)
CREATE INDEX idx_departments_status ON departments (status);
CREATE INDEX idx_departments_code ON departments (code);
CREATE INDEX idx_departments_name ON departments (name);

-- Seed Initial Standard Hospital Departments (§17)
INSERT INTO departments (id, code, name, description, location, status, version) VALUES
('dept-card-001', 'CARD', 'Cardiology', 'Department of Cardiology and Cardiovascular Medicine', 'Building A, Floor 2', 'ACTIVE', 0),
('dept-neur-001', 'NEUR', 'Neurology', 'Department of Neurology and Neurosciences', 'Building A, Floor 3', 'ACTIVE', 0),
('dept-orth-001', 'ORTH', 'Orthopedics', 'Department of Orthopedic Surgery and Musculoskeletal Health', 'Building B, Floor 1', 'ACTIVE', 0),
('dept-pedi-001', 'PEDI', 'Pediatrics', 'Department of Pediatric and Adolescent Medicine', 'Building B, Floor 2', 'ACTIVE', 0),
('dept-radi-001', 'RADI', 'Radiology', 'Department of Diagnostic and Interventional Radiology', 'Building C, Ground Floor', 'ACTIVE', 0),
('dept-path-001', 'PATH', 'Pathology', 'Department of Pathology and Laboratory Medicine', 'Building C, Floor 1', 'ACTIVE', 0),
('dept-phar-001', 'PHAR', 'Pharmacy', 'Hospital Central and Outpatient Pharmacy Services', 'Building A, Ground Floor', 'ACTIVE', 0),
('dept-emer-001', 'EMER', 'Emergency', 'Emergency Medicine and Trauma Center', 'Building D, Ground Floor', 'ACTIVE', 0),
('dept-genm-001', 'GENM', 'General Medicine', 'Department of Internal and General Medicine', 'Building A, Floor 1', 'ACTIVE', 0)
ON DUPLICATE KEY UPDATE name = VALUES(name);
