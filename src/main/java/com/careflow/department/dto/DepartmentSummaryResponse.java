package com.careflow.department.dto;

import com.careflow.department.domain.DepartmentStatus;

/**
 * Lightweight summary representation of a hospital department (§17, §40).
 */
public record DepartmentSummaryResponse(
        String id,
        String code,
        String name,
        String location,
        DepartmentStatus status
) {
}
