package com.careflow.appointment.service;

import com.careflow.appointment.domain.Appointment;
import com.careflow.appointment.domain.AppointmentStatus;
import com.careflow.appointment.dto.AppointmentResponse;
import com.careflow.appointment.dto.AppointmentSummaryResponse;
import com.careflow.appointment.dto.BookAppointmentRequest;
import com.careflow.appointment.dto.CancelAppointmentRequest;
import com.careflow.appointment.dto.RescheduleAppointmentRequest;
import com.careflow.appointment.exception.AppointmentNotFoundException;
import com.careflow.appointment.exception.DoubleBookingException;
import com.careflow.appointment.exception.InvalidAppointmentStatusTransitionException;
import com.careflow.appointment.mapper.AppointmentMapper;
import com.careflow.appointment.repository.AppointmentRepository;
import com.careflow.appointment.repository.AppointmentSpecification;
import com.careflow.common.dto.PageResponse;
import com.careflow.common.exception.BusinessRuleException;
import com.careflow.common.exception.ResourceNotFoundException;
import com.careflow.department.service.DepartmentService;
import com.careflow.patient.repository.PatientRepository;
import com.careflow.staff.domain.StaffMember;
import com.careflow.staff.domain.StaffStatus;
import com.careflow.staff.domain.StaffType;
import com.careflow.staff.exception.StaffNotFoundException;
import com.careflow.staff.repository.StaffMemberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Implementation of AppointmentService enforcing concurrency safety, double-booking prevention, and lifecycle state rules (§13, §19, §20).
 */
@Service
public class AppointmentServiceImpl implements AppointmentService {

    private static final Logger log = LoggerFactory.getLogger(AppointmentServiceImpl.class);

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final StaffMemberRepository staffMemberRepository;
    private final DepartmentService departmentService;
    private final AppointmentMapper appointmentMapper;

    public AppointmentServiceImpl(AppointmentRepository appointmentRepository,
                                  PatientRepository patientRepository,
                                  StaffMemberRepository staffMemberRepository,
                                  DepartmentService departmentService,
                                  AppointmentMapper appointmentMapper) {
        this.appointmentRepository = appointmentRepository;
        this.patientRepository = patientRepository;
        this.staffMemberRepository = staffMemberRepository;
        this.departmentService = departmentService;
        this.appointmentMapper = appointmentMapper;
    }

    @Override
    @Transactional
    public AppointmentResponse bookAppointment(BookAppointmentRequest request) {
        log.info("Booking appointment patient='{}', doctor='{}', time='{}'",
                request.patientId(), request.doctorId(), request.appointmentDateTime());

        validatePatient(request.patientId());
        validateDoctor(request.doctorId());
        departmentService.getDepartmentById(request.departmentId().trim());

        if (request.appointmentDateTime().isBefore(LocalDateTime.now())) {
            throw new BusinessRuleException("APPOINTMENT_PAST_DATE",
                    "Appointment date and time must be in the future.",
                    HttpStatus.BAD_REQUEST);
        }

        // Fast-path application check (§20)
        if (appointmentRepository.existsByDoctorIdAndAppointmentDateTimeAndActiveSlotFlag(
                request.doctorId(), request.appointmentDateTime(), 1)) {
            log.warn("Double booking rejected (fast-path): doctor='{}', time='{}'",
                    request.doctorId(), request.appointmentDateTime());
            throw new DoubleBookingException(request.doctorId(), request.appointmentDateTime());
        }

        String id = UUID.randomUUID().toString();
        Appointment appointment = appointmentMapper.toEntity(id, request);

        try {
            // saveAndFlush triggers immediate constraint validation within transaction
            Appointment saved = appointmentRepository.saveAndFlush(appointment);
            log.info("Successfully booked appointment id='{}'", saved.getId());
            return appointmentMapper.toResponse(saved);
        } catch (DataIntegrityViolationException ex) {
            // Hard database constraint uk_active_appointment_slot caught concurrent double-booking (§20, §92)
            log.warn("Double booking rejected (database constraint uk_active_appointment_slot): doctor='{}', time='{}'",
                    request.doctorId(), request.appointmentDateTime());
            throw new DoubleBookingException(request.doctorId(), request.appointmentDateTime());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public AppointmentResponse getAppointmentById(String id) {
        log.debug("Fetching appointment by id='{}'", id);
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new AppointmentNotFoundException(id));
        return appointmentMapper.toResponse(appointment);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AppointmentSummaryResponse> searchAppointments(
            String patientId,
            String doctorId,
            String departmentId,
            AppointmentStatus status,
            LocalDateTime fromDateTime,
            LocalDateTime toDateTime,
            Pageable pageable
    ) {
        log.debug("Searching appointments patient='{}', doctor='{}', status='{}'", patientId, doctorId, status);
        Specification<Appointment> spec = AppointmentSpecification.withFilters(
                patientId, doctorId, departmentId, status, fromDateTime, toDateTime
        );
        Page<Appointment> page = appointmentRepository.findAll(spec, pageable);
        return PageResponse.from(page, appointmentMapper::toSummaryResponse);
    }

    @Override
    @Transactional
    public AppointmentResponse confirmAppointment(String id) {
        return transitionAppointmentStatus(id, AppointmentStatus.CONFIRMED, null);
    }

    @Override
    @Transactional
    public AppointmentResponse checkInAppointment(String id) {
        return transitionAppointmentStatus(id, AppointmentStatus.CHECKED_IN, null);
    }

    @Override
    @Transactional
    public AppointmentResponse startConsultation(String id) {
        return transitionAppointmentStatus(id, AppointmentStatus.IN_PROGRESS, null);
    }

    @Override
    @Transactional
    public AppointmentResponse completeConsultation(String id) {
        return transitionAppointmentStatus(id, AppointmentStatus.COMPLETED, null);
    }

    @Override
    @Transactional
    public AppointmentResponse cancelAppointment(String id, CancelAppointmentRequest request) {
        String reason = request != null ? request.cancellationReason() : null;
        return transitionAppointmentStatus(id, AppointmentStatus.CANCELLED, reason);
    }

    @Override
    @Transactional
    public AppointmentResponse markNoShow(String id) {
        return transitionAppointmentStatus(id, AppointmentStatus.NO_SHOW, null);
    }

    @Override
    @Transactional
    public AppointmentResponse rescheduleAppointment(String id, RescheduleAppointmentRequest request) {
        log.info("Rescheduling appointment id='{}' to '{}'", id, request.newDateTime());

        if (request.newDateTime().isBefore(LocalDateTime.now())) {
            throw new BusinessRuleException("APPOINTMENT_PAST_DATE",
                    "New appointment date and time must be in the future.",
                    HttpStatus.BAD_REQUEST);
        }

        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new AppointmentNotFoundException(id));

        if (!appointment.getStatus().isActive()) {
            throw new BusinessRuleException("APPOINTMENT_INACTIVE",
                    "Cannot reschedule an appointment in status " + appointment.getStatus(),
                    HttpStatus.BAD_REQUEST);
        }

        // Fast-path application check
        if (appointmentRepository.existsByDoctorIdAndAppointmentDateTimeAndActiveSlotFlag(
                appointment.getDoctorId(), request.newDateTime(), 1)) {
            throw new DoubleBookingException(appointment.getDoctorId(), request.newDateTime());
        }

        appointment.reschedule(request.newDateTime());

        try {
            Appointment updated = appointmentRepository.saveAndFlush(appointment);
            log.info("Successfully rescheduled appointment id='{}'", updated.getId());
            return appointmentMapper.toResponse(updated);
        } catch (DataIntegrityViolationException ex) {
            throw new DoubleBookingException(appointment.getDoctorId(), request.newDateTime());
        }
    }

    private AppointmentResponse transitionAppointmentStatus(String id, AppointmentStatus targetStatus, String reason) {
        log.info("Transitioning appointment id='{}' to status='{}'", id, targetStatus);

        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new AppointmentNotFoundException(id));

        try {
            appointment.transitionTo(targetStatus, reason);
        } catch (IllegalStateException ex) {
            log.warn("Invalid appointment transition attempted for id='{}': current='{}', target='{}'",
                    id, appointment.getStatus(), targetStatus);
            throw new InvalidAppointmentStatusTransitionException(appointment.getStatus(), targetStatus);
        }

        Appointment updated = appointmentRepository.save(appointment);
        log.info("Appointment id='{}' successfully transitioned to '{}'", updated.getId(), updated.getStatus());
        return appointmentMapper.toResponse(updated);
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
            throw new BusinessRuleException("INVALID_DOCTOR",
                    "Staff member '" + doctorId + "' is not a DOCTOR",
                    HttpStatus.BAD_REQUEST);
        }

        if (staff.getStatus() == StaffStatus.TERMINATED || staff.getStatus() == StaffStatus.RESIGNED) {
            throw new BusinessRuleException("INACTIVE_DOCTOR",
                    "Doctor '" + doctorId + "' is inactive with status: " + staff.getStatus(),
                    HttpStatus.BAD_REQUEST);
        }
    }
}
