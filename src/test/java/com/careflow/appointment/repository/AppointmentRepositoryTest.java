package com.careflow.appointment.repository;

import com.careflow.appointment.domain.Appointment;
import com.careflow.appointment.domain.AppointmentStatus;
import com.careflow.common.config.JpaConfig;
import com.careflow.department.domain.Department;
import com.careflow.department.repository.DepartmentRepository;
import com.careflow.patient.domain.Gender;
import com.careflow.patient.domain.Patient;
import com.careflow.patient.repository.PatientRepository;
import com.careflow.staff.domain.StaffMember;
import com.careflow.staff.domain.StaffType;
import com.careflow.staff.repository.StaffMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaConfig.class)
class AppointmentRepositoryTest {

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private StaffMemberRepository staffMemberRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Patient testPatient1;
    private Patient testPatient2;
    private StaffMember testDoctor;
    private Department testDepartment;

    @BeforeEach
    void setUp() {
        testDepartment = new Department(
                UUID.randomUUID().toString(), "CARD-APT", "Cardiology Apt", "Desc", "Building A"
        );
        departmentRepository.save(testDepartment);

        testDoctor = new StaffMember(
                UUID.randomUUID().toString(), "DOC-APT-01", testDepartment.getId(),
                "Gregory", "House", "house.apt@careflow.local", "+1-555-0321",
                StaffType.DOCTOR, LocalDate.of(2023, 1, 1)
        );
        staffMemberRepository.save(testDoctor);

        testPatient1 = new Patient(
                UUID.randomUUID().toString(), "CF-2026-0001", "John", "Doe",
                LocalDate.of(1985, 5, 20), Gender.MALE, "+1-555-0322"
        );
        testPatient1.setEmail("john.doe@test.local");
        patientRepository.save(testPatient1);

        testPatient2 = new Patient(
                UUID.randomUUID().toString(), "CF-2026-0002", "Jane", "Smith",
                LocalDate.of(1990, 8, 15), Gender.FEMALE, "+1-555-0323"
        );
        testPatient2.setEmail("jane.smith@test.local");
        patientRepository.save(testPatient2);

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("save should persist appointment with activeSlotFlag and audit metadata (§11, §19)")
    void save_shouldPersistAppointment() {
        LocalDateTime time = LocalDateTime.now().plusDays(5).withHour(10).withMinute(0).withSecond(0).withNano(0);
        Appointment appointment = new Appointment(
                UUID.randomUUID().toString(),
                testPatient1.getId(),
                testDoctor.getId(),
                testDepartment.getId(),
                time,
                30,
                "General consultation"
        );

        Appointment saved = appointmentRepository.save(appointment);
        entityManager.flush();
        entityManager.clear();

        Appointment found = appointmentRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getPatientId()).isEqualTo(testPatient1.getId());
        assertThat(found.getDoctorId()).isEqualTo(testDoctor.getId());
        assertThat(found.getActiveSlotFlag()).isEqualTo(1);
        assertThat(found.getVersion()).isEqualTo(0L);
        assertThat(found.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("uk_active_appointment_slot unique constraint should prevent duplicate active bookings (§20, §92)")
    void save_shouldThrowDataIntegrityViolation_whenSlotDoubleBooked() {
        LocalDateTime slotTime = LocalDateTime.now().plusDays(5).withHour(10).withMinute(0).withSecond(0).withNano(0);

        Appointment appt1 = new Appointment(
                UUID.randomUUID().toString(),
                testPatient1.getId(),
                testDoctor.getId(),
                testDepartment.getId(),
                slotTime,
                30,
                "First booking"
        );
        appointmentRepository.save(appt1);
        entityManager.flush();

        Appointment appt2 = new Appointment(
                UUID.randomUUID().toString(),
                testPatient2.getId(),
                testDoctor.getId(),
                testDepartment.getId(),
                slotTime,
                30,
                "Second concurrent booking"
        );

        assertThatThrownBy(() -> appointmentRepository.saveAndFlush(appt2))
                .satisfies(ex -> assertThat(
                        ex instanceof DataIntegrityViolationException ||
                        ex instanceof org.hibernate.exception.ConstraintViolationException ||
                        ex instanceof jakarta.persistence.PersistenceException
                ).isTrue());
    }

    @Test
    @DisplayName("Cancelled appointment with activeSlotFlag = NULL should allow slot to be rebooked (§20)")
    void cancelledAppointment_shouldAllowNewBookingForSameSlot() {
        LocalDateTime slotTime = LocalDateTime.now().plusDays(5).withHour(10).withMinute(0).withSecond(0).withNano(0);

        Appointment appt1 = new Appointment(
                UUID.randomUUID().toString(),
                testPatient1.getId(),
                testDoctor.getId(),
                testDepartment.getId(),
                slotTime,
                30,
                "First booking"
        );
        appointmentRepository.save(appt1);
        entityManager.flush();

        // Patient 1 cancels the appointment -> activeSlotFlag set to NULL
        appt1.transitionTo(AppointmentStatus.CANCELLED, "Cannot make it");
        appointmentRepository.save(appt1);
        entityManager.flush();

        // Patient 2 now books the exact same slot -> should succeed!
        Appointment appt2 = new Appointment(
                UUID.randomUUID().toString(),
                testPatient2.getId(),
                testDoctor.getId(),
                testDepartment.getId(),
                slotTime,
                30,
                "Rebooking after cancellation"
        );
        Appointment saved2 = appointmentRepository.save(appt2);
        entityManager.flush();

        assertThat(saved2.getId()).isNotNull();
        assertThat(saved2.getActiveSlotFlag()).isEqualTo(1);
    }
}
