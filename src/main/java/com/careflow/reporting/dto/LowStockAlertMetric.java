package com.careflow.reporting.dto;

import java.time.LocalDate;

/**
 * Metric representing an inventory batch requiring clinical or procurement attention (§37, §103 Phase 16).
 */
public record LowStockAlertMetric(
        String medicationId,
        String batchNumber,
        int quantityAvailable,
        int reorderThreshold,
        LocalDate expiryDate
) {
}
