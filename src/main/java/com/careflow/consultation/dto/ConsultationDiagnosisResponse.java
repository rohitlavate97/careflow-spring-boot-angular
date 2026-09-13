package com.careflow.consultation.dto;

import com.careflow.consultation.domain.DiagnosisType;

import java.time.Instant;

/**
 * Response representation for a clinical diagnosis (§22).
 */
public record ConsultationDiagnosisResponse(
        String id,
        String diagnosisCode,
        String diagnosisName,
        DiagnosisType diagnosisType,
        String severity,
        String notes,
        Instant createdAt,
        String createdBy
) {
}
