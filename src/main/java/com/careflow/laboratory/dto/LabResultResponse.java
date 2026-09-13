package com.careflow.laboratory.dto;

import com.careflow.laboratory.domain.AbnormalityFlag;

import java.time.Instant;

/**
 * Diagnostic lab observation result DTO (§27).
 */
public record LabResultResponse(
        String id,
        String orderItemId,
        String sampleId,
        String testParameter,
        String resultValue,
        Double numericValue,
        String unit,
        String referenceRange,
        AbnormalityFlag abnormalityFlag,
        String performedById,
        Instant performedAt,
        String technicianNotes
) {
}
