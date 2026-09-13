package com.careflow.queue.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Live status dashboard payload for a department queue (§21).
 */
public record DepartmentQueueLiveStatusResponse(
        String departmentId,
        LocalDate queueDate,
        long totalWaiting,
        long totalCalled,
        long totalInConsultation,
        List<QueueSummaryResponse> currentlyCalled,
        List<QueueSummaryResponse> waitingList
) {
}
