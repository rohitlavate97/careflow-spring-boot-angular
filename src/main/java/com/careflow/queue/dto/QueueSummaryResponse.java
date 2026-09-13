package com.careflow.queue.dto;

import com.careflow.queue.domain.QueuePriority;
import com.careflow.queue.domain.QueueStatus;

import java.time.Instant;

/**
 * Lightweight summary projection of a queue entry for live monitor displays (§21).
 */
public record QueueSummaryResponse(
        String id,
        String tokenDisplay,
        String patientId,
        String doctorId,
        QueuePriority priority,
        QueueStatus status,
        Instant entryTime,
        Instant calledTime
) {
}
