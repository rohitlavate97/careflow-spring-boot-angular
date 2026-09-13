package com.careflow.appointment.service;

import com.careflow.appointment.domain.Appointment;
import com.careflow.appointment.dto.BookAppointmentRequest;
import com.careflow.appointment.exception.DoubleBookingException;
import com.careflow.appointment.repository.AppointmentRepository;
import com.careflow.department.domain.Department;
import com.careflow.department.repository.DepartmentRepository;
import com.careflow.patient.domain.Gender;
import com.careflow.patient.domain.Patient;
import com.careflow.patient.repository.PatientRepository;
import com.careflow.staff.domain.DoctorProfile;
import com.careflow.staff.domain.StaffMember;
import com.careflow.staff.domain.StaffType;
import com.careflow.staff.repository.StaffMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Concurrency Lab 1: Double-Booking Prevention Integration Test (§20, §57, §62).
 * Verifies that under high concurrent contention (10 simultaneous threads attempting to book
 * the exact same doctor time slot), exactly ONE booking succeeds and N-1 fail with 409 Conflict.
 */
@SpringBootTest
@ActiveProfiles("test")
class AppointmentConcurrencyTest {

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private StaffMemberRepository staffMemberRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    private StaffMember doctor;
    private Department department;
    private List<Patient> patients;

    @BeforeEach
    void setUp() {
        department = new Department(
                UUID.randomUUID().toString(), "CARD-CONC", "Cardiology Concurrency", "Desc", "Building A"
        );
        departmentRepository.save(department);

        doctor = new StaffMember(
                UUID.randomUUID().toString(), "DOC-CONC-01", department.getId(),
                "Alexander", "Fleming", "fleming.conc@careflow.local", "+1-555-0621",
                StaffType.DOCTOR, LocalDate.of(2023, 1, 1)
        );
        DoctorProfile docProfile = new DoctorProfile(
                UUID.randomUUID().toString(), "Cardiology", "MD, FACC", "LIC-CONC-01",
                BigDecimal.valueOf(150), "Room 101", null
        );
        doctor.setDoctorProfile(docProfile);
        staffMemberRepository.save(doctor);

        // Pre-create 10 distinct patients for the 10 concurrent requests
        patients = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            Patient p = new Patient(
                    UUID.randomUUID().toString(), "CF-CONC-" + i, "Patient" + i, "Test",
                    LocalDate.of(1990, 1, 1), Gender.MALE, "+1-555-06" + (20 + i)
            );
            p.setEmail("patient" + i + "@test.local");
            patients.add(patientRepository.save(p));
        }
    }

    @Test
    @DisplayName("Concurrency Lab 1: 10 threads concurrently booking the same slot -> exactly 1 succeeds, 9 fail (§20, §62)")
    void concurrentBooking_shouldPreventDoubleBooking_andAllowOnlyOneSuccess() throws InterruptedException {
        int numberOfThreads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);

        // Target slot: Monday at 10:00 AM
        LocalDateTime targetSlot = LocalDateTime.now().plusDays(7).withHour(10).withMinute(0).withSecond(0).withNano(0);

        CountDownLatch readyGate = new CountDownLatch(numberOfThreads);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch finishGate = new CountDownLatch(numberOfThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);
        AtomicInteger otherErrorCount = new AtomicInteger(0);

        for (int i = 0; i < numberOfThreads; i++) {
            final Patient patient = patients.get(i);
            executor.submit(() -> {
                readyGate.countDown();
                try {
                    // All threads wait at the starting gate for simultaneous release (§62)
                    startGate.await();

                    BookAppointmentRequest request = new BookAppointmentRequest(
                            patient.getId(),
                            doctor.getId(),
                            department.getId(),
                            targetSlot,
                            30,
                            "Concurrent booking test"
                    );

                    appointmentService.bookAppointment(request);
                    successCount.incrementAndGet();
                } catch (DoubleBookingException ex) {
                    // Expected conflict exception translated from DB unique constraint or fast-path (§20, §92)
                    conflictCount.incrementAndGet();
                } catch (Exception ex) {
                    otherErrorCount.incrementAndGet();
                } finally {
                    finishGate.countDown();
                }
            });
        }

        // Wait until all threads are queued up and ready
        readyGate.await(5, TimeUnit.SECONDS);

        // Fire the starting gun!
        startGate.countDown();

        // Wait for all 10 threads to finish
        boolean completed = finishGate.await(15, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).isTrue();

        // Assert thread outcome counts (§20, §62)
        assertThat(successCount.get()).as("Exactly ONE concurrent booking must succeed").isEqualTo(1);
        assertThat(conflictCount.get()).as("Exactly 9 concurrent bookings must be rejected with DoubleBookingException").isEqualTo(9);
        assertThat(otherErrorCount.get()).as("No unexpected errors should occur").isEqualTo(0);

        // Assert database end state (§62):
        // Exactly ONE active appointment exists in the database for that doctor and time slot!
        boolean exists = appointmentRepository.existsByDoctorIdAndAppointmentDateTimeAndActiveSlotFlag(
                doctor.getId(), targetSlot, 1
        );
        assertThat(exists).isTrue();

        List<Appointment> allBookings = appointmentRepository.findByDoctorIdAndAppointmentDateTimeBetween(
                doctor.getId(), targetSlot.minusMinutes(1), targetSlot.plusMinutes(1)
        );
        assertThat(allBookings).hasSize(1);
        assertThat(allBookings.get(0).getActiveSlotFlag()).isEqualTo(1);
    }
}
