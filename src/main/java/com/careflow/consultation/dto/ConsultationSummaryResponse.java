package com.careflow.consultation.dto;

import com.careflow.consultation.domain.ConsultationStatus;

import java.time.Instant;

/**
 * Summary DTO for consultation encounter listings and patient visit history (§22, §71).
 */
public record ConsultationSummaryResponse(
        String id,
        String patientId,
        String doctorId,
        String appointmentId,
        String queueEntryId,
        ConsultationStatus status,
        Instant startedAt,
        Instant completedAt,
        String chiefComplaint,
        String primaryDiagnosisCode,
        String primaryDiagnosisName
) {
}
