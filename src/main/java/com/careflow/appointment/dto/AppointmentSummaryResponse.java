package com.careflow.appointment.dto;

import com.careflow.appointment.domain.AppointmentStatus;

import java.time.LocalDateTime;

/**
 * Summary view of an appointment for calendars, queue views, and paginated searches (§19, §40).
 */
public record AppointmentSummaryResponse(
        String id,
        String patientId,
        String doctorId,
        String departmentId,
        LocalDateTime appointmentDateTime,
        Integer durationMinutes,
        AppointmentStatus status
) {
}
