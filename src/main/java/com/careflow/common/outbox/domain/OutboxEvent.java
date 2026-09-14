package com.careflow.common.outbox.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;

/**
 * Entity representing an asynchronous domain event persisted within a local database transaction
 * to guarantee atomic at-least-once publishing via the Transactional Outbox pattern (§53).
 */
@Entity
@Table(name = "outbox_events")
public class OutboxEvent {

    @Id
    @Column(name = "id", nullable = false, length = 64)
    private String id;

    @Column(name = "aggregate_type", nullable = false, length = 64)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false, length = 64)
    private String aggregateId;

    @Column(name = "event_type", nullable = false, length = 128)
    private String eventType;

    @Column(name = "payload", nullable = false, columnDefinition = "LONGTEXT")
    private String payload;

    @Column(name = "topic", nullable = false, length = 128)
    private String topic;

    @Column(name = "partition_key", nullable = false, length = 128)
    private String partitionKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private OutboxStatus status = OutboxStatus.PENDING;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount = 0;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    protected OutboxEvent() {
    }

    public OutboxEvent(
            String id,
            String aggregateType,
            String aggregateId,
            String eventType,
            String payload,
            String topic,
            String partitionKey,
            Instant occurredAt
    ) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.aggregateType = Objects.requireNonNull(aggregateType, "aggregateType must not be null");
        this.aggregateId = Objects.requireNonNull(aggregateId, "aggregateId must not be null");
        this.eventType = Objects.requireNonNull(eventType, "eventType must not be null");
        this.payload = Objects.requireNonNull(payload, "payload must not be null");
        this.topic = Objects.requireNonNull(topic, "topic must not be null");
        this.partitionKey = Objects.requireNonNull(partitionKey, "partitionKey must not be null");
        this.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        this.status = OutboxStatus.PENDING;
        this.attemptCount = 0;
    }

    public void markPublished(Instant publishedAt) {
        this.status = OutboxStatus.PUBLISHED;
        this.publishedAt = publishedAt;
        this.errorMessage = null;
    }

    public void recordFailure(String error) {
        this.attemptCount++;
        this.errorMessage = error;
        if (this.attemptCount >= 5) {
            this.status = OutboxStatus.FAILED;
        }
    }

    public String getId() {
        return id;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getPayload() {
        return payload;
    }

    public String getTopic() {
        return topic;
    }

    public String getPartitionKey() {
        return partitionKey;
    }

    public OutboxStatus getStatus() {
        return status;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OutboxEvent that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
