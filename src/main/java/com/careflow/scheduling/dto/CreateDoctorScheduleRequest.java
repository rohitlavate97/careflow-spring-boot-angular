package com.careflow.scheduling.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * Request payload for creating a doctor's weekly recurring schedule template (§18, §40).
 */
public record CreateDoctorScheduleRequest(
        @NotNull(message = "Day of week is required")
        DayOfWeek dayOfWeek,

        @NotNull(message = "Start time is required")
        LocalTime startTime,

        @NotNull(message = "End time is required")
        LocalTime endTime,

        LocalTime breakStartTime,

        LocalTime breakEndTime,

        @NotNull(message = "Slot duration is required")
        @Min(value = 5, message = "Slot duration must be at least 5 minutes")
        @Max(value = 240, message = "Slot duration must not exceed 240 minutes")
        Integer slotDurationMinutes
) {
}
