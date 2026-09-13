package com.careflow.staff.dto;

import com.careflow.staff.domain.StaffStatus;
import com.careflow.staff.domain.StaffType;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Detailed representation of a hospital staff member (§17, §40).
 */
public record StaffResponse(
        String id,
        String staffCode,
        String userId,
        String departmentId,
        String firstName,
        String lastName,
        String fullName,
        String email,
        String phone,
        StaffType staffType,
        StaffStatus status,
        LocalDate dateOfJoining,
        DoctorProfileResponse doctorProfile,
        Instant createdAt,
        Instant updatedAt,
        Long version
) {
}
