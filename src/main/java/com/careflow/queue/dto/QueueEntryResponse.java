package com.careflow.queue.dto;

import com.careflow.queue.domain.QueuePriority;
import com.careflow.queue.domain.QueueStatus;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Detailed representation of a patient queue ticket (§21).
 */
public record QueueEntryResponse(
        String id,
        String departmentId,
        String doctorId,
        String patientId,
        String appointmentId,
        LocalDate queueDate,
        Integer tokenNumber,
        String tokenDisplay,
        QueuePriority priority,
        QueueStatus status,
        Instant entryTime,
        Instant calledTime,
        Instant consultationStartTime,
        Instant consultationEndTime,
        String notes,
        long patientsAhead,
        Instant createdAt,
        Instant updatedAt,
        Long version
) {
}
