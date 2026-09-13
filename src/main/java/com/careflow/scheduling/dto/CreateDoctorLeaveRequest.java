package com.careflow.scheduling.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Request payload for submitting a doctor leave or absence request (§18, §40).
 */
public record CreateDoctorLeaveRequest(
        @NotNull(message = "Start date is required")
        @FutureOrPresent(message = "Leave start date cannot be in the past")
        LocalDate startDate,

        @NotNull(message = "End date is required")
        @FutureOrPresent(message = "Leave end date cannot be in the past")
        LocalDate endDate,

        @Size(max = 255, message = "Reason must not exceed 255 characters")
        String reason
) {
}
