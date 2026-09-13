-- CareFlow Enterprise Hospital Operations Platform
-- Flyway Migration V18: Multi-Channel Notifications & Preferences Schema (§35, §103 Phase 14)
-- Creates notifications and notification_preferences tables with performance indexes

CREATE TABLE IF NOT EXISTS notifications (
    id VARCHAR(64) NOT NULL,
    recipient_user_id VARCHAR(64) NULL,
    recipient_email VARCHAR(100) NULL,
    recipient_phone VARCHAR(30) NULL,
    patient_id VARCHAR(64) NULL,
    notification_type VARCHAR(50) NOT NULL,
    channel VARCHAR(20) NOT NULL,
    title VARCHAR(200) NOT NULL,
    message VARCHAR(2000) NOT NULL,
    reference_type VARCHAR(50) NULL,
    reference_id VARCHAR(64) NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    failure_reason VARCHAR(500) NULL,
    sent_at TIMESTAMP NULL,
    read_at TIMESTAMP NULL,

    -- Audit & Optimistic Locking (§11, §92)
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NULL,
    updated_by VARCHAR(100) NULL,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT pk_notifications PRIMARY KEY (id),
    CONSTRAINT chk_notifications_channel CHECK (channel IN ('IN_APP', 'EMAIL', 'SMS')),
    CONSTRAINT chk_notifications_status CHECK (status IN ('PENDING', 'SENT', 'DELIVERED', 'READ', 'FAILED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_notifications_recipient_user ON notifications (recipient_user_id, status);
CREATE INDEX idx_notifications_patient ON notifications (patient_id);
CREATE INDEX idx_notifications_type ON notifications (notification_type);
CREATE INDEX idx_notifications_channel ON notifications (channel);
CREATE INDEX idx_notifications_created_at ON notifications (created_at);
CREATE INDEX idx_notifications_reference ON notifications (reference_type, reference_id);

CREATE TABLE IF NOT EXISTS notification_preferences (
    id VARCHAR(64) NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    email_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    sms_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    in_app_enabled BOOLEAN NOT NULL DEFAULT TRUE,

    -- Audit & Optimistic Locking
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NULL,
    updated_by VARCHAR(100) NULL,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT pk_notification_preferences PRIMARY KEY (id),
    CONSTRAINT uk_notification_preferences_user UNIQUE (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_notification_preferences_user ON notification_preferences (user_id);
