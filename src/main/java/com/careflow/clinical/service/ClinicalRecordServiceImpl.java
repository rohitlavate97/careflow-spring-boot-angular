package com.careflow.clinical.service;

import com.careflow.clinical.domain.ClinicalNote;
import com.careflow.clinical.dto.ClinicalNoteResponse;
import com.careflow.clinical.dto.CreateClinicalNoteRequest;
import com.careflow.clinical.mapper.ClinicalNoteMapper;
import com.careflow.clinical.repository.ClinicalNoteRepository;
import com.careflow.common.exception.BusinessRuleException;
import com.careflow.common.exception.ResourceNotFoundException;
import com.careflow.consultation.repository.ConsultationRepository;
import com.careflow.patient.repository.PatientRepository;
import com.careflow.staff.domain.StaffMember;
import com.careflow.staff.domain.StaffType;
import com.careflow.staff.exception.StaffNotFoundException;
import com.careflow.staff.repository.StaffMemberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Production implementation of ClinicalRecordService managing medical progress notes and longitudinal clinical history (§23, §103 Phase 7).
 */
@Service
public class ClinicalRecordServiceImpl implements ClinicalRecordService {

    private static final Logger log = LoggerFactory.getLogger(ClinicalRecordServiceImpl.class);

    private final ClinicalNoteRepository clinicalNoteRepository;
    private final ClinicalNoteMapper clinicalNoteMapper;
    private final PatientRepository patientRepository;
    private final StaffMemberRepository staffMemberRepository;
    private final ConsultationRepository consultationRepository;

    public ClinicalRecordServiceImpl(ClinicalNoteRepository clinicalNoteRepository,
                                     ClinicalNoteMapper clinicalNoteMapper,
                                     PatientRepository patientRepository,
                                     StaffMemberRepository staffMemberRepository,
                                     ConsultationRepository consultationRepository) {
        this.clinicalNoteRepository = clinicalNoteRepository;
        this.clinicalNoteMapper = clinicalNoteMapper;
        this.patientRepository = patientRepository;
        this.staffMemberRepository = staffMemberRepository;
        this.consultationRepository = consultationRepository;
    }

    @Override
    @Transactional
    public ClinicalNoteResponse createClinicalNote(CreateClinicalNoteRequest request) {
        log.info("Creating clinical note for patient id='{}', author id='{}', type='{}'",
                request.patientId(), request.authorId(), request.noteType());

        if (!patientRepository.existsById(request.patientId().trim())) {
            throw new ResourceNotFoundException("Patient", request.patientId());
        }

        StaffMember author = staffMemberRepository.findById(request.authorId().trim())
                .orElseThrow(() -> new StaffNotFoundException(request.authorId()));

        if (author.getStaffType() != StaffType.DOCTOR && author.getStaffType() != StaffType.NURSE) {
            throw new BusinessRuleException(
                    "UNAUTHORIZED_CLINICAL_AUTHOR",
                    String.format("Staff member '%s' of type '%s' is not authorized to author clinical notes.",
                            request.authorId(), author.getStaffType()),
                    HttpStatus.FORBIDDEN
            );
        }

        String consultationId = null;
        if (request.consultationId() != null && !request.consultationId().isBlank()) {
            consultationId = request.consultationId().trim();
            if (!consultationRepository.existsById(consultationId)) {
                throw new ResourceNotFoundException("Consultation", consultationId);
            }
        }

        String id = UUID.randomUUID().toString();
        ClinicalNote note = new ClinicalNote(
                id,
                request.patientId().trim(),
                consultationId,
                request.authorId().trim(),
                request.noteType(),
                request.title().trim(),
                request.content().trim()
        );

        ClinicalNote saved = clinicalNoteRepository.save(note);
        log.info("Clinical note id='{}' successfully recorded for patient id='{}'", saved.getId(), saved.getPatientId());
        return clinicalNoteMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ClinicalNoteResponse getClinicalNoteById(String id) {
        log.debug("Fetching clinical note id='{}'", id);
        ClinicalNote note = clinicalNoteRepository.findById(id.trim())
                .orElseThrow(() -> new ResourceNotFoundException("ClinicalNote", id.trim()));
        return clinicalNoteMapper.toResponse(note);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ClinicalNoteResponse> getNotesByPatient(String patientId, Pageable pageable) {
        if (!patientRepository.existsById(patientId.trim())) {
            throw new ResourceNotFoundException("Patient", patientId);
        }
        return clinicalNoteRepository.findByPatientIdOrderByCreatedAtDesc(patientId.trim(), pageable)
                .map(clinicalNoteMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClinicalNoteResponse> getNotesByConsultation(String consultationId) {
        if (!consultationRepository.existsById(consultationId.trim())) {
            throw new ResourceNotFoundException("Consultation", consultationId);
        }
        return clinicalNoteMapper.toResponseList(
                clinicalNoteRepository.findByConsultationIdOrderByCreatedAtAsc(consultationId.trim())
        );
    }
}
