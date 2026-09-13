package com.careflow.appointment.service;

import com.careflow.appointment.domain.Appointment;
import com.careflow.appointment.domain.AppointmentStatus;
import com.careflow.appointment.dto.AppointmentResponse;
import com.careflow.appointment.dto.BookAppointmentRequest;
import com.careflow.appointment.dto.CancelAppointmentRequest;
import com.careflow.appointment.dto.RescheduleAppointmentRequest;
import com.careflow.appointment.exception.AppointmentNotFoundException;
import com.careflow.appointment.exception.DoubleBookingException;
import com.careflow.appointment.exception.InvalidAppointmentStatusTransitionException;
import com.careflow.appointment.mapper.AppointmentMapper;
import com.careflow.appointment.repository.AppointmentRepository;
import com.careflow.common.exception.BusinessRuleException;
import com.careflow.common.exception.ResourceNotFoundException;
import com.careflow.department.dto.DepartmentResponse;
import com.careflow.department.service.DepartmentService;
import com.careflow.patient.repository.PatientRepository;
import com.careflow.staff.domain.StaffMember;
import com.careflow.staff.domain.StaffType;
import com.careflow.staff.exception.StaffNotFoundException;
import com.careflow.staff.repository.StaffMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private StaffMemberRepository staffMemberRepository;

    @Mock
    private DepartmentService departmentService;

    private AppointmentMapper appointmentMapper;
    private AppointmentServiceImpl appointmentService;

    private StaffMember mockDoctor;

    @BeforeEach
    void setUp() {
        appointmentMapper = new AppointmentMapper();
        appointmentService = new AppointmentServiceImpl(
                appointmentRepository,
                patientRepository,
                staffMemberRepository,
                departmentService,
                appointmentMapper
        );

        mockDoctor = new StaffMember(
                "doc-1",
                "DOC-001",
                "dept-card-001",
                "Alexander",
                "Fleming",
                "fleming@careflow.local",
                "+1-555-0102",
                StaffType.DOCTOR,
                LocalDate.of(2023, 1, 1)
        );
    }

    @Test
    @DisplayName("bookAppointment should save and return appointment when valid (§19, §20)")
    void bookAppointment_shouldSucceed_whenValid() {
        LocalDateTime appointmentTime = LocalDateTime.now().plusDays(2).withHour(10).withMinute(0);
        BookAppointmentRequest request = new BookAppointmentRequest(
                "pat-1", "doc-1", "dept-card-001", appointmentTime, 30, "Routine checkup"
        );

        when(patientRepository.existsById("pat-1")).thenReturn(true);
        when(staffMemberRepository.findById("doc-1")).thenReturn(Optional.of(mockDoctor));
        when(departmentService.getDepartmentById("dept-card-001")).thenReturn(
                new DepartmentResponse("dept-card-001", "CARD", "Cardiology", null, null, null, null, null, null, null, null, 0L)
        );
        when(appointmentRepository.existsByDoctorIdAndAppointmentDateTimeAndActiveSlotFlag("doc-1", appointmentTime, 1))
                .thenReturn(false);
        when(appointmentRepository.saveAndFlush(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));

        AppointmentResponse response = appointmentService.bookAppointment(request);

        assertThat(response).isNotNull();
        assertThat(response.patientId()).isEqualTo("pat-1");
        assertThat(response.doctorId()).isEqualTo("doc-1");
        assertThat(response.status()).isEqualTo(AppointmentStatus.REQUESTED);
        verify(appointmentRepository).saveAndFlush(any(Appointment.class));
    }

    @Test
    @DisplayName("bookAppointment should throw DoubleBookingException when slot already booked (§20, §92)")
    void bookAppointment_shouldThrow_whenSlotAlreadyBooked() {
        LocalDateTime appointmentTime = LocalDateTime.now().plusDays(2).withHour(10).withMinute(0);
        BookAppointmentRequest request = new BookAppointmentRequest(
                "pat-1", "doc-1", "dept-card-001", appointmentTime, 30, "Routine checkup"
        );

        when(patientRepository.existsById("pat-1")).thenReturn(true);
        when(staffMemberRepository.findById("doc-1")).thenReturn(Optional.of(mockDoctor));
        when(departmentService.getDepartmentById("dept-card-001")).thenReturn(
                new DepartmentResponse("dept-card-001", "CARD", "Cardiology", null, null, null, null, null, null, null, null, 0L)
        );
        when(appointmentRepository.existsByDoctorIdAndAppointmentDateTimeAndActiveSlotFlag("doc-1", appointmentTime, 1))
                .thenReturn(true);

        assertThatThrownBy(() -> appointmentService.bookAppointment(request))
                .isInstanceOf(DoubleBookingException.class)
                .hasMessageContaining("already booked");
    }

    @Test
    @DisplayName("bookAppointment should throw DoubleBookingException when DB unique constraint fails concurrently (§20, §92)")
    void bookAppointment_shouldThrow_whenConcurrentDatabaseConstraintFails() {
        LocalDateTime appointmentTime = LocalDateTime.now().plusDays(2).withHour(10).withMinute(0);
        BookAppointmentRequest request = new BookAppointmentRequest(
                "pat-1", "doc-1", "dept-card-001", appointmentTime, 30, "Routine checkup"
        );

        when(patientRepository.existsById("pat-1")).thenReturn(true);
        when(staffMemberRepository.findById("doc-1")).thenReturn(Optional.of(mockDoctor));
        when(departmentService.getDepartmentById("dept-card-001")).thenReturn(
                new DepartmentResponse("dept-card-001", "CARD", "Cardiology", null, null, null, null, null, null, null, null, 0L)
        );
        when(appointmentRepository.existsByDoctorIdAndAppointmentDateTimeAndActiveSlotFlag("doc-1", appointmentTime, 1))
                .thenReturn(false);
        when(appointmentRepository.saveAndFlush(any(Appointment.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate entry for uk_active_appointment_slot"));

        assertThatThrownBy(() -> appointmentService.bookAppointment(request))
                .isInstanceOf(DoubleBookingException.class)
                .hasMessageContaining("already booked");
    }

    @Test
    @DisplayName("bookAppointment should throw ResourceNotFoundException when patient not found (§19)")
    void bookAppointment_shouldThrow_whenPatientNotFound() {
        LocalDateTime appointmentTime = LocalDateTime.now().plusDays(2).withHour(10).withMinute(0);
        BookAppointmentRequest request = new BookAppointmentRequest(
                "missing-pat", "doc-1", "dept-card-001", appointmentTime, 30, "Checkup"
        );

        when(patientRepository.existsById("missing-pat")).thenReturn(false);

        assertThatThrownBy(() -> appointmentService.bookAppointment(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("missing-pat");
    }

    @Test
    @DisplayName("bookAppointment should throw StaffNotFoundException when doctor not found (§19)")
    void bookAppointment_shouldThrow_whenDoctorNotFound() {
        LocalDateTime appointmentTime = LocalDateTime.now().plusDays(2).withHour(10).withMinute(0);
        BookAppointmentRequest request = new BookAppointmentRequest(
                "pat-1", "missing-doc", "dept-card-001", appointmentTime, 30, "Checkup"
        );

        when(patientRepository.existsById("pat-1")).thenReturn(true);
        when(staffMemberRepository.findById("missing-doc")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> appointmentService.bookAppointment(request))
                .isInstanceOf(StaffNotFoundException.class);
    }

    @Test
    @DisplayName("confirmAppointment should transition status from REQUESTED to CONFIRMED (§19, §69)")
    void confirmAppointment_shouldTransitionToConfirmed() {
        Appointment appointment = new Appointment(
                "apt-1", "pat-1", "doc-1", "dept-card-001",
                LocalDateTime.now().plusDays(1), 30, "Checkup"
        );
        appointment.setStatus(AppointmentStatus.REQUESTED);

        when(appointmentRepository.findById("apt-1")).thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));

        AppointmentResponse response = appointmentService.confirmAppointment("apt-1");

        assertThat(response.status()).isEqualTo(AppointmentStatus.CONFIRMED);
    }

    @Test
    @DisplayName("checkInAppointment should transition status from CONFIRMED to CHECKED_IN (§19, §69)")
    void checkInAppointment_shouldTransitionToCheckedIn() {
        Appointment appointment = new Appointment(
                "apt-1", "pat-1", "doc-1", "dept-card-001",
                LocalDateTime.now().plusDays(1), 30, "Checkup"
        );
        appointment.setStatus(AppointmentStatus.CONFIRMED);

        when(appointmentRepository.findById("apt-1")).thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));

        AppointmentResponse response = appointmentService.checkInAppointment("apt-1");

        assertThat(response.status()).isEqualTo(AppointmentStatus.CHECKED_IN);
    }

    @Test
    @DisplayName("startConsultation and completeConsultation should transition through clinical states (§19, §69)")
    void startAndCompleteConsultation_shouldTransitionStates() {
        Appointment appointment = new Appointment(
                "apt-1", "pat-1", "doc-1", "dept-card-001",
                LocalDateTime.now().plusDays(1), 30, "Checkup"
        );
        appointment.setStatus(AppointmentStatus.CHECKED_IN);

        when(appointmentRepository.findById("apt-1")).thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));

        AppointmentResponse startResponse = appointmentService.startConsultation("apt-1");
        assertThat(startResponse.status()).isEqualTo(AppointmentStatus.IN_PROGRESS);

        AppointmentResponse completeResponse = appointmentService.completeConsultation("apt-1");
        assertThat(completeResponse.status()).isEqualTo(AppointmentStatus.COMPLETED);
    }

    @Test
    @DisplayName("cancelAppointment should set status CANCELLED and release active slot reservation (§19, §20)")
    void cancelAppointment_shouldCancelAndReleaseSlot() {
        Appointment appointment = new Appointment(
                "apt-1", "pat-1", "doc-1", "dept-card-001",
                LocalDateTime.now().plusDays(1), 30, "Checkup"
        );
        appointment.setStatus(AppointmentStatus.CONFIRMED);
        appointment.setActiveSlotFlag(1);

        when(appointmentRepository.findById("apt-1")).thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));

        AppointmentResponse response = appointmentService.cancelAppointment(
                "apt-1", new CancelAppointmentRequest("Patient requested cancellation")
        );

        assertThat(response.status()).isEqualTo(AppointmentStatus.CANCELLED);
        assertThat(response.cancellationReason()).isEqualTo("Patient requested cancellation");
        assertThat(appointment.getActiveSlotFlag()).isNull(); // Slot is freed! (§20)
    }

    @Test
    @DisplayName("transition should throw InvalidAppointmentStatusTransitionException when transition invalid (§19, §69)")
    void transition_shouldThrow_whenInvalidTransition() {
        Appointment appointment = new Appointment(
                "apt-1", "pat-1", "doc-1", "dept-card-001",
                LocalDateTime.now().plusDays(1), 30, "Checkup"
        );
        appointment.setStatus(AppointmentStatus.CANCELLED);

        when(appointmentRepository.findById("apt-1")).thenReturn(Optional.of(appointment));

        // CANCELLED -> COMPLETED is forbidden per §19!
        assertThatThrownBy(() -> appointmentService.completeConsultation("apt-1"))
                .isInstanceOf(InvalidAppointmentStatusTransitionException.class)
                .hasMessageContaining("CANCELLED");
    }

    @Test
    @DisplayName("rescheduleAppointment should update appointment date time when new slot free (§19, §20)")
    void rescheduleAppointment_shouldSucceed_whenNewSlotFree() {
        LocalDateTime originalTime = LocalDateTime.now().plusDays(2).withHour(9).withMinute(0);
        LocalDateTime newTime = LocalDateTime.now().plusDays(3).withHour(11).withMinute(0);

        Appointment appointment = new Appointment(
                "apt-1", "pat-1", "doc-1", "dept-card-001", originalTime, 30, "Consultation"
        );
        appointment.setStatus(AppointmentStatus.CONFIRMED);

        when(appointmentRepository.findById("apt-1")).thenReturn(Optional.of(appointment));
        when(appointmentRepository.existsByDoctorIdAndAppointmentDateTimeAndActiveSlotFlag("doc-1", newTime, 1))
                .thenReturn(false);
        when(appointmentRepository.saveAndFlush(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));

        AppointmentResponse response = appointmentService.rescheduleAppointment(
                "apt-1", new RescheduleAppointmentRequest(newTime)
        );

        assertThat(response.appointmentDateTime()).isEqualTo(newTime);
    }
}
