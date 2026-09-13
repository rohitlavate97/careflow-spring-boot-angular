package com.careflow.insurance.dto;

import java.math.BigDecimal;

/**
 * Response DTO for a service line item on an insurance claim (§33).
 */
public record ClaimItemResponse(
        String id,
        String claimId,
        String invoiceItemId,
        String serviceCode,
        String description,
        BigDecimal claimedAmount,
        BigDecimal approvedAmount,
        String rejectionReason
) {}
