package com.careflow.common.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Domain event emitted when a billing payment is captured (§51).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PaymentCompletedEvent(
        String eventId,
        String paymentId,
        String invoiceId,
        String patientId,
        BigDecimal amount,
        String paymentMethod,
        Instant occurredAt
) implements DomainEvent {

    public PaymentCompletedEvent(
            String paymentId,
            String invoiceId,
            String patientId,
            BigDecimal amount,
            String paymentMethod
    ) {
        this(
                UUID.randomUUID().toString(),
                paymentId,
                invoiceId,
                patientId,
                amount,
                paymentMethod,
                Instant.now()
        );
    }

    @Override
    public String getEventId() {
        return eventId;
    }

    @Override
    public String getEventType() {
        return "PaymentCompleted";
    }

    @Override
    public String getAggregateType() {
        return "BILLING";
    }

    @Override
    public String getAggregateId() {
        return paymentId;
    }

    @Override
    public Instant getOccurredAt() {
        return occurredAt;
    }
}
