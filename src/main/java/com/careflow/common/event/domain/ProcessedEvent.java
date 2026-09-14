package com.careflow.common.event.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;

/**
 * Entity recording processed events per consumer group to guarantee exactly-once business semantics
 * against duplicate Kafka message deliveries (§51, §76 Scenario 7).
 */
@Entity
@Table(name = "processed_events")
@IdClass(ProcessedEventId.class)
public class ProcessedEvent {

    @Id
    @Column(name = "event_id", nullable = false, length = 64)
    private String eventId;

    @Id
    @Column(name = "consumer_group", nullable = false, length = 128)
    private String consumerGroup;

    @Column(name = "event_type", nullable = false, length = 128)
    private String eventType;

    @Column(name = "status", nullable = false, length = 32)
    private String status = "SUCCESS";

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt = Instant.now();

    protected ProcessedEvent() {
    }

    public ProcessedEvent(String eventId, String consumerGroup, String eventType, String status) {
        this.eventId = Objects.requireNonNull(eventId, "eventId must not be null");
        this.consumerGroup = Objects.requireNonNull(consumerGroup, "consumerGroup must not be null");
        this.eventType = Objects.requireNonNull(eventType, "eventType must not be null");
        this.status = status != null ? status : "SUCCESS";
        this.processedAt = Instant.now();
    }

    public String getEventId() {
        return eventId;
    }

    public String getConsumerGroup() {
        return consumerGroup;
    }

    public String getEventType() {
        return eventType;
    }

    public String getStatus() {
        return status;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProcessedEvent that)) return false;
        return Objects.equals(eventId, that.eventId) && Objects.equals(consumerGroup, that.consumerGroup);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventId, consumerGroup);
    }
}
