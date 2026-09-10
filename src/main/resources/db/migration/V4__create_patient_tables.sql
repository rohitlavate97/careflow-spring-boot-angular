-- CareFlow Enterprise Hospital Operations Platform
-- Flyway Migration V4: Patient Management Schema
-- Creates patients table with demographics, embedded value objects, audit columns, and specialized query indexes (§16, §96)

CREATE TABLE IF NOT EXISTS patients (
    id VARCHAR(64) NOT NULL,
    mrn VARCHAR(32) NOT NULL,
    first_name VARCHAR(50) NOT NULL,
    middle_name VARCHAR(50) NULL,
    last_name VARCHAR(50) NOT NULL,
    date_of_birth DATE NOT NULL,
    gender VARCHAR(20) NOT NULL,
    blood_group VARCHAR(20) NULL,
    email VARCHAR(100) NULL,
    phone VARCHAR(20) NOT NULL,
    
    -- Embedded Address
    address_line1 VARCHAR(150) NULL,
    address_line2 VARCHAR(150) NULL,
    city VARCHAR(50) NULL,
    state VARCHAR(50) NULL,
    postal_code VARCHAR(20) NULL,
    country VARCHAR(50) NULL,
    
    -- Embedded Emergency Contact
    emergency_contact_name VARCHAR(100) NULL,
    emergency_contact_relationship VARCHAR(50) NULL,
    emergency_contact_phone VARCHAR(20) NULL,
    
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    
    -- Audit & Optimistic Locking (§11)
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NULL,
    updated_by VARCHAR(100) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    
    CONSTRAINT pk_patients PRIMARY KEY (id),
    CONSTRAINT uk_patients_mrn UNIQUE (mrn)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Query optimization indexes (§96)
CREATE INDEX idx_patients_phone ON patients (phone);
CREATE INDEX idx_patients_email ON patients (email);
CREATE INDEX idx_patients_dob ON patients (date_of_birth);
CREATE INDEX idx_patients_status ON patients (status);
CREATE INDEX idx_patients_last_first_name ON patients (last_name, first_name);
