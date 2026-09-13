package com.careflow.pharmacy.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Payload for adding a physical inventory batch to a medication (§25).
 */
public record AddInventoryBatchRequest(
        @NotBlank(message = "Medication ID is required")
        @Size(max = 64, message = "Medication ID cannot exceed 64 characters")
        String medicationId,

        @NotBlank(message = "Batch number is required")
        @Size(max = 64, message = "Batch number cannot exceed 64 characters")
        String batchNumber,

        @NotNull(message = "Expiry date is required")
        @Future(message = "Expiry date must be in the future")
        LocalDate expiryDate,

        @NotNull(message = "Quantity available is required")
        @Min(value = 1, message = "Initial quantity must be at least 1")
        Integer quantityAvailable,

        @Min(value = 0, message = "Reorder threshold must be at least 0")
        Integer reorderThreshold
) {
}
