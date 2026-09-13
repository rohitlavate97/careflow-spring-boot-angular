package com.careflow.admission.dto;

import com.careflow.admission.domain.AdmissionStatus;

import java.time.Instant;

/**
 * Lightweight inpatient admission summary for census boards and worklists (§28).
 */
public record AdmissionSummaryResponse(
        String id,
        String admissionNumber,
        String patientId,
        String admittingDoctorId,
        String currentBedId,
        String currentBedNumber,
        String wardName,
        AdmissionStatus status,
        Instant admittedAt,
        Instant dischargedAt
) {
}
