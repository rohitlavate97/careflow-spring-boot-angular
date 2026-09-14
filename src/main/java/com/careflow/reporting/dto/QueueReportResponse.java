package com.careflow.reporting.dto;

import java.time.LocalDate;
import java.util.Map;

/**
 * Report payload for outpatient queue throughput and wait times (§37, §103 Phase 16).
 */
public record QueueReportResponse(
        LocalDate queueDate,
        String departmentId,
        long totalQueued,
        long waitingCount,
        long inConsultationCount,
        long completedCount,
        long cancelledCount,
        Map<String, Long> priorityBreakdown,
        double averageWaitTimeMinutes
) {
}
