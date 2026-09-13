package com.careflow.pharmacy.mapper;

import com.careflow.pharmacy.domain.DispenseRecord;
import com.careflow.pharmacy.domain.Medication;
import com.careflow.pharmacy.domain.PharmacyInventoryBatch;
import com.careflow.pharmacy.dto.CreateMedicationRequest;
import com.careflow.pharmacy.dto.DispenseRecordResponse;
import com.careflow.pharmacy.dto.MedicationResponse;
import com.careflow.pharmacy.dto.PharmacyInventoryBatchResponse;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Pure Java mapper for Pharmacy domain entities and DTOs (§25, §40, §90).
 */
@Component
public class PharmacyMapper {

    public Medication toEntity(String id, CreateMedicationRequest request) {
        if (request == null) {
            return null;
        }
        return new Medication(
                id,
                request.code(),
                request.name(),
                request.genericName(),
                request.form(),
                request.strength(),
                request.unitPrice(),
                request.reorderThreshold()
        );
    }

    public MedicationResponse toResponse(Medication medication) {
        if (medication == null) {
            return null;
        }
        return new MedicationResponse(
                medication.getId(),
                medication.getCode(),
                medication.getName(),
                medication.getGenericName(),
                medication.getForm(),
                medication.getStrength(),
                medication.getUnitPrice(),
                medication.getReorderThreshold(),
                medication.getStatus(),
                medication.getCreatedAt(),
                medication.getUpdatedAt()
        );
    }

    public List<MedicationResponse> toMedicationResponseList(Iterable<Medication> medications) {
        if (medications == null) {
            return Collections.emptyList();
        }
        return java.util.stream.StreamSupport.stream(medications.spliterator(), false)
                .map(this::toResponse)
                .toList();
    }

    public PharmacyInventoryBatchResponse toBatchResponse(PharmacyInventoryBatch batch) {
        if (batch == null) {
            return null;
        }
        return new PharmacyInventoryBatchResponse(
                batch.getId(),
                batch.getMedicationId(),
                batch.getBatchNumber(),
                batch.getExpiryDate(),
                batch.getQuantityAvailable(),
                batch.getReorderThreshold(),
                batch.isExpired(),
                batch.isLowStock(),
                batch.getCreatedAt()
        );
    }

    public List<PharmacyInventoryBatchResponse> toBatchResponseList(Iterable<PharmacyInventoryBatch> batches) {
        if (batches == null) {
            return Collections.emptyList();
        }
        return java.util.stream.StreamSupport.stream(batches.spliterator(), false)
                .map(this::toBatchResponse)
                .toList();
    }

    public DispenseRecordResponse toDispenseResponse(DispenseRecord record) {
        if (record == null) {
            return null;
        }
        return new DispenseRecordResponse(
                record.getId(),
                record.getPrescriptionId(),
                record.getPrescriptionItemId(),
                record.getInventoryBatchId(),
                record.getPharmacistId(),
                record.getQuantityDispensed(),
                record.getDispensedAt(),
                record.getNotes()
        );
    }

    public List<DispenseRecordResponse> toDispenseResponseList(Iterable<DispenseRecord> records) {
        if (records == null) {
            return Collections.emptyList();
        }
        return java.util.stream.StreamSupport.stream(records.spliterator(), false)
                .map(this::toDispenseResponse)
                .toList();
    }
}
