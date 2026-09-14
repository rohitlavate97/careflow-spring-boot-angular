package com.careflow.common.outbox;

import com.careflow.appointment.dto.AppointmentResponse;
import com.careflow.appointment.dto.BookAppointmentRequest;
import com.careflow.appointment.dto.CancelAppointmentRequest;
import com.careflow.appointment.service.AppointmentService;
import com.careflow.common.config.KafkaConfig;
import com.careflow.common.outbox.domain.OutboxEvent;
import com.careflow.common.outbox.domain.OutboxStatus;
import com.careflow.common.outbox.repository.OutboxEventRepository;
import com.careflow.department.domain.Department;
import com.careflow.department.repository.DepartmentRepository;
import com.careflow.patient.domain.BloodGroup;
import com.careflow.patient.domain.Gender;
import com.careflow.patient.domain.Patient;
import com.careflow.patient.repository.PatientRepository;
import com.careflow.staff.domain.DoctorProfile;
import com.careflow.staff.domain.StaffMember;
import com.careflow.staff.domain.StaffType;
import com.careflow.staff.repository.DoctorProfileRepository;
import com.careflow.staff.repository.StaffMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class OutboxIntegrationTest {

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private StaffMemberRepository staffMemberRepository;

    @Autowired
    private DoctorProfileRepository doctorProfileRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    private Patient testPatient;
    private StaffMember testDoctor;
    private Department testDept;

    @BeforeEach
    void setUp() {
        outboxEventRepository.deleteAll();

        String suffix = UUID.randomUUID().toString().substring(0, 6);
        String deptId = "dept-outbox-" + suffix;
        testDept = new Department(deptId, "OB-" + suffix, "Outbox Department " + suffix, "Desc", "Wing B");
        departmentRepository.save(testDept);

        testPatient = new Patient(
                UUID.randomUUID().toString(), "CF-OB-" + suffix, "Outbox", "Patient",
                LocalDate.of(1990, 1, 1), Gender.FEMALE, "+1-555-" + suffix
        );
        testPatient.setEmail("outbox." + suffix + "@careflow.local");
        patientRepository.save(testPatient);

        String docId = "doc-ob-" + suffix;
        testDoctor = new StaffMember(
                docId, "DOC-" + suffix, deptId, "Doctor", "Outbox",
                "doctor." + suffix + "@careflow.local", "+1-555-" + suffix, StaffType.DOCTOR, LocalDate.of(2020, 1, 1)
        );
        DoctorProfile profile = new DoctorProfile(
                UUID.randomUUID().toString(), "Cardiology", "MD Cardiology", "LIC-" + suffix,
                new BigDecimal("150.00"), "Room 101", "Outbox specialist"
        );
        testDoctor.setDoctorProfile(profile);
        staffMemberRepository.save(testDoctor);
    }

    @Test
    @DisplayName("Should atomically write AppointmentBookedEvent to outbox table when booking appointment")
    void shouldAtomicallyWriteBookedEventToOutbox() {
        LocalDateTime appointmentTime = LocalDateTime.now().plusDays(2).withHour(10).withMinute(0).withSecond(0).withNano(0);
        BookAppointmentRequest request = new BookAppointmentRequest(
                testPatient.getId(),
                testDoctor.getId(),
                testDept.getId(),
                appointmentTime,
                30,
                "Routine checkup"
        );

        AppointmentResponse response = appointmentService.bookAppointment(request);
        assertThat(response).isNotNull();

        List<OutboxEvent> outboxEvents = outboxEventRepository.findAll();
        assertThat(outboxEvents).hasSize(1);

        OutboxEvent event = outboxEvents.getFirst();
        assertThat(event.getAggregateType()).isEqualTo("APPOINTMENT");
        assertThat(event.getAggregateId()).isEqualTo(response.id());
        assertThat(event.getEventType()).isEqualTo("AppointmentBooked");
        assertThat(event.getTopic()).isEqualTo(KafkaConfig.TOPIC_APPOINTMENTS);
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(event.getPayload()).contains(response.id()).contains(testPatient.getId());
    }

    @Test
    @DisplayName("Should atomically write AppointmentCancelledEvent to outbox when appointment is cancelled")
    void shouldAtomicallyWriteCancelledEventToOutbox() {
        LocalDateTime appointmentTime = LocalDateTime.now().plusDays(3).withHour(14).withMinute(0).withSecond(0).withNano(0);
        BookAppointmentRequest bookRequest = new BookAppointmentRequest(
                testPatient.getId(),
                testDoctor.getId(),
                testDept.getId(),
                appointmentTime,
                30,
                "Consultation"
        );
        AppointmentResponse booked = appointmentService.bookAppointment(bookRequest);

        // Cancel the appointment
        CancelAppointmentRequest cancelRequest = new CancelAppointmentRequest("Patient schedule conflict");
        appointmentService.cancelAppointment(booked.id(), cancelRequest);

        List<OutboxEvent> outboxEvents = outboxEventRepository.findAll();
        assertThat(outboxEvents).hasSize(2);

        OutboxEvent cancelEvent = outboxEvents.stream()
                .filter(e -> "AppointmentCancelled".equals(e.getEventType()))
                .findFirst()
                .orElseThrow();

        assertThat(cancelEvent.getAggregateId()).isEqualTo(booked.id());
        assertThat(cancelEvent.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(cancelEvent.getPayload()).contains("Patient schedule conflict");
    }
}
