package com.careflow.prescription.service;

import com.careflow.common.exception.BusinessRuleException;
import com.careflow.common.exception.ResourceNotFoundException;
import com.careflow.consultation.repository.ConsultationRepository;
import com.careflow.patient.repository.PatientRepository;
import com.careflow.pharmacy.repository.MedicationRepository;
import com.careflow.prescription.domain.Prescription;
import com.careflow.prescription.domain.PrescriptionItem;
import com.careflow.prescription.domain.PrescriptionStatus;
import com.careflow.prescription.dto.CreatePrescriptionRequest;
import com.careflow.prescription.dto.PrescriptionItemRequest;
import com.careflow.prescription.dto.PrescriptionResponse;
import com.careflow.prescription.dto.PrescriptionSummaryResponse;
import com.careflow.prescription.exception.PrescriptionNotFoundException;
import com.careflow.prescription.mapper.PrescriptionMapper;
import com.careflow.prescription.repository.PrescriptionRepository;
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

import java.time.Instant;
import java.util.UUID;

/**
 * Production implementation of PrescriptionService managing physician medication orders (§24, §88, §103 Phase 8).
 */
@Service
public class PrescriptionServiceImpl implements PrescriptionService {

    private static final Logger log = LoggerFactory.getLogger(PrescriptionServiceImpl.class);

    private final PrescriptionRepository prescriptionRepository;
    private final PrescriptionMapper prescriptionMapper;
    private final PatientRepository patientRepository;
    private final StaffMemberRepository staffMemberRepository;
    private final ConsultationRepository consultationRepository;
    private final MedicationRepository medicationRepository;

    public PrescriptionServiceImpl(PrescriptionRepository prescriptionRepository,
                                   PrescriptionMapper prescriptionMapper,
                                   PatientRepository patientRepository,
                                   StaffMemberRepository staffMemberRepository,
                                   ConsultationRepository consultationRepository,
                                   MedicationRepository medicationRepository) {
        this.prescriptionRepository = prescriptionRepository;
        this.prescriptionMapper = prescriptionMapper;
        this.patientRepository = patientRepository;
        this.staffMemberRepository = staffMemberRepository;
        this.consultationRepository = consultationRepository;
        this.medicationRepository = medicationRepository;
    }

    @Override
    @Transactional
    public PrescriptionResponse issuePrescription(CreatePrescriptionRequest request) {
        log.info("Issuing prescription for patient id='{}', doctor id='{}'",
                request.patientId(), request.doctorId());

        validatePatient(request.patientId());
        validateDoctor(request.doctorId());

        String consultationId = null;
        if (request.consultationId() != null && !request.consultationId().isBlank()) {
            consultationId = request.consultationId().trim();
            if (!consultationRepository.existsById(consultationId)) {
                throw new ResourceNotFoundException("Consultation", consultationId);
            }
        }

        String prescriptionId = UUID.randomUUID().toString();
        Prescription prescription = new Prescription(
                prescriptionId,
                request.patientId().trim(),
                request.doctorId().trim(),
                consultationId,
                request.notes() != null ? request.notes().trim() : null,
                Instant.now()
        );

        for (PrescriptionItemRequest itemReq : request.items()) {
            if (!medicationRepository.existsById(itemReq.medicationId().trim())) {
                throw new ResourceNotFoundException("Medication", itemReq.medicationId());
            }

            PrescriptionItem item = new PrescriptionItem(
                    UUID.randomUUID().toString(),
                    prescription,
                    itemReq.medicationId().trim(),
                    itemReq.dosage().trim(),
                    itemReq.frequency().trim(),
                    itemReq.duration().trim(),
                    itemReq.quantityPrescribed(),
                    itemReq.instructions() != null ? itemReq.instructions().trim() : null
            );
            prescription.addItem(item);
        }

        Prescription saved = prescriptionRepository.save(prescription);
        log.info("Prescription id='{}' issued with {} medication order items", saved.getId(), saved.getItems().size());
        return prescriptionMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PrescriptionResponse getPrescriptionById(String id) {
        log.debug("Fetching prescription id='{}'", id);
        Prescription prescription = prescriptionRepository.findWithItemsById(id.trim())
                .orElseThrow(() -> new PrescriptionNotFoundException(id.trim()));
        return prescriptionMapper.toResponse(prescription);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PrescriptionSummaryResponse> getPrescriptionsByPatient(String patientId, Pageable pageable) {
        validatePatient(patientId);
        return prescriptionRepository.findByPatientIdOrderByPrescribedAtDesc(patientId.trim(), pageable)
                .map(prescriptionMapper::toSummaryResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PrescriptionSummaryResponse> getPrescriptionsByDoctor(String doctorId, Pageable pageable) {
        validateDoctor(doctorId);
        return prescriptionRepository.findByDoctorIdOrderByPrescribedAtDesc(doctorId.trim(), pageable)
                .map(prescriptionMapper::toSummaryResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PrescriptionSummaryResponse> getPendingPrescriptions(Pageable pageable) {
        return prescriptionRepository.findByStatusOrderByPrescribedAtDesc(PrescriptionStatus.PENDING_DISPENSE, pageable)
                .map(prescriptionMapper::toSummaryResponse);
    }

    @Override
    @Transactional
    public PrescriptionResponse cancelPrescription(String id, String reason) {
        log.info("Cancelling prescription id='{}', reason='{}'", id, reason);
        Prescription prescription = prescriptionRepository.findWithItemsById(id.trim())
                .orElseThrow(() -> new PrescriptionNotFoundException(id.trim()));

        prescription.transitionTo(PrescriptionStatus.CANCELLED);
        Prescription saved = prescriptionRepository.save(prescription);
        return prescriptionMapper.toResponse(saved);
    }

    private void validatePatient(String patientId) {
        if (!patientRepository.existsById(patientId.trim())) {
            throw new ResourceNotFoundException("Patient", patientId);
        }
    }

    private void validateDoctor(String doctorId) {
        StaffMember staff = staffMemberRepository.findById(doctorId.trim())
                .orElseThrow(() -> new StaffNotFoundException(doctorId));

        if (staff.getStaffType() != StaffType.DOCTOR) {
            throw new BusinessRuleException(
                    "INVALID_STAFF_TYPE",
                    String.format("Staff member '%s' is of type '%s', but only DOCTOR can prescribe medications.",
                            doctorId, staff.getStaffType()),
                    HttpStatus.BAD_REQUEST
            );
        }
    }
}
