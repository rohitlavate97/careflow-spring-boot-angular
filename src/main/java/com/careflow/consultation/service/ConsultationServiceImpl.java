package com.careflow.consultation.service;

import com.careflow.appointment.domain.Appointment;
import com.careflow.appointment.domain.AppointmentStatus;
import com.careflow.appointment.repository.AppointmentRepository;
import com.careflow.appointment.service.AppointmentService;
import com.careflow.common.exception.BusinessRuleException;
import com.careflow.common.exception.ResourceNotFoundException;
import com.careflow.consultation.domain.Consultation;
import com.careflow.consultation.domain.ConsultationDiagnosis;
import com.careflow.consultation.domain.ConsultationStatus;
import com.careflow.consultation.domain.ConsultationVitals;
import com.careflow.consultation.dto.AddDiagnosisRequest;
import com.careflow.consultation.dto.ConsultationResponse;
import com.careflow.consultation.dto.ConsultationSummaryResponse;
import com.careflow.consultation.dto.CreateConsultationRequest;
import com.careflow.consultation.dto.RecordVitalsRequest;
import com.careflow.consultation.dto.UpdateConsultationRequest;
import com.careflow.consultation.exception.ConsultationNotFoundException;
import com.careflow.consultation.mapper.ConsultationMapper;
import com.careflow.consultation.repository.ConsultationRepository;
import com.careflow.patient.repository.PatientRepository;
import com.careflow.queue.domain.QueueEntry;
import com.careflow.queue.domain.QueueStatus;
import com.careflow.queue.repository.QueueEntryRepository;
import com.careflow.queue.service.QueueService;
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
 * Production implementation of ConsultationService managing clinical encounters,
 * vitals, diagnoses, state transitions, and downstream operational synchronization (§22, §88, §103 Phase 7).
 */
@Service
public class ConsultationServiceImpl implements ConsultationService {

    private static final Logger log = LoggerFactory.getLogger(ConsultationServiceImpl.class);

    private final ConsultationRepository consultationRepository;
    private final ConsultationMapper consultationMapper;
    private final PatientRepository patientRepository;
    private final StaffMemberRepository staffMemberRepository;
    private final AppointmentRepository appointmentRepository;
    private final AppointmentService appointmentService;
    private final QueueEntryRepository queueEntryRepository;
    private final QueueService queueService;

    public ConsultationServiceImpl(ConsultationRepository consultationRepository,
                                   ConsultationMapper consultationMapper,
                                   PatientRepository patientRepository,
                                   StaffMemberRepository staffMemberRepository,
                                   AppointmentRepository appointmentRepository,
                                   AppointmentService appointmentService,
                                   QueueEntryRepository queueEntryRepository,
                                   QueueService queueService) {
        this.consultationRepository = consultationRepository;
        this.consultationMapper = consultationMapper;
        this.patientRepository = patientRepository;
        this.staffMemberRepository = staffMemberRepository;
        this.appointmentRepository = appointmentRepository;
        this.appointmentService = appointmentService;
        this.queueEntryRepository = queueEntryRepository;
        this.queueService = queueService;
    }

    @Override
    @Transactional
    public ConsultationResponse startConsultation(CreateConsultationRequest request) {
        log.info("Initiating consultation for patient id='{}', doctor id='{}'",
                request.patientId(), request.doctorId());

        validatePatient(request.patientId());
        validateDoctor(request.doctorId());

        String appointmentId = (request.appointmentId() != null && !request.appointmentId().isBlank())
                ? request.appointmentId().trim()
                : null;
        String queueEntryId = (request.queueEntryId() != null && !request.queueEntryId().isBlank())
                ? request.queueEntryId().trim()
                : null;

        // Synchronize appointment if provided
        if (appointmentId != null) {
            Appointment appointment = appointmentRepository.findById(appointmentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Appointment", appointmentId));
            if (appointment.getStatus() == AppointmentStatus.CHECKED_IN) {
                appointmentService.startConsultation(appointmentId);
            }
        }

        // Synchronize queue entry if provided
        if (queueEntryId != null) {
            QueueEntry queueEntry = queueEntryRepository.findById(queueEntryId)
                    .orElseThrow(() -> new ResourceNotFoundException("QueueEntry", queueEntryId));
            if (queueEntry.getStatus() == QueueStatus.CALLED) {
                queueService.startConsultation(queueEntryId);
            }
        }

        String id = UUID.randomUUID().toString();
        Consultation consultation = new Consultation(
                id,
                request.patientId().trim(),
                request.doctorId().trim(),
                appointmentId,
                queueEntryId,
                Instant.now()
        );

        if (request.chiefComplaint() != null && !request.chiefComplaint().isBlank()) {
            consultation.setChiefComplaint(request.chiefComplaint().trim());
        }

        Consultation saved = consultationRepository.save(consultation);
        log.info("Consultation encounter id='{}' initiated with status '{}'", saved.getId(), saved.getStatus());
        return consultationMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ConsultationResponse getConsultationById(String id) {
        log.debug("Fetching consultation encounter id='{}'", id);
        Consultation consultation = consultationRepository.findWithDiagnosesById(id.trim())
                .orElseThrow(() -> new ConsultationNotFoundException(id.trim()));
        return consultationMapper.toResponse(consultation);
    }

    @Override
    @Transactional
    public ConsultationResponse updateFindings(String id, UpdateConsultationRequest request) {
        log.info("Updating clinical evaluation findings for consultation id='{}'", id);
        Consultation consultation = findEntityWithDiagnosesOrThrow(id);

        consultation.updateClinicalFindings(
                request.chiefComplaint(),
                request.historyOfPresentIllness(),
                request.physicalExamination(),
                request.treatmentPlan(),
                request.followUpDate(),
                request.followUpInstructions()
        );

        if (consultation.getStatus() == ConsultationStatus.STARTED) {
            consultation.transitionTo(ConsultationStatus.IN_PROGRESS);
        }

        Consultation updated = consultationRepository.save(consultation);
        return consultationMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public ConsultationResponse recordVitals(String id, RecordVitalsRequest request) {
        log.info("Recording vital signs for consultation id='{}'", id);
        Consultation consultation = findEntityWithDiagnosesOrThrow(id);

        ConsultationVitals vitals = new ConsultationVitals(
                request.systolicBp(),
                request.diastolicBp(),
                request.heartRate(),
                request.respiratoryRate(),
                request.temperatureCelsius(),
                request.oxygenSaturation(),
                request.heightCm(),
                request.weightKg()
        );

        consultation.updateVitals(vitals);

        if (consultation.getStatus() == ConsultationStatus.STARTED) {
            consultation.transitionTo(ConsultationStatus.IN_PROGRESS);
        }

        Consultation updated = consultationRepository.save(consultation);
        log.info("Vital signs successfully recorded for consultation id='{}' (BMI: {})", id, vitals.getBmi());
        return consultationMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public ConsultationResponse addDiagnosis(String id, AddDiagnosisRequest request) {
        log.info("Adding diagnosis code='{}' ({}) to consultation id='{}'",
                request.diagnosisCode(), request.diagnosisType(), id);
        Consultation consultation = findEntityWithDiagnosesOrThrow(id);

        ConsultationDiagnosis diagnosis = new ConsultationDiagnosis(
                UUID.randomUUID().toString(),
                consultation,
                request.diagnosisCode().trim(),
                request.diagnosisName().trim(),
                request.diagnosisType(),
                request.severity() != null ? request.severity().trim() : null,
                request.notes() != null ? request.notes().trim() : null
        );

        consultation.addDiagnosis(diagnosis);

        if (consultation.getStatus() == ConsultationStatus.STARTED) {
            consultation.transitionTo(ConsultationStatus.IN_PROGRESS);
        }

        Consultation updated = consultationRepository.save(consultation);
        return consultationMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public ConsultationResponse removeDiagnosis(String id, String diagnosisId) {
        log.info("Removing diagnosis id='{}' from consultation id='{}'", diagnosisId, id);
        Consultation consultation = findEntityWithDiagnosesOrThrow(id);

        consultation.removeDiagnosis(diagnosisId.trim());

        Consultation updated = consultationRepository.save(consultation);
        return consultationMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public ConsultationResponse completeConsultation(String id) {
        log.info("Completing and finalizing consultation encounter id='{}'", id);
        Consultation consultation = findEntityWithDiagnosesOrThrow(id);

        // Validation: must have at least one diagnosis or treatment plan recorded before completion
        boolean hasDiagnoses = consultation.getDiagnoses() != null && !consultation.getDiagnoses().isEmpty();
        boolean hasPlan = consultation.getTreatmentPlan() != null && !consultation.getTreatmentPlan().isBlank();
        if (!hasDiagnoses && !hasPlan) {
            throw new BusinessRuleException(
                    "CONSULTATION_INCOMPLETE",
                    "Consultation cannot be completed without at least one diagnosis or treatment plan.",
                    HttpStatus.BAD_REQUEST
            );
        }

        consultation.transitionTo(ConsultationStatus.COMPLETED);

        // Synchronize queue entry if linked
        if (consultation.getQueueEntryId() != null) {
            try {
                queueService.completeConsultation(consultation.getQueueEntryId());
            } catch (Exception ex) {
                log.warn("Queue entry '{}' completion synchronization skipped: {}",
                        consultation.getQueueEntryId(), ex.getMessage());
            }
        }

        // Synchronize appointment if linked
        if (consultation.getAppointmentId() != null) {
            try {
                appointmentService.completeConsultation(consultation.getAppointmentId());
            } catch (Exception ex) {
                log.warn("Appointment '{}' completion synchronization skipped: {}",
                        consultation.getAppointmentId(), ex.getMessage());
            }
        }

        Consultation completed = consultationRepository.save(consultation);
        log.info("Consultation encounter id='{}' successfully finalized and locked", completed.getId());
        return consultationMapper.toResponse(completed);
    }

    @Override
    @Transactional
    public ConsultationResponse cancelConsultation(String id, String reason) {
        log.info("Cancelling consultation encounter id='{}', reason='{}'", id, reason);
        Consultation consultation = findEntityWithDiagnosesOrThrow(id);

        consultation.transitionTo(ConsultationStatus.CANCELLED);

        Consultation cancelled = consultationRepository.save(consultation);
        log.info("Consultation encounter id='{}' successfully marked CANCELLED", cancelled.getId());
        return consultationMapper.toResponse(cancelled);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ConsultationSummaryResponse> getConsultationsByPatient(String patientId, Pageable pageable) {
        validatePatient(patientId);
        return consultationRepository.findByPatientIdOrderByStartedAtDesc(patientId.trim(), pageable)
                .map(consultationMapper::toSummaryResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ConsultationSummaryResponse> getConsultationsByDoctor(String doctorId, Pageable pageable) {
        validateDoctor(doctorId);
        return consultationRepository.findByDoctorIdOrderByStartedAtDesc(doctorId.trim(), pageable)
                .map(consultationMapper::toSummaryResponse);
    }

    private Consultation findEntityWithDiagnosesOrThrow(String id) {
        return consultationRepository.findWithDiagnosesById(id.trim())
                .orElseThrow(() -> new ConsultationNotFoundException(id.trim()));
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
                    String.format("Staff member '%s' is of type '%s', but only DOCTOR can conduct consultations.",
                            doctorId, staff.getStaffType()),
                    HttpStatus.BAD_REQUEST
            );
        }
    }
}
