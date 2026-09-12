package com.careflow.patient.dto;

import com.careflow.patient.domain.AllergyStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request payload for transitioning the lifecycle status of an allergy record (§16, §69).
 */
public record UpdateAllergyStatusRequest(
        @NotNull(message = "Allergy status is required")
        AllergyStatus status,

        @Size(max = 1000, message = "Status transition reason/notes must not exceed 1000 characters")
        String notes
) {
}
