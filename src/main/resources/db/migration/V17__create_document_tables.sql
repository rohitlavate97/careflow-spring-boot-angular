-- CareFlow Enterprise Hospital Operations Platform
-- Flyway Migration V17: Medical Documents & Metadata Schema (§34, §103 Phase 13)
-- Creates documents table and indexes for clinical and operational document management

CREATE TABLE IF NOT EXISTS documents (
    id VARCHAR(64) NOT NULL,
    document_number VARCHAR(64) NOT NULL,
    title VARCHAR(200) NOT NULL,
    document_type VARCHAR(50) NOT NULL,
    patient_id VARCHAR(64) NOT NULL,
    uploaded_by_id VARCHAR(64) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    checksum_sha256 VARCHAR(64) NOT NULL,
    storage_path VARCHAR(500) NOT NULL,
    document_version INT NOT NULL DEFAULT 1,
    parent_document_id VARCHAR(64) NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    description VARCHAR(1000) NULL,
    reference_id VARCHAR(64) NULL,
    reference_type VARCHAR(50) NULL,

    -- Audit & Optimistic Locking (§11, §92)
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NULL,
    updated_by VARCHAR(100) NULL,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT pk_documents PRIMARY KEY (id),
    CONSTRAINT uk_documents_document_number UNIQUE (document_number),
    CONSTRAINT fk_documents_patient FOREIGN KEY (patient_id) REFERENCES patients (id),
    CONSTRAINT fk_documents_parent FOREIGN KEY (parent_document_id) REFERENCES documents (id) ON DELETE SET NULL,
    CONSTRAINT chk_documents_size CHECK (file_size > 0),
    CONSTRAINT chk_documents_version CHECK (document_version >= 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_documents_patient_id ON documents (patient_id);
CREATE INDEX idx_documents_type ON documents (document_type);
CREATE INDEX idx_documents_status ON documents (status);
CREATE INDEX idx_documents_parent_id ON documents (parent_document_id);
CREATE INDEX idx_documents_uploaded_by ON documents (uploaded_by_id);
CREATE INDEX idx_documents_reference ON documents (reference_type, reference_id);
