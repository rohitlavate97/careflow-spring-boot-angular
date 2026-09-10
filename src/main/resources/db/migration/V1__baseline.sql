-- CareFlow Enterprise Hospital Operations Platform
-- Flyway Baseline Migration
-- Establishes initial database metadata and verifies migration execution

CREATE TABLE IF NOT EXISTS system_metadata (
    id VARCHAR(64) NOT NULL,
    metadata_key VARCHAR(128) NOT NULL,
    metadata_value VARCHAR(512) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT pk_system_metadata PRIMARY KEY (id),
    CONSTRAINT uk_system_metadata_key UNIQUE (metadata_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO system_metadata (id, metadata_key, metadata_value, version)
VALUES ('meta-careflow-init', 'platform_initialization_version', '0.0.1-SNAPSHOT', 0)
ON DUPLICATE KEY UPDATE metadata_value = VALUES(metadata_value);
