package com.careflow.staff.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Request payload for creating or updating doctor-specific clinical credentials (§17, §40).
 */
public record DoctorProfileDto(
        @NotBlank(message = "Specialization is required")
        @Size(max = 100, message = "Specialization must not exceed 100 characters")
        String specialization,

        @NotBlank(message = "Qualifications are required")
        @Size(max = 255, message = "Qualifications must not exceed 255 characters")
        String qualifications,

        @NotBlank(message = "Medical license number is required")
        @Size(max = 100, message = "Medical license number must not exceed 100 characters")
        String medicalLicenseNumber,

        @NotNull(message = "Consultation fee is required")
        @PositiveOrZero(message = "Consultation fee must be zero or positive")
        BigDecimal consultationFee,

        @Size(max = 50, message = "Consultation room must not exceed 50 characters")
        String consultationRoom,

        @Size(max = 1000, message = "Bio must not exceed 1000 characters")
        String bio
) {
}
