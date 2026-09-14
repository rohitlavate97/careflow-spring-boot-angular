package com.careflow.reporting.dto;

import java.util.List;

/**
 * Report payload for inpatient admissions and ward bed occupancy (§37, §103 Phase 16).
 */
public record InpatientReportResponse(
        long totalAdmissions,
        long activeAdmissions,
        long dischargedCount,
        long transferredCount,
        long totalBeds,
        long occupiedBeds,
        long availableBeds,
        double overallOccupancyRate,
        List<WardOccupancyMetric> wardOccupancy
) {
}
