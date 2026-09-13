package com.careflow.admission.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload for discharging an admitted inpatient (§28).
 */
public record DischargePatientRequest(
        @NotBlank(message = "Discharge summary is required")
        @Size(max = 2000, message = "Discharge summary must not exceed 2000 characters")
        String dischargeSummary
) {
}
