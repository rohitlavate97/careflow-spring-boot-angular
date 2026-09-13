package com.careflow.appointment.dto;

import com.careflow.appointment.domain.AppointmentStatus;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * Detailed representation of an appointment (§19, §40).
 */
public record AppointmentResponse(
        String id,
        String patientId,
        String doctorId,
        String departmentId,
        LocalDateTime appointmentDateTime,
        Integer durationMinutes,
        AppointmentStatus status,
        String reasonForVisit,
        String cancellationReason,
        Instant createdAt,
        Instant updatedAt,
        Long version
) {
}
