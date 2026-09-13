package com.careflow.staff.dto;

import java.math.BigDecimal;

/**
 * Lightweight representation of a doctor for lookups, scheduling, and directory listings (§17, §18).
 */
public record DoctorSummaryResponse(
        String staffId,
        String staffCode,
        String doctorName,
        String departmentId,
        String specialization,
        String medicalLicenseNumber,
        BigDecimal consultationFee,
        String consultationRoom
) {
}
