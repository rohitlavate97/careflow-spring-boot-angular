package com.careflow.appointment.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * Request payload for booking a patient consultation slot (§19, §40).
 */
public record BookAppointmentRequest(
        @NotBlank(message = "Patient ID is required")
        @Size(max = 64, message = "Patient ID must not exceed 64 characters")
        String patientId,

        @NotBlank(message = "Doctor ID is required")
        @Size(max = 64, message = "Doctor ID must not exceed 64 characters")
        String doctorId,

        @NotBlank(message = "Department ID is required")
        @Size(max = 64, message = "Department ID must not exceed 64 characters")
        String departmentId,

        @NotNull(message = "Appointment date and time is required")
        @Future(message = "Appointment date and time must be in the future")
        LocalDateTime appointmentDateTime,

        @NotNull(message = "Duration in minutes is required")
        @Min(value = 5, message = "Duration must be at least 5 minutes")
        @Max(value = 240, message = "Duration must not exceed 240 minutes")
        Integer durationMinutes,

        @Size(max = 500, message = "Reason for visit must not exceed 500 characters")
        String reasonForVisit
) {
}
