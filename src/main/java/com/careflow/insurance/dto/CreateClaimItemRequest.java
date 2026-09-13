package com.careflow.insurance.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Request DTO for a service line item on an insurance claim (§33).
 */
public record CreateClaimItemRequest(
        String invoiceItemId,

        @NotBlank(message = "Service code is required")
        String serviceCode,

        @NotBlank(message = "Description is required")
        String description,

        @NotNull(message = "Claimed amount is required")
        @DecimalMin(value = "0.01", message = "Claimed amount must be greater than zero")
        BigDecimal claimedAmount
) {}
