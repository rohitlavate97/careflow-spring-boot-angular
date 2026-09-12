package com.careflow.department.dto;

import com.careflow.department.domain.DepartmentStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Request payload for transitioning department operational status (§17, §69).
 */
public record UpdateDepartmentStatusRequest(
        @NotNull(message = "Department status is required")
        DepartmentStatus status
) {
}
