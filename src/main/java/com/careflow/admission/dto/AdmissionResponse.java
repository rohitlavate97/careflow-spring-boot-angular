package com.careflow.admission.dto;

import com.careflow.admission.domain.AdmissionStatus;

import java.time.Instant;
import java.util.List;

/**
 * Detailed inpatient admission response DTO (§28).
 */
public record AdmissionResponse(
        String id,
        String admissionNumber,
        String patientId,
        String admittingDoctorId,
        String currentBedId,
        String currentBedNumber,
        String roomNumber,
        String wardName,
        String encounterId,
        AdmissionStatus status,
        String admissionReason,
        String admittingDiagnosis,
        Instant admittedAt,
        Instant dischargedAt,
        String dischargeSummary,
        List<BedTransferResponse> transfers
) {
}
