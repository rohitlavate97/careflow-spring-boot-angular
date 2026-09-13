package com.careflow.laboratory.dto;

import com.careflow.laboratory.domain.LabTestCategory;
import com.careflow.laboratory.domain.SpecimenType;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Diagnostic lab test definition response DTO (§27).
 */
public record LabTestResponse(
        String id,
        String code,
        String name,
        LabTestCategory category,
        SpecimenType specimenType,
        String referenceRange,
        String unit,
        Integer turnaroundHours,
        BigDecimal price,
        boolean active,
        Instant createdAt
) {
}
