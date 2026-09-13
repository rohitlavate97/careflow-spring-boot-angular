package com.careflow.scheduling.dto;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;

/**
 * Detailed representation of a doctor schedule rule (§18, §40).
 */
public record DoctorScheduleResponse(
        String id,
        String doctorId,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        LocalTime breakStartTime,
        LocalTime breakEndTime,
        int slotDurationMinutes,
        boolean isActive,
        Instant createdAt,
        Instant updatedAt,
        Long version
) {
}
