package com.careflow.staff.dto;

import com.careflow.staff.domain.StaffStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Request payload for transitioning a staff member's lifecycle status (§17, §69).
 */
public record UpdateStaffStatusRequest(
        @NotNull(message = "Staff status is required")
        StaffStatus status
) {
}
