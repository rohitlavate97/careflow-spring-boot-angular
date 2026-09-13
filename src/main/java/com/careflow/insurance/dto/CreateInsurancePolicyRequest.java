package com.careflow.insurance.dto;

import com.careflow.insurance.domain.PolicyRelationship;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request DTO to register an insurance policy for a patient (§33).
 */
public record CreateInsurancePolicyRequest(
        @NotBlank(message = "Policy number is required")
        String policyNumber,

        String groupNumber,

        @NotBlank(message = "Patient ID is required")
        String patientId,

        @NotBlank(message = "Provider ID is required")
        String providerId,

        @NotBlank(message = "Policy holder name is required")
        String policyHolderName,

        PolicyRelationship relationship,

        @NotNull(message = "Coverage start date is required")
        LocalDate coverageStartDate,

        @NotNull(message = "Coverage end date is required")
        LocalDate coverageEndDate,

        @DecimalMin(value = "0.00", message = "Co-pay amount cannot be negative")
        BigDecimal coPayAmount,

        @DecimalMin(value = "0.00", message = "Coverage percentage cannot be negative")
        @DecimalMax(value = "100.00", message = "Coverage percentage cannot exceed 100")
        BigDecimal coveragePercentage,

        @DecimalMin(value = "0.00", message = "Deductible cannot be negative")
        BigDecimal deductible
) {}
