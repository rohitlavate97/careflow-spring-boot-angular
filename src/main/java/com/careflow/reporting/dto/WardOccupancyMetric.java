package com.careflow.reporting.dto;

/**
 * Metric representing inpatient bed capacity and occupancy per ward (§37, §103 Phase 16).
 */
public record WardOccupancyMetric(
        String wardId,
        String wardName,
        String wardType,
        long totalBeds,
        long occupiedBeds,
        double occupancyRate
) {
}
