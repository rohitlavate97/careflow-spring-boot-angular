package com.careflow.consultation.service;

import com.careflow.consultation.dto.AddDiagnosisRequest;
import com.careflow.consultation.dto.ConsultationResponse;
import com.careflow.consultation.dto.ConsultationSummaryResponse;
import com.careflow.consultation.dto.CreateConsultationRequest;
import com.careflow.consultation.dto.RecordVitalsRequest;
import com.careflow.consultation.dto.UpdateConsultationRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service contract for patient consultation encounters (§22, §89, §103 Phase 7).
 */
public interface ConsultationService {

    ConsultationResponse startConsultation(CreateConsultationRequest request);

    ConsultationResponse getConsultationById(String id);

    ConsultationResponse updateFindings(String id, UpdateConsultationRequest request);

    ConsultationResponse recordVitals(String id, RecordVitalsRequest request);

    ConsultationResponse addDiagnosis(String id, AddDiagnosisRequest request);

    ConsultationResponse removeDiagnosis(String id, String diagnosisId);

    ConsultationResponse completeConsultation(String id);

    ConsultationResponse cancelConsultation(String id, String reason);

    Page<ConsultationSummaryResponse> getConsultationsByPatient(String patientId, Pageable pageable);

    Page<ConsultationSummaryResponse> getConsultationsByDoctor(String doctorId, Pageable pageable);
}
