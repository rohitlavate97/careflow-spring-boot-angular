package com.careflow.appointment.dto;

import jakarta.validation.constraints.Size;

/**
 * Request payload for cancelling an appointment (§19, §40).
 */
public record CancelAppointmentRequest(
        @Size(max = 500, message = "Cancellation reason must not exceed 500 characters")
        String cancellationReason
) {
}
