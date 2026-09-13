package com.careflow.clinical.service;

import com.careflow.clinical.dto.ClinicalNoteResponse;
import com.careflow.clinical.dto.CreateClinicalNoteRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Service contract for patient clinical records and medical documentation (§23, §103 Phase 7).
 */
public interface ClinicalRecordService {

    ClinicalNoteResponse createClinicalNote(CreateClinicalNoteRequest request);

    ClinicalNoteResponse getClinicalNoteById(String id);

    Page<ClinicalNoteResponse> getNotesByPatient(String patientId, Pageable pageable);

    List<ClinicalNoteResponse> getNotesByConsultation(String consultationId);
}
