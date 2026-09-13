package com.careflow.laboratory.dto;

import com.careflow.laboratory.domain.LabOrderPriority;
import com.careflow.laboratory.domain.LabOrderStatus;
import com.careflow.laboratory.domain.LabReviewStatus;

import java.time.Instant;

/**
 * Lightweight summary of a diagnostic lab order for worklists and queue tables (§27).
 */
public record LabOrderSummaryResponse(
        String id,
        String orderNumber,
        String patientId,
        String orderingDoctorId,
        String encounterId,
        LabOrderPriority priority,
        LabOrderStatus status,
        LabReviewStatus reviewStatus,
        int itemCount,
        int sampleCount,
        Instant orderedAt,
        Instant completedAt
) {
}
