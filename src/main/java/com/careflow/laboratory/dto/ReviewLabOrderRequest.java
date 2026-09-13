package com.careflow.laboratory.dto;

import com.careflow.laboratory.domain.LabReviewStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Physician review and sign-off payload for completed laboratory orders (§27).
 */
public record ReviewLabOrderRequest(
        @NotNull(message = "Review status is required")
        LabReviewStatus reviewStatus,

        @Size(max = 2000, message = "Review notes must not exceed 2000 characters")
        String reviewNotes,

        String reviewerDoctorId
) {
}
