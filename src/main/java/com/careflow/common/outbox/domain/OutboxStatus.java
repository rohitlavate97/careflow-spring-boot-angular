package com.careflow.common.outbox.domain;

/**
 * Lifecycle status for reliable transactional outbox events (§53).
 */
public enum OutboxStatus {
    PENDING,
    PUBLISHED,
    FAILED
}
