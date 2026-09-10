package com.careflow.patient.dto;

import com.careflow.patient.domain.PatientStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request payload for transitioning patient operational status (§16, §69).
 */
public record UpdatePatientStatusRequest(
        @NotNull(message = "Patient status is required")
        PatientStatus status,

        @Size(max = 255, message = "Reason must not exceed 255 characters")
        String reason
) {
}
