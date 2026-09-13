package com.careflow.prescription.dto;

import com.careflow.prescription.domain.PrescriptionStatus;

import java.time.Instant;

/**
 * Summary DTO for prescription listings (§24, §71).
 */
public record PrescriptionSummaryResponse(
        String id,
        String patientId,
        String doctorId,
        String consultationId,
        PrescriptionStatus status,
        Instant prescribedAt,
        Instant dispensedAt,
        int totalItems,
        int dispensedItems
) {
}
