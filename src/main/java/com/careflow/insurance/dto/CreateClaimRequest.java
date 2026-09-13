package com.careflow.insurance.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Request DTO to create a draft insurance claim (§33).
 */
public record CreateClaimRequest(
        @NotBlank(message = "Policy ID is required")
        String policyId,

        @NotBlank(message = "Patient ID is required")
        String patientId,

        String invoiceId,

        @NotEmpty(message = "At least one claim service item is required")
        List<@Valid CreateClaimItemRequest> items
) {}
