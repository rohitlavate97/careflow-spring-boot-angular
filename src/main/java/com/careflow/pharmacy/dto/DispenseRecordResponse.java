package com.careflow.pharmacy.dto;

import java.time.Instant;

/**
 * Response representation for a pharmacy dispense record (§25).
 */
public record DispenseRecordResponse(
        String id,
        String prescriptionId,
        String prescriptionItemId,
        String inventoryBatchId,
        String pharmacistId,
        Integer quantityDispensed,
        Instant dispensedAt,
        String notes
) {
}
