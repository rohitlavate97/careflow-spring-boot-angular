package com.careflow.insurance.dto;

import com.careflow.insurance.domain.ClaimStatus;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Lightweight summary response DTO for claim listings and queues (§33).
 */
public record ClaimSummaryResponse(
        String id,
        String claimNumber,
        String policyId,
        String patientId,
        String invoiceId,
        ClaimStatus status,
        BigDecimal totalClaimedAmount,
        BigDecimal approvedAmount,
        BigDecimal patientResponsibility,
        Instant submittedAt,
        Instant adjudicatedAt,
        Instant settledAt,
        Instant createdAt
) {}
