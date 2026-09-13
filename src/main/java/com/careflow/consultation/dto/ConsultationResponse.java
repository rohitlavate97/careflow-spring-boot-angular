package com.careflow.consultation.dto;

import com.careflow.consultation.domain.ConsultationStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Detailed consultation encounter response DTO (§22, §40).
 */
public record ConsultationResponse(
        String id,
        String patientId,
        String doctorId,
        String appointmentId,
        String queueEntryId,
        ConsultationStatus status,
        Instant startedAt,
        Instant completedAt,
        String chiefComplaint,
        String historyOfPresentIllness,
        String physicalExamination,
        String treatmentPlan,
        LocalDate followUpDate,
        String followUpInstructions,
        ConsultationVitalsResponse vitals,
        List<ConsultationDiagnosisResponse> diagnoses,
        Instant createdAt,
        Instant updatedAt,
        String createdBy,
        String updatedBy
) {
}
