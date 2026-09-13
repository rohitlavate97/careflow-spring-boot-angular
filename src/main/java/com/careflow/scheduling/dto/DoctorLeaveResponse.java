package com.careflow.scheduling.dto;

import com.careflow.scheduling.domain.LeaveStatus;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Detailed representation of a doctor leave request (§18, §40).
 */
public record DoctorLeaveResponse(
        String id,
        String doctorId,
        LocalDate startDate,
        LocalDate endDate,
        String reason,
        LeaveStatus status,
        Instant createdAt,
        Instant updatedAt,
        Long version
) {
}
