package com.careflow.common.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain event emitted when laboratory results are finalized (§51).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record LabResultCompletedEvent(
        String eventId,
        String orderId,
        String patientId,
        String testName,
        Instant completedAt,
        Instant occurredAt
) implements DomainEvent {

    public LabResultCompletedEvent(
            String orderId,
            String patientId,
            String testName,
            Instant completedAt
    ) {
        this(
                UUID.randomUUID().toString(),
                orderId,
                patientId,
                testName,
                completedAt,
                Instant.now()
        );
    }

    @Override
    public String getEventId() {
        return eventId;
    }

    @Override
    public String getEventType() {
        return "LabResultCompleted";
    }

    @Override
    public String getAggregateType() {
        return "LABORATORY";
    }

    @Override
    public String getAggregateId() {
        return orderId;
    }

    @Override
    public Instant getOccurredAt() {
        return occurredAt;
    }
}
