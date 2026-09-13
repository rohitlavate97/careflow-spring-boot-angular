package com.careflow.prescription.dto;

import com.careflow.prescription.domain.PrescriptionStatus;

import java.time.Instant;
import java.util.List;

/**
 * Detailed prescription response representation (§24, §40).
 */
public record PrescriptionResponse(
        String id,
        String patientId,
        String doctorId,
        String consultationId,
        PrescriptionStatus status,
        String notes,
        Instant prescribedAt,
        Instant dispensedAt,
        List<PrescriptionItemResponse> items,
        Instant createdAt,
        Instant updatedAt,
        String createdBy,
        String updatedBy
) {
}
