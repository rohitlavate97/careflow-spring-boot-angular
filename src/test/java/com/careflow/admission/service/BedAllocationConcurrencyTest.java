package com.careflow.admission.service;

import com.careflow.admission.domain.Bed;
import com.careflow.admission.domain.BedStatus;
import com.careflow.admission.domain.Room;
import com.careflow.admission.domain.RoomType;
import com.careflow.admission.domain.Ward;
import com.careflow.admission.domain.WardType;
import com.careflow.admission.dto.AdmitPatientRequest;
import com.careflow.admission.exception.BedNotAvailableException;
import com.careflow.admission.repository.AdmissionRepository;
import com.careflow.admission.repository.BedRepository;
import com.careflow.admission.repository.RoomRepository;
import com.careflow.admission.repository.WardRepository;
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
 * Multi-threaded Concurrency Lab 4 testing race condition prevention in hospital bed allocation (§28, §57 Lab 4, §92).
 */
@SpringBootTest
@ActiveProfiles("test")
class BedAllocationConcurrencyTest {

    @Autowired
    private AdmissionService admissionService;

    @Autowired
    private BedRepository bedRepository;

    @Autowired
    private AdmissionRepository admissionRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private StaffMemberRepository staffMemberRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private WardRepository wardRepository;

    @Autowired
    private RoomRepository roomRepository;

    private Bed singleBed;
    private StaffMember doctor;
    private List<Patient> patients = new ArrayList<>();

    @BeforeEach
    void setUp() {
        Department dept = departmentRepository.save(new Department(
                UUID.randomUUID().toString(), "ICU-CONC-" + UUID.randomUUID().toString().substring(0, 4),
                "Critical Care Conc", "ICU Dept", "Floor 1"
        ));

        doctor = new StaffMember(
                UUID.randomUUID().toString(), "DOC-CONC-4-" + UUID.randomUUID().toString().substring(0, 4),
                dept.getId(), "William", "Osler", "osler@careflow.local", "+1-555-0701",
                StaffType.DOCTOR, LocalDate.now()
        );
        DoctorProfile docProfile = new DoctorProfile(
                UUID.randomUUID().toString(), "Critical Care Medicine", "MD",
                "LIC-CONC-4-" + UUID.randomUUID().toString().substring(0, 4),
                BigDecimal.valueOf(250.00), "ICU Bay 1", "Intensivist"
        );
        doctor.setDoctorProfile(docProfile);
        doctor = staffMemberRepository.save(doctor);

        Ward ward = wardRepository.save(new Ward(
                UUID.randomUUID().toString(), "WARD-ICU-CONC-" + UUID.randomUUID().toString().substring(0, 4),
                "ICU Concurrency Ward", dept.getId(), WardType.ICU, "Floor 1", 1, true
        ));

        Room room = roomRepository.save(new Room(
                UUID.randomUUID().toString(), "ICU-C-101", ward, RoomType.ISOLATION, true
        ));

        singleBed = bedRepository.save(new Bed(
                UUID.randomUUID().toString(), "BED-ICU-SINGLE", room, BedStatus.AVAILABLE, BigDecimal.valueOf(600.00), true
        ));

        patients.clear();
        for (int i = 1; i <= 5; i++) {
            Patient p = patientRepository.save(new Patient(
                    UUID.randomUUID().toString(), "MRN-CONC-4-" + i + "-" + UUID.randomUUID().toString().substring(0, 4),
                    "Patient" + i, "StressTest", LocalDate.of(1980 + i, 1, 1), Gender.OTHER,
                    "+1-555-080" + i
            ));
            patients.add(p);
        }
    }

    @Test
    @DisplayName("Concurrency Lab 4: 5 threads compete for 1 available bed -> exactly 1 succeeds, 4 fail with BedNotAvailableException")
    void concurrentBedAllocation_raceConditionPrevented() throws InterruptedException {
        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);
        AtomicInteger otherErrorCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final Patient patientForThread = patients.get(i);
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    admissionService.admitPatient(new AdmitPatientRequest(
                            patientForThread.getId(),
                            doctor.getId(),
                            singleBed.getId(),
                            null,
                            "Emergency critical admission",
                            "Sepsis"
                    ));
                    successCount.incrementAndGet();
                } catch (BedNotAvailableException ex) {
                    conflictCount.incrementAndGet();
                } catch (Exception ex) {
                    otherErrorCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown(); // Fire all 5 threads simultaneously
        doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        // 1. Exactly 1 thread successfully admitted patient to bed
        assertThat(successCount.get()).isEqualTo(1);

        // 2. Exactly 4 threads failed with BedNotAvailableException (409 Conflict)
        assertThat(conflictCount.get()).isEqualTo(4);
        assertThat(otherErrorCount.get()).isEqualTo(0);

        // 3. Database state invariant: Bed is strictly OCCUPIED
        Bed reloadedBed = bedRepository.findById(singleBed.getId()).orElseThrow();
        assertThat(reloadedBed.getStatus()).isEqualTo(BedStatus.OCCUPIED);
    }
}
