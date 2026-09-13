package com.careflow.admission.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Payload for creating a physical hospital bed (§28).
 */
public record CreateBedRequest(
        @NotBlank(message = "Bed number is required")
        @Size(max = 32, message = "Bed number must not exceed 32 characters")
        String bedNumber,

        @NotBlank(message = "Room ID is required")
        String roomId,

        @NotNull(message = "Daily rate is required")
        @DecimalMin(value = "0.0", message = "Daily rate cannot be negative")
        BigDecimal dailyRate
) {
}
