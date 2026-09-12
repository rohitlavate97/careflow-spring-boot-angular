package com.careflow.patient.dto;

import com.careflow.patient.domain.AllergenCategory;
import com.careflow.patient.domain.AllergySeverity;
import com.careflow.patient.domain.AllergyStatus;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Response DTO exposing patient allergy details and risk assessment (§16, §40).
 */
public record PatientAllergyResponse(
        String id,
        String patientId,
        String allergen,
        AllergenCategory category,
        AllergySeverity severity,
        String reaction,
        AllergyStatus status,
        String notes,
        LocalDate diagnosedDate,
        boolean isHighRisk,
        Instant createdAt,
        Instant updatedAt,
        Long version
) {
}
