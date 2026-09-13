package com.careflow.prescription.service;

import com.careflow.prescription.dto.CreatePrescriptionRequest;
import com.careflow.prescription.dto.PrescriptionResponse;
import com.careflow.prescription.dto.PrescriptionSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service contract for Prescription issuance and lifecycle management (§24, §89, §103 Phase 8).
 */
public interface PrescriptionService {

    PrescriptionResponse issuePrescription(CreatePrescriptionRequest request);

    PrescriptionResponse getPrescriptionById(String id);

    Page<PrescriptionSummaryResponse> getPrescriptionsByPatient(String patientId, Pageable pageable);

    Page<PrescriptionSummaryResponse> getPrescriptionsByDoctor(String doctorId, Pageable pageable);

    Page<PrescriptionSummaryResponse> getPendingPrescriptions(Pageable pageable);

    PrescriptionResponse cancelPrescription(String id, String reason);
}
