package com.careflow.common.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Domain event emitted when an outpatient appointment is booked (§51).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AppointmentBookedEvent(
        String eventId,
        String appointmentId,
        String patientId,
        String doctorId,
        String departmentId,
        LocalDateTime appointmentDateTime,
        Integer durationMinutes,
        Instant occurredAt
) implements DomainEvent {

    public AppointmentBookedEvent(
            String appointmentId,
            String patientId,
            String doctorId,
            String departmentId,
            LocalDateTime appointmentDateTime,
            Integer durationMinutes
    ) {
        this(
                UUID.randomUUID().toString(),
                appointmentId,
                patientId,
                doctorId,
                departmentId,
                appointmentDateTime,
                durationMinutes,
                Instant.now()
        );
    }

    @Override
    public String getEventId() {
        return eventId;
    }

    @Override
    public String getEventType() {
        return "AppointmentBooked";
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
