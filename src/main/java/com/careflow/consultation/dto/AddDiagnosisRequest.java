package com.careflow.consultation.dto;

import com.careflow.consultation.domain.DiagnosisType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Payload for adding a diagnosis to an ongoing consultation (§22).
 */
public record AddDiagnosisRequest(
        @NotBlank(message = "Diagnosis code is required (e.g. ICD-10)")
        @Size(max = 32, message = "Diagnosis code cannot exceed 32 characters")
        String diagnosisCode,

        @NotBlank(message = "Diagnosis name is required")
        @Size(max = 255, message = "Diagnosis name cannot exceed 255 characters")
        String diagnosisName,

        @NotNull(message = "Diagnosis type is required (e.g. PRIMARY, SECONDARY, PROVISIONAL, DIFFERENTIAL)")
        DiagnosisType diagnosisType,

        @Size(max = 30, message = "Severity cannot exceed 30 characters")
        String severity,

        String notes
) {
}
