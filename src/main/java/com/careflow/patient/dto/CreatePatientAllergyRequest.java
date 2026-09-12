package com.careflow.patient.dto;

import com.careflow.patient.domain.AllergenCategory;
import com.careflow.patient.domain.AllergySeverity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Request payload for documenting a new patient allergy (§16, §40).
 */
public record CreatePatientAllergyRequest(
        @NotBlank(message = "Allergen name is required")
        @Size(max = 100, message = "Allergen name must not exceed 100 characters")
        String allergen,

        @NotNull(message = "Allergen category is required")
        AllergenCategory category,

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
