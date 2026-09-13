package com.careflow.laboratory.dto;

import com.careflow.laboratory.domain.LabOrderPriority;
import com.careflow.laboratory.domain.LabOrderStatus;
import com.careflow.laboratory.domain.LabReviewStatus;

import java.time.Instant;
import java.util.List;

/**
 * Detailed diagnostic lab order response DTO (§27).
 */
public record LabOrderResponse(
        String id,
        String orderNumber,
        String patientId,
        String orderingDoctorId,
        String encounterId,
        LabOrderPriority priority,
        LabOrderStatus status,
        LabReviewStatus reviewStatus,
        String clinicalNotes,
        String cancellationReason,
        Instant orderedAt,
        Instant completedAt,
        String reviewedById,
        Instant reviewedAt,
        String reviewNotes,
        List<LabOrderItemResponse> items,
        List<LabSampleResponse> samples
) {
}
