-- CareFlow Enterprise Hospital Operations Platform
-- Flyway Migration V21: Administration & System Configuration Schema (§38, §103 Phase 17)
-- Creates system_settings table and seeds initial enterprise facility configurations

CREATE TABLE IF NOT EXISTS system_settings (
    id VARCHAR(64) NOT NULL,
    setting_key VARCHAR(128) NOT NULL,
    setting_value VARCHAR(1024) NOT NULL,
    category VARCHAR(64) NOT NULL,
    data_type VARCHAR(32) NOT NULL,
    description VARCHAR(255) NULL,
    is_encrypted BOOLEAN NOT NULL DEFAULT FALSE,
    is_editable BOOLEAN NOT NULL DEFAULT TRUE,

    -- Audit & Optimistic Locking (§11, §92)
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NULL,
    updated_by VARCHAR(100) NULL,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT pk_system_settings PRIMARY KEY (id),
    CONSTRAINT uk_system_settings_key UNIQUE (setting_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_sys_setting_key ON system_settings (setting_key);
CREATE INDEX idx_sys_setting_category ON system_settings (category);

-- Seed Initial Facility & System Configuration (§38)
INSERT INTO system_settings (id, setting_key, setting_value, category, data_type, description, is_encrypted, is_editable, created_by, version)
VALUES
    ('set-001', 'careflow.facility.name', 'CareFlow Central Hospital', 'FACILITY', 'STRING', 'Primary clinical institution facility name', FALSE, TRUE, 'SYSTEM', 0),
    ('set-002', 'careflow.facility.code', 'CF-MAIN', 'FACILITY', 'STRING', 'Unique organizational hospital identification code', FALSE, FALSE, 'SYSTEM', 0),
    ('set-003', 'careflow.facility.currency', 'USD', 'BILLING', 'STRING', 'Default financial currency code for invoicing', FALSE, TRUE, 'SYSTEM', 0),
    ('set-004', 'careflow.facility.timezone', 'UTC', 'FACILITY', 'STRING', 'Standard operating hospital timezone', FALSE, TRUE, 'SYSTEM', 0),
    ('set-005', 'careflow.appointment.default_slot_minutes', '30', 'SCHEDULING', 'NUMBER', 'Default doctor consultation slot duration in minutes', FALSE, TRUE, 'SYSTEM', 0),
    ('set-006', 'careflow.appointment.cancellation_cutoff_hours', '24', 'SCHEDULING', 'NUMBER', 'Minimum advance notice required to cancel scheduled appointments', FALSE, TRUE, 'SYSTEM', 0),
    ('set-007', 'careflow.queue.max_active_waiting', '100', 'CLINICAL', 'NUMBER', 'Maximum outpatient queue capacity per department', FALSE, TRUE, 'SYSTEM', 0),
    ('set-008', 'careflow.billing.tax_rate_percent', '0.0', 'BILLING', 'NUMBER', 'Applicable value-added tax percentage applied to patient billing', FALSE, TRUE, 'SYSTEM', 0),
    ('set-009', 'careflow.system.maintenance_mode', 'false', 'MAINTENANCE', 'BOOLEAN', 'Global emergency maintenance lock flag', FALSE, TRUE, 'SYSTEM', 0),
    ('set-010', 'careflow.system.maintenance_reason', '', 'MAINTENANCE', 'STRING', 'Administrative justification for active maintenance state', FALSE, TRUE, 'SYSTEM', 0);
