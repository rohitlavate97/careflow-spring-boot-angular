package com.careflow.laboratory.dto;

import com.careflow.laboratory.domain.LabOrderStatus;
import com.careflow.laboratory.domain.LabTestCategory;
import com.careflow.laboratory.domain.SpecimenType;

import java.math.BigDecimal;
import java.util.List;

/**
 * Detailed lab order line item with catalog metadata and recorded test results (§27).
 */
public record LabOrderItemResponse(
        String id,
        String labTestId,
        String testCode,
        String testName,
        LabTestCategory category,
        SpecimenType specimenType,
        BigDecimal price,
        LabOrderStatus status,
        String notes,
        List<LabResultResponse> results
) {
}
