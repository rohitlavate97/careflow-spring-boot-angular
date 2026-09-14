package com.careflow.common.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain event emitted when an outpatient appointment is cancelled (§51).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AppointmentCancelledEvent(
        String eventId,
        String appointmentId,
        String patientId,
        String doctorId,
        String cancellationReason,
        Instant occurredAt
) implements DomainEvent {

    public AppointmentCancelledEvent(
            String appointmentId,
            String patientId,
            String doctorId,
            String cancellationReason
    ) {
        this(
                UUID.randomUUID().toString(),
                appointmentId,
                patientId,
                doctorId,
                cancellationReason,
                Instant.now()
        );
    }

    @Override
    public String getEventId() {
        return eventId;
    }

    @Override
    public String getEventType() {
        return "AppointmentCancelled";
    }

    @Override
    public String getAggregateType() {
        return "APPOINTMENT";
    }

    @Override
    public String getAggregateId() {
        return appointmentId;
    }

    @Override
    public Instant getOccurredAt() {
        return occurredAt;
    }
}
