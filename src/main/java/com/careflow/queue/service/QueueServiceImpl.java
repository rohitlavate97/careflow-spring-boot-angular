package com.careflow.queue.service;

import com.careflow.appointment.repository.AppointmentRepository;
import com.careflow.common.exception.BusinessRuleException;
import com.careflow.department.domain.Department;
import com.careflow.department.dto.DepartmentResponse;
import com.careflow.department.repository.DepartmentRepository;
import com.careflow.department.service.DepartmentService;
import com.careflow.patient.repository.PatientRepository;
import com.careflow.queue.domain.QueueEntry;
import com.careflow.queue.domain.QueueStatus;
import com.careflow.queue.dto.DepartmentQueueLiveStatusResponse;
import com.careflow.queue.dto.EnqueuePatientRequest;
import com.careflow.queue.dto.QueueEntryResponse;
import com.careflow.queue.dto.QueueSummaryResponse;
import com.careflow.queue.exception.QueueEmptyException;
import com.careflow.queue.exception.QueueEntryNotFoundException;
import com.careflow.queue.mapper.QueueEntryMapper;
import com.careflow.queue.repository.QueueEntryRepository;
import com.careflow.staff.repository.StaffMemberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Production-ready implementation of QueueService with concurrency-safe locking and priority scheduling (§21, §92).
 */
@Service
public class QueueServiceImpl implements QueueService {

    private static final Logger log = LoggerFactory.getLogger(QueueServiceImpl.class);

    private final QueueEntryRepository queueEntryRepository;
    private final QueueEntryMapper queueEntryMapper;
    private final DepartmentRepository departmentRepository;
    private final DepartmentService departmentService;
    private final PatientRepository patientRepository;
    private final StaffMemberRepository staffMemberRepository;
    private final AppointmentRepository appointmentRepository;

    public QueueServiceImpl(QueueEntryRepository queueEntryRepository,
                            QueueEntryMapper queueEntryMapper,
                            DepartmentRepository departmentRepository,
                            DepartmentService departmentService,
                            PatientRepository patientRepository,
                            StaffMemberRepository staffMemberRepository,
                            AppointmentRepository appointmentRepository) {
        this.queueEntryRepository = queueEntryRepository;
        this.queueEntryMapper = queueEntryMapper;
        this.departmentRepository = departmentRepository;
        this.departmentService = departmentService;
        this.patientRepository = patientRepository;
        this.staffMemberRepository = staffMemberRepository;
        this.appointmentRepository = appointmentRepository;
    }

    @Override
    @Transactional
    public QueueEntryResponse enqueuePatient(EnqueuePatientRequest request) {
        log.info("Enqueuing patient id='{}' in department id='{}'", request.patientId(), request.departmentId());

        validatePatient(request.patientId());

        // Lock department record to serialize daily token generation per department (§21, §92)
        Department department = departmentRepository.findByIdForUpdate(request.departmentId().trim())
                .orElseThrow(() -> new BusinessRuleException("DEPARTMENT_NOT_FOUND",
                        String.format("Department with ID '%s' does not exist.", request.departmentId()),
                        HttpStatus.NOT_FOUND));

        if (request.doctorId() != null && !request.doctorId().isBlank()) {
            validateDoctor(request.doctorId().trim());
        }

        if (request.appointmentId() != null && !request.appointmentId().isBlank()) {
            validateAppointment(request.appointmentId().trim());
        }

        // Check if patient is already actively queued today
        List<QueueStatus> activeStatuses = List.of(QueueStatus.REGISTERED, QueueStatus.WAITING, QueueStatus.CALLED, QueueStatus.IN_CONSULTATION);
        Optional<QueueEntry> existingActive = queueEntryRepository.findFirstByPatientIdAndStatusIn(request.patientId().trim(), activeStatuses);
        if (existingActive.isPresent()) {
            log.warn("Patient '{}' is already in active queue state '{}'", request.patientId(), existingActive.get().getStatus());
            throw new BusinessRuleException("PATIENT_ALREADY_IN_QUEUE",
                    String.format("Patient '%s' is already in active queue with token '%s' (status: %s).",
                            request.patientId(), existingActive.get().getTokenDisplay(), existingActive.get().getStatus()),
                    HttpStatus.CONFLICT);
        }

        LocalDate today = LocalDate.now();

        int nextToken = queueEntryRepository.findMaxTokenNumberByDepartmentIdAndQueueDate(department.getId(), today) + 1;
        String tokenDisplay = String.format("%s-%03d", department.getCode(), nextToken);
        String id = UUID.randomUUID().toString();

        QueueEntry entry = queueEntryMapper.toEntity(id, request, today, nextToken, tokenDisplay);
        QueueEntry savedEntry = queueEntryRepository.saveAndFlush(entry);
        log.info("Successfully enqueued patient with token '{}' in department '{}'", tokenDisplay, department.getCode());

        long patientsAhead = calculatePatientsAhead(savedEntry);
        return queueEntryMapper.toResponse(savedEntry, patientsAhead);
    }

    @Override
    @Transactional
    public QueueEntryResponse callNextPatient(String departmentId, String doctorId) {
        log.info("Calling next patient in department id='{}', doctorId='{}'", departmentId, doctorId);
        departmentService.getDepartmentById(departmentId.trim());

        LocalDate today = LocalDate.now();
        String resolvedDoctorId = (doctorId != null && !doctorId.isBlank()) ? doctorId.trim() : null;

        List<String> candidateIds = queueEntryRepository.findNextWaitingCandidateIds(
                departmentId.trim(), today, resolvedDoctorId, PageRequest.of(0, 10));

        if (candidateIds.isEmpty()) {
            log.info("No waiting patients found in queue for department '{}' on {}", departmentId, today);
            throw new QueueEmptyException(departmentId, today);
        }

        for (String candidateId : candidateIds) {
            Optional<QueueEntry> optEntry = queueEntryRepository.findByIdForUpdate(candidateId);
            if (optEntry.isPresent()) {
                QueueEntry entry = optEntry.get();
                if (entry.getStatus() == QueueStatus.WAITING) {
                    entry.call(resolvedDoctorId);
                    QueueEntry saved = queueEntryRepository.saveAndFlush(entry);
                    log.info("Called patient with token '{}' for doctor '{}'", saved.getTokenDisplay(), resolvedDoctorId);
                    return queueEntryMapper.toResponse(saved, 0);
                }
            }
        }

        // All candidates in the batch were grabbed concurrently by other threads
        throw new QueueEmptyException(departmentId, today);
    }

    @Override
    @Transactional
    public QueueEntryResponse startConsultation(String queueEntryId) {
        log.info("Starting consultation for queue entry id='{}'", queueEntryId);
        QueueEntry entry = findEntryOrThrow(queueEntryId);
        entry.startConsultation();
        QueueEntry saved = queueEntryRepository.saveAndFlush(entry);
        return queueEntryMapper.toResponse(saved, 0);
    }

    @Override
    @Transactional
    public QueueEntryResponse completeConsultation(String queueEntryId) {
        log.info("Completing consultation for queue entry id='{}'", queueEntryId);
        QueueEntry entry = findEntryOrThrow(queueEntryId);
        entry.complete();
        QueueEntry saved = queueEntryRepository.saveAndFlush(entry);
        return queueEntryMapper.toResponse(saved, 0);
    }

    @Override
    @Transactional
    public QueueEntryResponse skipPatient(String queueEntryId) {
        log.info("Skipping patient for queue entry id='{}'", queueEntryId);
        QueueEntry entry = findEntryOrThrow(queueEntryId);
        entry.skip();
        QueueEntry saved = queueEntryRepository.saveAndFlush(entry);
        return queueEntryMapper.toResponse(saved, 0);
    }

    @Override
    @Transactional
    public QueueEntryResponse requeuePatient(String queueEntryId) {
        log.info("Requeuing patient for queue entry id='{}'", queueEntryId);
        QueueEntry entry = findEntryOrThrow(queueEntryId);
        entry.requeue();
        QueueEntry saved = queueEntryRepository.saveAndFlush(entry);
        long patientsAhead = calculatePatientsAhead(saved);
        return queueEntryMapper.toResponse(saved, patientsAhead);
    }

    @Override
    @Transactional
    public QueueEntryResponse cancelQueueEntry(String queueEntryId, String reason) {
        log.info("Cancelling queue entry id='{}', reason='{}'", queueEntryId, reason);
        QueueEntry entry = findEntryOrThrow(queueEntryId);
        entry.cancel(reason);
        QueueEntry saved = queueEntryRepository.saveAndFlush(entry);
        return queueEntryMapper.toResponse(saved, 0);
    }

    @Override
    @Transactional(readOnly = true)
    public QueueEntryResponse getQueueEntry(String id) {
        QueueEntry entry = findEntryOrThrow(id);
        long patientsAhead = calculatePatientsAhead(entry);
        return queueEntryMapper.toResponse(entry, patientsAhead);
    }

    @Override
    @Transactional(readOnly = true)
    public DepartmentQueueLiveStatusResponse getDepartmentLiveStatus(String departmentId) {
        departmentService.getDepartmentById(departmentId.trim());
        LocalDate today = LocalDate.now();

        long totalWaiting = queueEntryRepository.countByDepartmentIdAndQueueDateAndStatus(
                departmentId.trim(), today, QueueStatus.WAITING);
        long totalCalled = queueEntryRepository.countByDepartmentIdAndQueueDateAndStatus(
                departmentId.trim(), today, QueueStatus.CALLED);
        long totalInConsultation = queueEntryRepository.countByDepartmentIdAndQueueDateAndStatus(
                departmentId.trim(), today, QueueStatus.IN_CONSULTATION);

        List<QueueEntry> calledList = queueEntryRepository.findByDepartmentIdAndQueueDateAndStatusInOrderByPriorityAscEntryTimeAsc(
                departmentId.trim(), today, List.of(QueueStatus.CALLED, QueueStatus.IN_CONSULTATION));

        List<QueueEntry> waitingList = queueEntryRepository.findByDepartmentIdAndQueueDateAndStatusInOrderByPriorityAscEntryTimeAsc(
                departmentId.trim(), today, List.of(QueueStatus.WAITING));

        List<QueueSummaryResponse> calledSummaries = queueEntryMapper.toSummaries(calledList);
        List<QueueSummaryResponse> waitingSummaries = queueEntryMapper.toSummaries(waitingList);

        return new DepartmentQueueLiveStatusResponse(
                departmentId.trim(),
                today,
                totalWaiting,
                totalCalled,
                totalInConsultation,
                calledSummaries,
                waitingSummaries
        );
    }

    private QueueEntry findEntryOrThrow(String id) {
        return queueEntryRepository.findById(id.trim())
                .orElseThrow(() -> new QueueEntryNotFoundException(id.trim()));
    }

    private long calculatePatientsAhead(QueueEntry entry) {
        if (entry.getStatus() != QueueStatus.WAITING) {
            return 0;
        }
        return queueEntryRepository.countPatientsAhead(
                entry.getDepartmentId(),
                entry.getQueueDate(),
                entry.getPriority().getRank(),
                entry.getEntryTime()
        );
    }

    private void validatePatient(String patientId) {
        if (!patientRepository.existsById(patientId.trim())) {
            throw new BusinessRuleException("PATIENT_NOT_FOUND",
                    String.format("Patient with ID '%s' does not exist.", patientId),
                    HttpStatus.NOT_FOUND);
        }
    }

    private void validateDoctor(String doctorId) {
        if (!staffMemberRepository.existsById(doctorId.trim())) {
            throw new BusinessRuleException("DOCTOR_NOT_FOUND",
                    String.format("Doctor with ID '%s' does not exist.", doctorId),
                    HttpStatus.NOT_FOUND);
        }
    }

    private void validateAppointment(String appointmentId) {
        if (!appointmentRepository.existsById(appointmentId.trim())) {
            throw new BusinessRuleException("APPOINTMENT_NOT_FOUND",
                    String.format("Appointment with ID '%s' does not exist.", appointmentId),
                    HttpStatus.NOT_FOUND);
        }
    }
}
