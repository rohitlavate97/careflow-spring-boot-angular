package com.careflow.staff.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Detailed representation of a doctor's clinical profile (§17, §40).
 */
public record DoctorProfileResponse(
        String id,
        String specialization,
        String qualifications,
        String medicalLicenseNumber,
        BigDecimal consultationFee,
        String consultationRoom,
        String bio,
        Instant createdAt,
        Instant updatedAt,
        Long version
) {
}
