package com.careflow.pharmacy.service;

import com.careflow.department.domain.Department;
import com.careflow.department.repository.DepartmentRepository;
import com.careflow.patient.domain.Gender;
import com.careflow.patient.domain.Patient;
import com.careflow.patient.repository.PatientRepository;
import com.careflow.pharmacy.domain.Medication;
import com.careflow.pharmacy.domain.MedicationForm;
import com.careflow.pharmacy.domain.PharmacyInventoryBatch;
import com.careflow.pharmacy.dto.DispenseMedicationRequest;
import com.careflow.pharmacy.exception.InsufficientInventoryException;
import com.careflow.pharmacy.repository.DispenseRecordRepository;
import com.careflow.pharmacy.repository.MedicationRepository;
import com.careflow.pharmacy.repository.PharmacyInventoryBatchRepository;
import com.careflow.prescription.domain.Prescription;
import com.careflow.prescription.domain.PrescriptionItem;
import com.careflow.prescription.repository.PrescriptionItemRepository;
import com.careflow.prescription.repository.PrescriptionRepository;
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
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Multi-threaded Concurrency Lab 3 testing race condition prevention in pharmacy inventory stock decrements (§26, §57 Lab 3, §92).
 */
@SpringBootTest
@ActiveProfiles("test")
class PharmacyConcurrencyTest {

    @Autowired
    private PharmacyService pharmacyService;

    @Autowired
    private PharmacyInventoryBatchRepository batchRepository;

    @Autowired
    private MedicationRepository medicationRepository;

    @Autowired
    private DispenseRecordRepository dispenseRecordRepository;

    @Autowired
    private PrescriptionRepository prescriptionRepository;

    @Autowired
    private PrescriptionItemRepository prescriptionItemRepository;

    @Autowired
    private StaffMemberRepository staffMemberRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    private List<StaffMember> pharmacists;
    private Medication medication;
    private PharmacyInventoryBatch batch;
    private List<Prescription> prescriptions;
    private List<PrescriptionItem> prescriptionItems;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        Department dept = new Department(
                UUID.randomUUID().toString(), "PH-" + suffix, "Pharmacy " + suffix, "Pharmacy unit", "Wing P"
        );
        departmentRepository.save(dept);

        StaffMember doctor = new StaffMember(
                UUID.randomUUID().toString(), "DOC-" + suffix, dept.getId(),
                "Doctor", suffix, "doc." + suffix.toLowerCase() + "@careflow.local", "+1-555-0800",
                StaffType.DOCTOR, LocalDate.of(2023, 1, 1)
        );
        DoctorProfile docProfile = new DoctorProfile(
                UUID.randomUUID().toString(), "General", "MD", "LIC-" + suffix,
                BigDecimal.valueOf(150), "Room 1", null
        );
        doctor.setDoctorProfile(docProfile);
        staffMemberRepository.save(doctor);

        // Create 5 pharmacists
        pharmacists = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            StaffMember p = new StaffMember(
                    UUID.randomUUID().toString(), "PHARM-" + suffix + "-" + i, dept.getId(),
                    "Pharmacist" + i, "Staff", "pharm" + i + "." + suffix.toLowerCase() + "@careflow.local", "+1-555-080" + i,
                    StaffType.PHARMACIST, LocalDate.of(2023, 1, 1)
            );
            pharmacists.add(staffMemberRepository.save(p));
        }

        // Create 1 high-demand medication
        medication = new Medication(
                UUID.randomUUID().toString(), "MED-CONC-" + suffix, "Rare Medicine " + suffix, "Rare Drug",
                MedicationForm.CAPSULE, "250 mg", BigDecimal.valueOf(50.00), 10
        );
        medicationRepository.save(medication);

        // Critical Concurrency Setup: Available stock = exactly 1 unit!
        batch = new PharmacyInventoryBatch(
                UUID.randomUUID().toString(), medication.getId(), "BATCH-CONC-" + suffix,
                LocalDate.now().plusMonths(12), 1, 5
        );
        batchRepository.saveAndFlush(batch);

        // Pre-create 5 patients with prescriptions for 1 unit each
        prescriptions = new ArrayList<>();
        prescriptionItems = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            Patient patient = new Patient(
                    UUID.randomUUID().toString(), "PAT-CONC-" + suffix + "-" + i, "Patient" + i, "Test",
                    LocalDate.of(1990, 1, i), Gender.MALE, "+1-555-081" + i
            );
            patientRepository.save(patient);

            Prescription rx = new Prescription(
                    UUID.randomUUID().toString(), patient.getId(), doctor.getId(), null, "Concurrency test", Instant.now()
            );
            PrescriptionItem item = new PrescriptionItem(
                    UUID.randomUUID().toString(), rx, medication.getId(), "250 mg", "Once", "1 day", 1, null
            );
            rx.addItem(item);
            prescriptionRepository.save(rx);

            prescriptions.add(rx);
            prescriptionItems.add(item);
        }
    }

    @Test
    @DisplayName("Concurrency Lab 3: 5 concurrent pharmacists attempt to dispense the final single stock unit (§26, §92)")
    void concurrentDispense_onlyOneSucceeds_zeroNegativeStock() throws InterruptedException {
        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startGate.await(); // Simultaneous unleash

                    StaffMember pharmacist = pharmacists.get(index);
                    Prescription rx = prescriptions.get(index);
                    PrescriptionItem item = prescriptionItems.get(index);

                    DispenseMedicationRequest request = new DispenseMedicationRequest(
                            rx.getId(), item.getId(), batch.getId(), pharmacist.getId(), 1, "Concurrency lab dispense"
                    );

                    pharmacyService.dispenseMedication(request);
                    successCount.incrementAndGet();
                } catch (InsufficientInventoryException ex) {
                    // Correctly rejected by pessimistic lock and stock verification
                    conflictCount.incrementAndGet();
                } catch (Exception ex) {
                    // Any other failure
                    if (ex.getCause() instanceof InsufficientInventoryException) {
                        conflictCount.incrementAndGet();
                    }
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        readyLatch.await(5, TimeUnit.SECONDS);
        startGate.countDown(); // Release all 5 threads simultaneously!
        finishLatch.await(15, TimeUnit.SECONDS);
        executor.shutdown();

        // Verification of invariants (§26, §92):
        // 1. Exactly 1 pharmacist succeeded in dispensing the last unit
        assertThat(successCount.get()).isEqualTo(1);

        // 2. The remaining 4 concurrent attempts were safely rejected with InsufficientInventoryException
        assertThat(conflictCount.get()).isEqualTo(4);

        // 3. Database batch stock must be exactly 0 (never negative)
        PharmacyInventoryBatch updatedBatch = batchRepository.findById(batch.getId()).orElseThrow();
        assertThat(updatedBatch.getQuantityAvailable()).isEqualTo(0);

        // 4. Exactly 1 dispense record must exist in the database for this batch
        long recordsForBatch = dispenseRecordRepository.findAll().stream()
                .filter(r -> r.getInventoryBatchId().equals(batch.getId()))
                .count();
        assertThat(recordsForBatch).isEqualTo(1L);
    }
}
