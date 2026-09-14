-- CareFlow Enterprise Hospital Operations Platform
-- Flyway Migration V20: Operational Reporting & Analytics Schema (§37, §103 Phase 16)
-- Creates report_executions table for auditing generated operational dashboards and KPI queries

CREATE TABLE IF NOT EXISTS report_executions (
    id VARCHAR(64) NOT NULL,
    report_type VARCHAR(50) NOT NULL,
    requested_by VARCHAR(100) NOT NULL,
    parameters VARCHAR(1000) NULL,
    execution_time_ms BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'COMPLETED',

    -- Audit & Optimistic Locking (§11, §92)
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NULL,
    updated_by VARCHAR(100) NULL,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT pk_report_executions PRIMARY KEY (id),
    CONSTRAINT chk_report_executions_status CHECK (status IN ('COMPLETED', 'FAILED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_report_exec_type_time ON report_executions (report_type, created_at DESC);
CREATE INDEX idx_report_exec_user_time ON report_executions (requested_by, created_at DESC);
