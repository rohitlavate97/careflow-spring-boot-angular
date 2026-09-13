package com.careflow.queue.dto;

import jakarta.validation.constraints.Size;

/**
 * Optional request payload when calling the next waiting patient into a specific consultation room (§21).
 */
public record CallNextPatientRequest(
        @Size(max = 64, message = "Doctor ID must not exceed 64 characters")
        String doctorId
) {
}
