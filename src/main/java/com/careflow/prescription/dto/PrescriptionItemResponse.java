package com.careflow.prescription.dto;

import com.careflow.prescription.domain.PrescriptionItemStatus;

/**
 * Response representation for a prescription medication order item (§24).
 */
public record PrescriptionItemResponse(
        String id,
        String medicationId,
        String dosage,
        String frequency,
        String duration,
        Integer quantityPrescribed,
        Integer quantityDispensed,
        String instructions,
        PrescriptionItemStatus status
) {
}
