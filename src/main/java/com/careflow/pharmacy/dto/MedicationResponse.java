package com.careflow.pharmacy.dto;

import com.careflow.pharmacy.domain.MedicationForm;
import com.careflow.pharmacy.domain.MedicationStatus;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Response representation for a medication (§25, §40).
 */
public record MedicationResponse(
        String id,
        String code,
        String name,
        String genericName,
        MedicationForm form,
        String strength,
        BigDecimal unitPrice,
        Integer reorderThreshold,
        MedicationStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
