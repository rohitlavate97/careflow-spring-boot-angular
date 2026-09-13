package com.careflow.staff.dto;

import com.careflow.staff.domain.StaffStatus;
import com.careflow.staff.domain.StaffType;

/**
 * Lightweight representation of a staff member for lists, table views, and selectors (§17, §40).
 */
public record StaffSummaryResponse(
        String id,
        String staffCode,
        String fullName,
        String departmentId,
        StaffType staffType,
        StaffStatus status,
        String email,
        String phone
) {
}
