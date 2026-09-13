package com.careflow.pharmacy.dto;

import com.careflow.pharmacy.domain.MedicationForm;
import com.careflow.pharmacy.domain.MedicationStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Payload for updating an existing medication in the catalog (§25).
 */
public record UpdateMedicationRequest(
        @Size(max = 200, message = "Medication name cannot exceed 200 characters")
        String name,

        @Size(max = 200, message = "Generic name cannot exceed 200 characters")
        String genericName,

        MedicationForm form,

        @Size(max = 50, message = "Strength cannot exceed 50 characters")
        String strength,

        @DecimalMin(value = "0.00", message = "Unit price must be greater than or equal to 0.00")
        BigDecimal unitPrice,

        @Min(value = 0, message = "Reorder threshold must be greater than or equal to 0")
        Integer reorderThreshold,

        MedicationStatus status
) {
}
