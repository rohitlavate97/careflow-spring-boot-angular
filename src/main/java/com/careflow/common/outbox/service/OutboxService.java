package com.careflow.common.outbox.service;

import com.careflow.common.event.DomainEvent;

/**
 * Service for capturing domain events within local database transactions via the Transactional Outbox pattern (§53).
 */
public interface OutboxService {

    /**
     * Serializes and writes a domain event to the outbox_events table within the caller's active database transaction.
     *
     * @param event The immutable domain event
     * @param topic Target Kafka topic
     * @param partitionKey Partition key (typically the aggregate root ID)
     */
    void saveEvent(DomainEvent event, String topic, String partitionKey);
}
