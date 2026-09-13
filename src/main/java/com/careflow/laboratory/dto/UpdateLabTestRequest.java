package com.careflow.laboratory.dto;

import com.careflow.laboratory.domain.LabTestCategory;
import com.careflow.laboratory.domain.SpecimenType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Request payload for updating an existing diagnostic lab test definition (§27).
 */
public record UpdateLabTestRequest(
        @NotBlank(message = "Test name is required")
        @Size(max = 150, message = "Test name must not exceed 150 characters")
        String name,

        @NotNull(message = "Category is required")
        LabTestCategory category,

        @NotNull(message = "Specimen type is required")
        SpecimenType specimenType,

        @Size(max = 100, message = "Reference range must not exceed 100 characters")
        String referenceRange,

        @Size(max = 50, message = "Unit must not exceed 50 characters")
        String unit,

        @NotNull(message = "Turnaround hours is required")
        @Min(value = 1, message = "Turnaround hours must be at least 1")
        Integer turnaroundHours,

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.0", message = "Price cannot be negative")
        BigDecimal price,

        @NotNull(message = "Active status is required")
        Boolean active
) {
}
