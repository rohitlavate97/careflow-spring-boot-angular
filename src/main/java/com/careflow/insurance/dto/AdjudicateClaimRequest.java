package com.careflow.insurance.dto;

import com.careflow.insurance.domain.ClaimStatus;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Request DTO to adjudicate a submitted claim with an approval, partial approval, or rejection decision (§33).
 */
public record AdjudicateClaimRequest(
        @NotNull(message = "Adjudication decision is required")
        ClaimStatus decision,

        BigDecimal approvedAmount,

        String notes,

        String denialReason
) {}
