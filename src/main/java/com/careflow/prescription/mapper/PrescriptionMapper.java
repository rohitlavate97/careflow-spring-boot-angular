package com.careflow.prescription.mapper;

import com.careflow.prescription.domain.Prescription;
import com.careflow.prescription.domain.PrescriptionItem;
import com.careflow.prescription.domain.PrescriptionItemStatus;
import com.careflow.prescription.dto.PrescriptionItemResponse;
import com.careflow.prescription.dto.PrescriptionResponse;
import com.careflow.prescription.dto.PrescriptionSummaryResponse;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Pure Java mapper for Prescription domain entities and DTOs (§24, §40, §90).
 */
@Component
public class PrescriptionMapper {

    public PrescriptionItemResponse toItemResponse(PrescriptionItem item) {
        if (item == null) {
            return null;
        }
        return new PrescriptionItemResponse(
                item.getId(),
                item.getMedicationId(),
                item.getDosage(),
                item.getFrequency(),
                item.getDuration(),
                item.getQuantityPrescribed(),
                item.getQuantityDispensed(),
                item.getInstructions(),
                item.getStatus()
        );
    }

    public List<PrescriptionItemResponse> toItemResponseList(Iterable<PrescriptionItem> items) {
        if (items == null) {
            return Collections.emptyList();
        }
        return java.util.stream.StreamSupport.stream(items.spliterator(), false)
                .map(this::toItemResponse)
                .toList();
    }

    public PrescriptionResponse toResponse(Prescription prescription) {
        if (prescription == null) {
            return null;
        }

        return new PrescriptionResponse(
                prescription.getId(),
                prescription.getPatientId(),
                prescription.getDoctorId(),
                prescription.getConsultationId(),
                prescription.getStatus(),
                prescription.getNotes(),
                prescription.getPrescribedAt(),
                prescription.getDispensedAt(),
                toItemResponseList(prescription.getItems()),
                prescription.getCreatedAt(),
                prescription.getUpdatedAt(),
                prescription.getCreatedBy(),
                prescription.getUpdatedBy()
        );
    }

    public PrescriptionSummaryResponse toSummaryResponse(Prescription prescription) {
        if (prescription == null) {
            return null;
        }

        int total = prescription.getItems() != null ? prescription.getItems().size() : 0;
        int dispensed = 0;
        if (prescription.getItems() != null) {
            dispensed = (int) prescription.getItems().stream()
                    .filter(i -> i.getStatus() == PrescriptionItemStatus.DISPENSED)
                    .count();
        }

        return new PrescriptionSummaryResponse(
                prescription.getId(),
                prescription.getPatientId(),
                prescription.getDoctorId(),
                prescription.getConsultationId(),
                prescription.getStatus(),
                prescription.getPrescribedAt(),
                prescription.getDispensedAt(),
                total,
                dispensed
        );
    }
}
