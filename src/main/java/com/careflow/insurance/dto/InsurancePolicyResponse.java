package com.careflow.insurance.dto;

import com.careflow.insurance.domain.PolicyRelationship;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Response DTO for a patient insurance policy (§33).
 */
public record InsurancePolicyResponse(
        String id,
        String policyNumber,
        String groupNumber,
        String patientId,
        String providerId,
        String providerName,
        String payerId,
        String policyHolderName,
        PolicyRelationship relationship,
        LocalDate coverageStartDate,
        LocalDate coverageEndDate,
        BigDecimal coPayAmount,
        BigDecimal coveragePercentage,
        BigDecimal deductible,
        boolean active,
        Instant createdAt
) {}
