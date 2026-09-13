package com.careflow.pharmacy.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Payload for dispensing medication against a prescription order item (§25, §26).
 */
public record DispenseMedicationRequest(
        @NotBlank(message = "Prescription ID is required")
        @Size(max = 64, message = "Prescription ID cannot exceed 64 characters")
        String prescriptionId,

        @NotBlank(message = "Prescription Item ID is required")
        @Size(max = 64, message = "Prescription Item ID cannot exceed 64 characters")
        String prescriptionItemId,

        @Size(max = 64, message = "Inventory Batch ID cannot exceed 64 characters")
        String inventoryBatchId,

        @NotBlank(message = "Pharmacist ID is required")
        @Size(max = 64, message = "Pharmacist ID cannot exceed 64 characters")
        String pharmacistId,

        @NotNull(message = "Quantity to dispense is required")
        @Min(value = 1, message = "Quantity to dispense must be at least 1")
        Integer quantityToDispense,

        String notes
) {
}
