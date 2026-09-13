package com.careflow.insurance.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO to register a new insurance payer in the system (§33).
 */
public record CreateInsuranceProviderRequest(
        @NotBlank(message = "Provider code is required")
        String providerCode,

        @NotBlank(message = "Provider name is required")
        String name,

        @NotBlank(message = "Payer ID is required")
        String payerId,

        String contactEmail,

        String contactPhone,

        String address
) {}
