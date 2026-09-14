-- CareFlow Enterprise Hospital Operations Platform
-- Flyway Migration V19: Immutable Audit Logging Schema (§36, §74, §103 Phase 15)
-- Creates audit_logs table with indexes for actor, patient, resource, and timestamp queries

CREATE TABLE IF NOT EXISTS audit_logs (
    id VARCHAR(64) NOT NULL,
    actor_user_id VARCHAR(100) NOT NULL,
    action VARCHAR(60) NOT NULL,
    resource_type VARCHAR(60) NOT NULL,
    resource_id VARCHAR(100) NULL,
    patient_id VARCHAR(64) NULL,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    previous_value TEXT NULL,
    new_value TEXT NULL,
    ip_address VARCHAR(45) NULL,
    correlation_id VARCHAR(64) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'SUCCESS',
    details VARCHAR(1000) NULL,

    CONSTRAINT pk_audit_logs PRIMARY KEY (id),
    CONSTRAINT chk_audit_logs_status CHECK (status IN ('SUCCESS', 'FAILURE', 'ACCESS_DENIED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_audit_timestamp ON audit_logs (timestamp DESC);
CREATE INDEX idx_audit_actor_timestamp ON audit_logs (actor_user_id, timestamp DESC);
CREATE INDEX idx_audit_patient_timestamp ON audit_logs (patient_id, timestamp DESC);
CREATE INDEX idx_audit_resource ON audit_logs (resource_type, resource_id);
CREATE INDEX idx_audit_action_timestamp ON audit_logs (action, timestamp DESC);
CREATE INDEX idx_audit_correlation_id ON audit_logs (correlation_id);
