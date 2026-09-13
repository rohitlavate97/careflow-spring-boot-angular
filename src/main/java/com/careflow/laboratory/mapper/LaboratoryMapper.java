package com.careflow.laboratory.mapper;

import com.careflow.laboratory.domain.LabOrder;
import com.careflow.laboratory.domain.LabOrderItem;
import com.careflow.laboratory.domain.LabResult;
import com.careflow.laboratory.domain.LabSample;
import com.careflow.laboratory.domain.LabTest;
import com.careflow.laboratory.dto.LabOrderItemResponse;
import com.careflow.laboratory.dto.LabOrderResponse;
import com.careflow.laboratory.dto.LabOrderSummaryResponse;
import com.careflow.laboratory.dto.LabResultResponse;
import com.careflow.laboratory.dto.LabSampleResponse;
import com.careflow.laboratory.dto.LabTestResponse;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper converting between Laboratory domain entities and DTO representations (§27).
 */
@Component
public class LaboratoryMapper {

    public LabTestResponse toLabTestResponse(LabTest test) {
        if (test == null) {
            return null;
        }
        return new LabTestResponse(
                test.getId(),
                test.getCode(),
                test.getName(),
                test.getCategory(),
                test.getSpecimenType(),
                test.getReferenceRange(),
                test.getUnit(),
                test.getTurnaroundHours(),
                test.getPrice(),
                test.isActive(),
                test.getCreatedAt()
        );
    }

    public LabOrderResponse toLabOrderResponse(LabOrder order) {
        if (order == null) {
            return null;
        }
        List<LabOrderItemResponse> items = order.getItems() != null
                ? order.getItems().stream().map(this::toLabOrderItemResponse).collect(Collectors.toList())
                : Collections.emptyList();

        List<LabSampleResponse> samples = order.getSamples() != null
                ? order.getSamples().stream().map(this::toLabSampleResponse).collect(Collectors.toList())
                : Collections.emptyList();

        return new LabOrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getPatientId(),
                order.getOrderingDoctorId(),
                order.getEncounterId(),
                order.getPriority(),
                order.getStatus(),
                order.getReviewStatus(),
                order.getClinicalNotes(),
                order.getCancellationReason(),
                order.getOrderedAt(),
                order.getCompletedAt(),
                order.getReviewedById(),
                order.getReviewedAt(),
                order.getReviewNotes(),
                items,
                samples
        );
    }

    public LabOrderSummaryResponse toLabOrderSummaryResponse(LabOrder order) {
        if (order == null) {
            return null;
        }
        int itemCount = order.getItems() != null ? order.getItems().size() : 0;
        int sampleCount = order.getSamples() != null ? order.getSamples().size() : 0;

        return new LabOrderSummaryResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getPatientId(),
                order.getOrderingDoctorId(),
                order.getEncounterId(),
                order.getPriority(),
                order.getStatus(),
                order.getReviewStatus(),
                itemCount,
                sampleCount,
                order.getOrderedAt(),
                order.getCompletedAt()
        );
    }

    public LabOrderItemResponse toLabOrderItemResponse(LabOrderItem item) {
        if (item == null) {
            return null;
        }
        LabTest test = item.getLabTest();
        List<LabResultResponse> results = item.getResults() != null
                ? item.getResults().stream().map(this::toLabResultResponse).collect(Collectors.toList())
                : Collections.emptyList();

        return new LabOrderItemResponse(
                item.getId(),
                test != null ? test.getId() : null,
                test != null ? test.getCode() : null,
                test != null ? test.getName() : null,
                test != null ? test.getCategory() : null,
                test != null ? test.getSpecimenType() : null,
                test != null ? test.getPrice() : null,
                item.getStatus(),
                item.getNotes(),
                results
        );
    }

    public LabSampleResponse toLabSampleResponse(LabSample sample) {
        if (sample == null) {
            return null;
        }
        return new LabSampleResponse(
                sample.getId(),
                sample.getSampleBarcode(),
                sample.getSpecimenType(),
                sample.getCollectedById(),
                sample.getCollectedAt(),
                sample.getConditionNotes(),
                sample.getStatus(),
                sample.getRejectionReason()
        );
    }

    public LabResultResponse toLabResultResponse(LabResult result) {
        if (result == null) {
            return null;
        }
        return new LabResultResponse(
                result.getId(),
                result.getOrderItem() != null ? result.getOrderItem().getId() : null,
                result.getSample() != null ? result.getSample().getId() : null,
                result.getTestParameter(),
                result.getResultValue(),
                result.getNumericValue(),
                result.getUnit(),
                result.getReferenceRange(),
                result.getAbnormalityFlag(),
                result.getPerformedById(),
                result.getPerformedAt(),
                result.getTechnicianNotes()
        );
    }
}
