package com.careflow.admission.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload for transferring an inpatient to a different hospital bed (§28).
 */
public record TransferBedRequest(
        @NotBlank(message = "Target bed ID is required")
        String targetBedId,

        @NotBlank(message = "Transfer reason is required")
        @Size(max = 500, message = "Transfer reason must not exceed 500 characters")
        String transferReason,

        String transferredById
) {
}
