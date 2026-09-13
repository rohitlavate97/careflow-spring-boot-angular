package com.careflow.insurance.dto;

import com.careflow.insurance.domain.ClaimStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Detailed response DTO for an insurance claim including payer context and line items (§33).
 */
public record InsuranceClaimResponse(
        String id,
        String claimNumber,
        String policyId,
        String policyNumber,
        String providerName,
        String payerId,
        String patientId,
        String invoiceId,
        ClaimStatus status,
        BigDecimal totalClaimedAmount,
        BigDecimal approvedAmount,
        BigDecimal patientResponsibility,
        String denialReason,
        String adjudicationNotes,
        Instant submittedAt,
        Instant adjudicatedAt,
        Instant settledAt,
        Instant createdAt,
        Instant updatedAt,
        List<ClaimItemResponse> items
) {}
