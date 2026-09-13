package com.careflow.laboratory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Diagnostic lab order cancellation request payload (§27).
 */
public record CancelLabOrderRequest(
        @NotBlank(message = "Cancellation reason is required")
        @Size(max = 1000, message = "Cancellation reason must not exceed 1000 characters")
        String cancellationReason
) {
}
