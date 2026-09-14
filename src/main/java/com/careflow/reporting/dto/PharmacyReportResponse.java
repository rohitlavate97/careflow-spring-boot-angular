package com.careflow.reporting.dto;

import java.util.List;

/**
 * Report payload for pharmacy inventory valuation, stockouts, and expiry alerts (§37, §103 Phase 16).
 */
public record PharmacyReportResponse(
        long totalBatches,
        long totalStockQuantity,
        long expiredBatchesCount,
        long expiringWithin30DaysCount,
        long lowStockBatchesCount,
        long outOfStockBatchesCount,
        List<LowStockAlertMetric> criticalAlerts
) {
}
