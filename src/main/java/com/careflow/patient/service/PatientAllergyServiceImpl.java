package com.careflow.patient.service;

import com.careflow.common.exception.BusinessRuleException;
import com.careflow.patient.domain.AllergySeverity;
import com.careflow.patient.domain.AllergyStatus;
import com.careflow.patient.domain.Patient;
import com.careflow.patient.domain.PatientAllergy;
import com.careflow.patient.domain.PatientStatus;
import com.careflow.patient.dto.CreatePatientAllergyRequest;
import com.careflow.patient.dto.PatientAllergyResponse;
import com.careflow.patient.dto.UpdateAllergyStatusRequest;
import com.careflow.patient.dto.UpdatePatientAllergyRequest;
import com.careflow.patient.exception.AllergyNotFoundException;
import com.careflow.patient.exception.DuplicateAllergyException;
import com.careflow.patient.exception.PatientNotFoundException;
import com.careflow.patient.mapper.PatientAllergyMapper;
import com.careflow.patient.repository.PatientAllergyRepository;
import com.careflow.patient.repository.PatientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Implementation of PatientAllergyService enforcing clinical safety rules and concurrency controls (§13, §16, §88).
 */
@Service
public class PatientAllergyServiceImpl implements PatientAllergyService {

    private static final Logger log = LoggerFactory.getLogger(PatientAllergyServiceImpl.class);

    private final PatientRepository patientRepository;
    private final PatientAllergyRepository patientAllergyRepository;
    private final PatientAllergyMapper allergyMapper;

    public PatientAllergyServiceImpl(
            PatientRepository patientRepository,
            PatientAllergyRepository patientAllergyRepository,
            PatientAllergyMapper allergyMapper
    ) {
        this.patientRepository = patientRepository;
        this.patientAllergyRepository = patientAllergyRepository;
        this.allergyMapper = allergyMapper;
    }

    @Override
    @Transactional
    public PatientAllergyResponse recordAllergy(String patientId, CreatePatientAllergyRequest request) {
        log.info("Recording allergy '{}' for patient id '{}'", request.allergen(), patientId);

        Patient patient = findActivePatientOrThrow(patientId);

        String trimmedAllergen = request.allergen().trim();
        if (patientAllergyRepository.existsByPatientIdAndAllergenIgnoreCaseAndStatus(patientId, trimmedAllergen, AllergyStatus.ACTIVE)) {
            log.warn("Conflict: Active allergy already recorded for allergen '{}' on patient id '{}'", trimmedAllergen, patientId);
            throw new DuplicateAllergyException(trimmedAllergen);
        }

        String allergyId = UUID.randomUUID().toString();
        PatientAllergy allergy = allergyMapper.toEntity(allergyId, patient.getId(), request);

        PatientAllergy saved = patientAllergyRepository.save(allergy);

        if (saved.isHighRisk()) {
            log.warn("HIGH-RISK CLINICAL ALERT: Patient id '{}' recorded with {} allergy to '{}'",
                    patientId, saved.getSeverity(), saved.getAllergen());
        } else {
            log.info("Successfully recorded allergy id '{}' for patient id '{}'", saved.getId(), patientId);
        }

        return allergyMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PatientAllergyResponse> getPatientAllergies(String patientId, AllergyStatus status) {
        log.debug("Fetching allergies for patient id '{}' with status filter '{}'", patientId, status);
        if (!patientRepository.existsById(patientId)) {
            throw new PatientNotFoundException(patientId);
        }

        List<PatientAllergy> list;
        if (status != null) {
            list = patientAllergyRepository.findByPatientIdAndStatusOrderByCreatedAtDesc(patientId, status);
        } else {
            list = patientAllergyRepository.findByPatientIdOrderByCreatedAtDesc(patientId);
        }

        return allergyMapper.toResponseList(list);
    }

    @Override
    @Transactional(readOnly = true)
    public PatientAllergyResponse getAllergyById(String patientId, String allergyId) {
        log.debug("Fetching allergy id '{}' for patient id '{}'", allergyId, patientId);
        if (!patientRepository.existsById(patientId)) {
            throw new PatientNotFoundException(patientId);
        }

        PatientAllergy allergy = patientAllergyRepository.findByIdAndPatientId(allergyId, patientId)
                .orElseThrow(() -> new AllergyNotFoundException(allergyId));

        return allergyMapper.toResponse(allergy);
    }

    @Override
    @Transactional
    public PatientAllergyResponse updateAllergy(String patientId, String allergyId, UpdatePatientAllergyRequest request) {
        log.info("Updating allergy id '{}' for patient id '{}'", allergyId, patientId);
        findActivePatientOrThrow(patientId);

        PatientAllergy allergy = patientAllergyRepository.findByIdAndPatientId(allergyId, patientId)
                .orElseThrow(() -> new AllergyNotFoundException(allergyId));

        allergyMapper.updateEntity(allergy, request);
        PatientAllergy updated = patientAllergyRepository.save(allergy);

        log.info("Updated allergy id '{}' for patient id '{}'", updated.getId(), patientId);
        return allergyMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public PatientAllergyResponse updateAllergyStatus(String patientId, String allergyId, UpdateAllergyStatusRequest request) {
        log.info("Transitioning status of allergy id '{}' for patient id '{}' to '{}'", allergyId, patientId, request.status());
        findActivePatientOrThrow(patientId);

        PatientAllergy allergy = patientAllergyRepository.findByIdAndPatientId(allergyId, patientId)
                .orElseThrow(() -> new AllergyNotFoundException(allergyId));

        allergy.setStatus(request.status());
        if (request.notes() != null && !request.notes().isBlank()) {
            allergy.setNotes(request.notes().trim());
        }

        PatientAllergy updated = patientAllergyRepository.save(allergy);
        log.info("Transitioned allergy id '{}' to status '{}'", updated.getId(), updated.getStatus());
        return allergyMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public void removeAllergy(String patientId, String allergyId) {
        log.info("Deactivating allergy id '{}' for patient id '{}'", allergyId, patientId);
        findActivePatientOrThrow(patientId);

        PatientAllergy allergy = patientAllergyRepository.findByIdAndPatientId(allergyId, patientId)
                .orElseThrow(() -> new AllergyNotFoundException(allergyId));

        // Soft deactivation to preserve medical audit trail
        allergy.deactivate();
        patientAllergyRepository.save(allergy);
        log.info("Allergy id '{}' marked INACTIVE", allergyId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasHighRiskAllergies(String patientId) {
        long highRiskCount = patientAllergyRepository.countHighRiskAllergies(
                patientId,
                AllergyStatus.ACTIVE,
                List.of(AllergySeverity.SEVERE, AllergySeverity.LIFE_THREATENING)
        );
        return highRiskCount > 0;
    }

    private Patient findActivePatientOrThrow(String patientId) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new PatientNotFoundException(patientId));

        if (patient.getStatus() == PatientStatus.DECEASED) {
            log.warn("Operation rejected: patient id '{}' is DECEASED", patientId);
            throw new BusinessRuleException(
                    "INVALID_PATIENT_STATUS",
                    "Cannot modify allergies for a deceased patient",
                    HttpStatus.UNPROCESSABLE_ENTITY
            );
        }

        return patient;
    }
}
