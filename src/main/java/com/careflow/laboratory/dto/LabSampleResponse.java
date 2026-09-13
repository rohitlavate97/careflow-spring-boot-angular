package com.careflow.laboratory.dto;

import com.careflow.laboratory.domain.SampleStatus;
import com.careflow.laboratory.domain.SpecimenType;

import java.time.Instant;

/**
 * Accessioned biological specimen response DTO (§27).
 */
public record LabSampleResponse(
        String id,
        String sampleBarcode,
        SpecimenType specimenType,
        String collectedById,
        Instant collectedAt,
        String conditionNotes,
        SampleStatus status,
        String rejectionReason
) {
}
