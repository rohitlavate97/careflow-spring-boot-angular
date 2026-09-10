package com.careflow.patient.service;

import com.careflow.common.dto.PageResponse;
import com.careflow.patient.domain.Gender;
import com.careflow.patient.domain.Patient;
import com.careflow.patient.domain.PatientStatus;
import com.careflow.patient.dto.CreatePatientRequest;
import com.careflow.patient.dto.PatientResponse;
import com.careflow.patient.dto.PatientSummaryResponse;
import com.careflow.patient.dto.UpdatePatientRequest;
import com.careflow.patient.dto.UpdatePatientStatusRequest;
import com.careflow.patient.exception.InvalidPatientStatusTransitionException;
import com.careflow.patient.exception.PatientNotFoundException;
import com.careflow.patient.mapper.PatientMapper;
import com.careflow.patient.repository.PatientRepository;
import com.careflow.patient.repository.PatientSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Production implementation of PatientService (§16, §88).
 * Manages transactional boundaries, status transitions, and dynamic search criteria.
 */
@Service
@Transactional(readOnly = true)
public class PatientServiceImpl implements PatientService {

    private static final Logger log = LoggerFactory.getLogger(PatientServiceImpl.class);

    private final PatientRepository patientRepository;
    private final PatientMapper patientMapper;
    private final MrnGenerator mrnGenerator;

    public PatientServiceImpl(PatientRepository patientRepository,
                              PatientMapper patientMapper,
                              MrnGenerator mrnGenerator) {
        this.patientRepository = patientRepository;
        this.patientMapper = patientMapper;
        this.mrnGenerator = mrnGenerator;
    }

    @Override
    @Transactional
    public PatientResponse registerPatient(CreatePatientRequest request) {
        log.info("Registering new patient: {} {}", request.firstName(), request.lastName());

        String id = UUID.randomUUID().toString();
        String mrn = mrnGenerator.generateUniqueMrn();

        Patient patient = patientMapper.toEntity(id, mrn, request);
        Patient saved = patientRepository.save(patient);

        log.info("Patient successfully registered with id '{}' and MRN '{}'", saved.getId(), saved.getMrn());
        return patientMapper.toResponse(saved);
    }

    @Override
    public PatientResponse getPatientById(String id) {
        log.debug("Fetching patient with id '{}'", id);
        Patient patient = findPatientOrThrow(id);
        return patientMapper.toResponse(patient);
    }

    @Override
    public PatientResponse getPatientByMrn(String mrn) {
        log.debug("Fetching patient with MRN '{}'", mrn);
        Patient patient = patientRepository.findByMrn(mrn)
                .orElseThrow(() -> PatientNotFoundException.forMrn(mrn));
        return patientMapper.toResponse(patient);
    }

    @Override
    @Transactional
    public PatientResponse updatePatient(String id, UpdatePatientRequest request) {
        log.info("Updating patient details for id '{}'", id);
        Patient patient = findPatientOrThrow(id);

        if (patient.getStatus() == PatientStatus.DECEASED) {
            log.warn("Attempt to update demographic data for deceased patient with id '{}'", id);
            throw new InvalidPatientStatusTransitionException("Demographic updates are not permitted for deceased patients.");
        }

        patientMapper.updateEntity(patient, request);
        Patient updated = patientRepository.save(patient);

        log.info("Patient details updated for id '{}' (version={})", updated.getId(), updated.getVersion());
        return patientMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public PatientResponse updatePatientStatus(String id, UpdatePatientStatusRequest request) {
        log.info("Updating status for patient id '{}' to '{}'", id, request.status());
        Patient patient = findPatientOrThrow(id);

        PatientStatus currentStatus = patient.getStatus();
        PatientStatus targetStatus = request.status();

        if (currentStatus == PatientStatus.DECEASED && targetStatus != PatientStatus.DECEASED) {
            log.warn("Illegal status transition attempted from DECEASED to '{}' for patient id '{}'", targetStatus, id);
            throw new InvalidPatientStatusTransitionException(currentStatus, targetStatus);
        }

        if (targetStatus == PatientStatus.DECEASED) {
            patient.markDeceased();
        } else if (targetStatus == PatientStatus.INACTIVE) {
            patient.deactivate();
        } else if (targetStatus == PatientStatus.ACTIVE) {
            patient.activate();
        }

        Patient updated = patientRepository.save(patient);
        log.info("Patient id '{}' transitioned to status '{}' (reason: '{}')",
                id, targetStatus, request.reason() != null ? request.reason() : "N/A");

        return patientMapper.toResponse(updated);
    }

    @Override
    public PageResponse<PatientResponse> searchPatients(String query,
                                                        Gender gender,
                                                        PatientStatus status,
                                                        LocalDate dateOfBirth,
                                                        Pageable pageable) {
        log.debug("Searching patients with query='{}', gender={}, status={}, dob={}", query, gender, status, dateOfBirth);

        Specification<Patient> specification = PatientSpecification.withFilters(query, gender, status, dateOfBirth);
        Page<Patient> page = patientRepository.findAll(specification, pageable);

        return PageResponse.from(page, patientMapper::toResponse);
    }

    @Override
    public List<PatientSummaryResponse> quickSearch(String query, int limit) {
        log.debug("Quick searching patients with query='{}', limit={}", query, limit);
        int maxLimit = Math.clamp(limit, 1, 50);

        if (query == null || query.isBlank()) {
            return List.of();
        }

        PageRequest pageRequest = PageRequest.of(0, maxLimit, Sort.by("lastName").ascending().and(Sort.by("firstName").ascending()));
        Page<Patient> page = patientRepository.searchPatients(query.trim(), pageRequest);

        return page.getContent().stream()
                .map(patientMapper::toSummaryResponse)
                .toList();
    }

    private Patient findPatientOrThrow(String id) {
        return patientRepository.findById(id)
                .orElseThrow(() -> new PatientNotFoundException(id));
    }
}
