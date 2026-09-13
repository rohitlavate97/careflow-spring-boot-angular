package com.careflow.pharmacy.dto;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Response representation for a pharmacy inventory batch (§25).
 */
public record PharmacyInventoryBatchResponse(
        String id,
        String medicationId,
        String batchNumber,
        LocalDate expiryDate,
        Integer quantityAvailable,
        Integer reorderThreshold,
        boolean isExpired,
        boolean isLowStock,
        Instant createdAt
) {
}
