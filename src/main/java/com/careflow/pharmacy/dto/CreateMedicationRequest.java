package com.careflow.pharmacy.dto;

import com.careflow.pharmacy.domain.MedicationForm;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Payload for registering a new medication in the formulary catalog (§25).
 */
public record CreateMedicationRequest(
        @NotBlank(message = "Medication code is required")
        @Size(max = 32, message = "Medication code cannot exceed 32 characters")
        String code,

        @NotBlank(message = "Medication brand name is required")
        @Size(max = 200, message = "Medication name cannot exceed 200 characters")
        String name,

        @NotBlank(message = "Generic name is required")
        @Size(max = 200, message = "Generic name cannot exceed 200 characters")
        String genericName,

        @NotNull(message = "Medication form is required (e.g. TABLET, CAPSULE, SYRUP)")
        MedicationForm form,

        @NotBlank(message = "Strength is required (e.g. 500 mg)")
        @Size(max = 50, message = "Strength cannot exceed 50 characters")
        String strength,

        @NotNull(message = "Unit price is required")
        @DecimalMin(value = "0.00", message = "Unit price must be greater than or equal to 0.00")
        BigDecimal unitPrice,

        @NotNull(message = "Reorder threshold is required")
        @Min(value = 0, message = "Reorder threshold must be greater than or equal to 0")
        Integer reorderThreshold
) {
}
