package com.careflow.laboratory.dto;

import com.careflow.laboratory.domain.AbnormalityFlag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Diagnostic result entry payload for an individual test parameter (§27).
 */
public record EnterLabResultRequest(
        @NotBlank(message = "Order item ID is required")
        String orderItemId,

        String sampleId,

        @NotBlank(message = "Test parameter name is required")
        @Size(max = 100, message = "Test parameter name must not exceed 100 characters")
        String testParameter,

        @NotBlank(message = "Result value is required")
        @Size(max = 255, message = "Result value must not exceed 255 characters")
        String resultValue,

        Double numericValue,

        @Size(max = 50, message = "Unit must not exceed 50 characters")
        String unit,

        @Size(max = 100, message = "Reference range must not exceed 100 characters")
        String referenceRange,

        AbnormalityFlag abnormalityFlag,

        @Size(max = 1000, message = "Technician notes must not exceed 1000 characters")
        String technicianNotes,

        String technicianStaffId
) {
}
