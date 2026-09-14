package com.careflow.reporting.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Report payload for diagnostic laboratory turnaround times and order volumes (§37, §103 Phase 16).
 */
public record LabReportResponse(
        LocalDate startDate,
        LocalDate endDate,
        long totalOrders,
        Map<String, Long> ordersByStatus,
        double averageTurnaroundHours,
        List<TopLabTestMetric> topTests
) {
}
