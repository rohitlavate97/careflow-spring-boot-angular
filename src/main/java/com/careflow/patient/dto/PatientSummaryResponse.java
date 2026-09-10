package com.careflow.patient.dto;

import com.careflow.patient.domain.Gender;
import com.careflow.patient.domain.PatientStatus;

import java.time.LocalDate;

/**
 * Compact projection for patient search dropdowns and summary lists (§16, §111).
 */
public record PatientSummaryResponse(
        String id,
        String mrn,
        String fullName,
        LocalDate dateOfBirth,
        Gender gender,
        String phone,
        PatientStatus status
) {
}
