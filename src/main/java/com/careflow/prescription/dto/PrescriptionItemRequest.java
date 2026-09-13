package com.careflow.prescription.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Payload for an individual prescription medication order item (§24).
 */
public record PrescriptionItemRequest(
        @NotBlank(message = "Medication ID is required")
        @Size(max = 64, message = "Medication ID cannot exceed 64 characters")
        String medicationId,

        @NotBlank(message = "Dosage is required (e.g. 500 mg)")
        @Size(max = 100, message = "Dosage cannot exceed 100 characters")
        String dosage,

        @NotBlank(message = "Frequency is required (e.g. Twice daily)")
        @Size(max = 100, message = "Frequency cannot exceed 100 characters")
        String frequency,

        @NotBlank(message = "Duration is required (e.g. 5 days)")
        @Size(max = 100, message = "Duration cannot exceed 100 characters")
        String duration,

        @NotNull(message = "Quantity prescribed is required")
        @Min(value = 1, message = "Quantity prescribed must be at least 1")
        Integer quantityPrescribed,

        String instructions
) {
}
