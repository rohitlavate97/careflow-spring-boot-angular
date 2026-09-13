package com.careflow.scheduling.dto;

import com.careflow.scheduling.domain.LeaveStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Request payload for approving, rejecting, or cancelling a doctor leave request (§18, §69).
 */
public record UpdateDoctorLeaveStatusRequest(
        @NotNull(message = "Leave status is required")
        LeaveStatus status
) {
}
