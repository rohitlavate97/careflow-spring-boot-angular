package com.careflow.department.dto;

import com.careflow.department.domain.DepartmentStatus;

import java.time.Instant;

/**
 * Detailed response representation of a hospital department (§17, §40).
 */
public record DepartmentResponse(
        String id,
        String code,
        String name,
        String description,
        String contactPhone,
        String contactEmail,
        String location,
        String headOfDepartmentId,
        DepartmentStatus status,
        Instant createdAt,
        Instant updatedAt,
        Long version
) {
}
