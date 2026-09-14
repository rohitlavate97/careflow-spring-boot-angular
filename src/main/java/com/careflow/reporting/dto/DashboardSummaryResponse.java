package com.careflow.reporting.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * High-level executive operational dashboard payload (§37, §103 Phase 16).
 */
public record DashboardSummaryResponse(
        Instant generatedAt,
        long todayAppointments,
        long todayCompletedAppointments,
        double appointmentCancellationRate,
        long activeAdmissions,
        double bedOccupancyRate,
        long activeQueueWaiting,
        long pendingLabOrders,
        long pharmacyReorderAlerts,
        BigDecimal totalBilledToday,
        BigDecimal totalCollectedToday
) {
}
