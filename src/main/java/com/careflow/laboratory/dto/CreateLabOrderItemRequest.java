package com.careflow.laboratory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Item specification when submitting a lab order requisition (§27).
 */
public record CreateLabOrderItemRequest(
        @NotBlank(message = "Lab test ID is required")
        String labTestId,

        @Size(max = 500, message = "Item notes must not exceed 500 characters")
        String notes
) {
}
