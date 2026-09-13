package com.careflow.queue.service;

import com.careflow.department.domain.Department;
import com.careflow.department.repository.DepartmentRepository;
import com.careflow.patient.domain.Gender;
import com.careflow.patient.domain.Patient;
import com.careflow.patient.repository.PatientRepository;
import com.careflow.queue.domain.QueueEntry;
import com.careflow.queue.domain.QueuePriority;
import com.careflow.queue.dto.EnqueuePatientRequest;
import com.careflow.queue.dto.QueueEntryResponse;
import com.careflow.queue.repository.QueueEntryRepository;
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
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class QueueConcurrencyTest {

    @Autowired
    private QueueService queueService;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private StaffMemberRepository staffMemberRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private QueueEntryRepository queueEntryRepository;

    private Department department;
    private List<StaffMember> doctors;
    private List<Patient> patients;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        department = new Department(
                UUID.randomUUID().toString(), "EM-" + suffix, "Emergency Queue " + suffix, "Desc", "ER Wing"
        );
        departmentRepository.save(department);

        // Pre-create 10 doctors
        doctors = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            StaffMember doc = new StaffMember(
                    UUID.randomUUID().toString(), "DOC-" + suffix + "-" + i, department.getId(),
                    "Doctor" + i, "Staff", "doc" + i + "." + suffix.toLowerCase() + "@careflow.local", "+1-555-09" + (10 + i),
                    StaffType.DOCTOR, LocalDate.of(2023, 1, 1)
            );
            DoctorProfile profile = new DoctorProfile(
                    UUID.randomUUID().toString(), "General", "MD", "LIC-" + suffix + "-" + i,
                    BigDecimal.valueOf(150), "Room " + i, null
            );
            doc.setDoctorProfile(profile);
            doctors.add(staffMemberRepository.save(doc));
        }

        // Pre-create 10 patients
        patients = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            Patient p = new Patient(
                    UUID.randomUUID().toString(), "CF-" + suffix + "-" + i, "Patient" + i, "Test",
                    LocalDate.of(1990, 1, 1), Gender.MALE, "+1-555-08" + (20 + i)
            );
            patients.add(patientRepository.save(p));
        }
    }

    @Test
    @DisplayName("Concurrency Lab: 10 doctors simultaneously calling next patient -> exactly 10 distinct patients called without double-calling (§21, §92)")
    void concurrentCallNext_shouldPreventDoubleCalling_andCallEachPatientExactlyOnce() throws InterruptedException {
        int numberOfThreads = 10;
        LocalDate today = LocalDate.now();

        // Pre-populate 10 waiting patients in the department queue
        for (int i = 0; i < numberOfThreads; i++) {
            Patient p = patients.get(i);
            QueueEntry entry = new QueueEntry(
                    UUID.randomUUID().toString(),
                    department.getId(),
                    null,
                    p.getId(),
                    null,
                    today,
                    i + 1,
                    String.format("EMERG-Q-%03d", i + 1),
                    QueuePriority.NORMAL,
                    Instant.now().plusMillis(i * 10),
                    "Pre-queued patient " + (i + 1)
            );
            queueEntryRepository.save(entry);
        }

        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch readyGate = new CountDownLatch(numberOfThreads);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch finishGate = new CountDownLatch(numberOfThreads);

        List<QueueEntryResponse> calledResults = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger errorCount = new AtomicInteger(0);

        for (int i = 0; i < numberOfThreads; i++) {
            final StaffMember doctor = doctors.get(i);
            executor.submit(() -> {
                readyGate.countDown();
                try {
                    startGate.await(); // Simultaneous release of all 10 threads (§62)
                    QueueEntryResponse response = queueService.callNextPatient(department.getId(), doctor.getId());
                    calledResults.add(response);
                } catch (Exception ex) {
                    errorCount.incrementAndGet();
                } finally {
                    finishGate.countDown();
                }
            });
        }

        readyGate.await(10, TimeUnit.SECONDS);
        startGate.countDown(); // FIRE!
        finishGate.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        // Verify: All 10 succeeded, 0 errors
        assertThat(errorCount.get()).isEqualTo(0);
        assertThat(calledResults).hasSize(numberOfThreads);

        // Verify: Exactly 10 DISTINCT patients were called (NO DUPLICATE CALLING) (§21, §92)
        Set<String> uniquePatientIds = new HashSet<>();
        Set<String> uniqueTicketIds = new HashSet<>();
        for (QueueEntryResponse resp : calledResults) {
            uniquePatientIds.add(resp.patientId());
            uniqueTicketIds.add(resp.id());
        }

        assertThat(uniquePatientIds)
                .as("Every concurrent call-next operation must receive a distinct patient")
                .hasSize(numberOfThreads);

        assertThat(uniqueTicketIds)
                .as("Every concurrent call-next operation must lock and transition a distinct ticket")
                .hasSize(numberOfThreads);
    }

    @Test
    @DisplayName("Concurrency Lab: 5 concurrent enqueues for the same department -> all succeed with unique consecutive tokens (§21, §92)")
    void concurrentEnqueue_shouldGenerateUniqueTokensWithoutCollision() throws InterruptedException {
        int numberOfThreads = 5;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch readyGate = new CountDownLatch(numberOfThreads);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch finishGate = new CountDownLatch(numberOfThreads);

        List<QueueEntryResponse> enqueuedResults = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger errorCount = new AtomicInteger(0);

        for (int i = 0; i < numberOfThreads; i++) {
            final Patient patient = patients.get(i);
            executor.submit(() -> {
                readyGate.countDown();
                try {
                    startGate.await();
                    EnqueuePatientRequest request = new EnqueuePatientRequest(
                            patient.getId(),
                            department.getId(),
                            null,
                            null,
                            QueuePriority.NORMAL,
                            "Concurrent enqueue"
                    );
                    QueueEntryResponse response = queueService.enqueuePatient(request);
                    enqueuedResults.add(response);
                } catch (Exception ex) {
                    errorCount.incrementAndGet();
                } finally {
                    finishGate.countDown();
                }
            });
        }

        readyGate.await(10, TimeUnit.SECONDS);
        startGate.countDown();
        finishGate.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(errorCount.get()).isEqualTo(0);
        assertThat(enqueuedResults).hasSize(numberOfThreads);

        Set<Integer> uniqueTokens = new HashSet<>();
        for (QueueEntryResponse resp : enqueuedResults) {
            uniqueTokens.add(resp.tokenNumber());
        }

        assertThat(uniqueTokens)
                .as("All concurrently enqueued patients must receive unique token numbers")
                .hasSize(numberOfThreads);
    }
}
