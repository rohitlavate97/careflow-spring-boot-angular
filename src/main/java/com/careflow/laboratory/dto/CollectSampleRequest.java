package com.careflow.laboratory.dto;

import com.careflow.laboratory.domain.SpecimenType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Specimen collection payload (§27).
 */
public record CollectSampleRequest(
        @NotNull(message = "Specimen type is required")
        SpecimenType specimenType,

        @Size(max = 500, message = "Condition notes must not exceed 500 characters")
        String conditionNotes,

        String collectorStaffId
) {
}
