-- CareFlow Enterprise Hospital Operations Platform
-- Flyway Migration V22: Asynchronous Events, Transactional Outbox & Consumer Idempotency Schema (§51, §53, §103 Phase 18)
-- Creates outbox_events and processed_events tables

CREATE TABLE IF NOT EXISTS outbox_events (
    id VARCHAR(64) NOT NULL,
    aggregate_type VARCHAR(64) NOT NULL,
    aggregate_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(128) NOT NULL,
    payload LONGTEXT NOT NULL,
    topic VARCHAR(128) NOT NULL,
    partition_key VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    attempt_count INT NOT NULL DEFAULT 0,
    occurred_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMP NULL,
    error_message TEXT NULL,

    CONSTRAINT pk_outbox_events PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_outbox_status_occurred ON outbox_events (status, occurred_at);
CREATE INDEX idx_outbox_aggregate ON outbox_events (aggregate_type, aggregate_id);

CREATE TABLE IF NOT EXISTS processed_events (
    event_id VARCHAR(64) NOT NULL,
    consumer_group VARCHAR(128) NOT NULL,
    event_type VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'SUCCESS',
    processed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_processed_events PRIMARY KEY (event_id, consumer_group)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_processed_events_type ON processed_events (event_type);
