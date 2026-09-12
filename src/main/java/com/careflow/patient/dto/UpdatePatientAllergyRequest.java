package com.careflow.patient.dto;

import com.careflow.patient.domain.AllergySeverity;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Request payload for updating clinical allergy details (§16, §40).
 */
public record UpdatePatientAllergyRequest(
        @NotNull(message = "Allergy severity is required")
        AllergySeverity severity,

        @Size(max = 255, message = "Reaction description must not exceed 255 characters")
        String reaction,

        @Size(max = 1000, message = "Clinical notes must not exceed 1000 characters")
        String notes,

        @PastOrPresent(message = "Diagnosed date cannot be in the future")
        LocalDate diagnosedDate
) {
}
