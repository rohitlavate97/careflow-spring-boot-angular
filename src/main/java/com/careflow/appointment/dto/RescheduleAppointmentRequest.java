package com.careflow.appointment.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * Request payload for rescheduling an existing appointment (§19, §40).
 */
public record RescheduleAppointmentRequest(
        @NotNull(message = "New appointment date and time is required")
        @Future(message = "New appointment date and time must be in the future")
        LocalDateTime newDateTime
) {
}
