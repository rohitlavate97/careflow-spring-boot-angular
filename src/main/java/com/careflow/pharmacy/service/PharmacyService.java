package com.careflow.pharmacy.service;

import com.careflow.pharmacy.domain.MedicationStatus;
import com.careflow.pharmacy.dto.AddInventoryBatchRequest;
import com.careflow.pharmacy.dto.CreateMedicationRequest;
import com.careflow.pharmacy.dto.DispenseMedicationRequest;
import com.careflow.pharmacy.dto.DispenseRecordResponse;
import com.careflow.pharmacy.dto.MedicationResponse;
import com.careflow.pharmacy.dto.PharmacyInventoryBatchResponse;
import com.careflow.pharmacy.dto.UpdateMedicationRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Service contract for Pharmacy operations, formulary catalog, and concurrency-safe dispensing (§25, §26, §103 Phase 8).
 */
public interface PharmacyService {

    MedicationResponse registerMedication(CreateMedicationRequest request);

    MedicationResponse updateMedication(String id, UpdateMedicationRequest request);

    MedicationResponse getMedicationById(String id);

    Page<MedicationResponse> searchMedications(String query, MedicationStatus status, Pageable pageable);

    PharmacyInventoryBatchResponse addInventoryBatch(AddInventoryBatchRequest request);

    List<PharmacyInventoryBatchResponse> getBatchesForMedication(String medicationId);

    DispenseRecordResponse dispenseMedication(DispenseMedicationRequest request);

    List<DispenseRecordResponse> getDispensesForPrescription(String prescriptionId);
}
