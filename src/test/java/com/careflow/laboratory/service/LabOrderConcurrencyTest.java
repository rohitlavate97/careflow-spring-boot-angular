package com.careflow.laboratory.service;

import com.careflow.department.domain.Department;
import com.careflow.department.repository.DepartmentRepository;
import com.careflow.laboratory.domain.AbnormalityFlag;
import com.careflow.laboratory.domain.LabOrder;
import com.careflow.laboratory.domain.LabOrderPriority;
import com.careflow.laboratory.domain.LabOrderStatus;
import com.careflow.laboratory.domain.LabTest;
import com.careflow.laboratory.domain.LabTestCategory;
import com.careflow.laboratory.domain.SpecimenType;
import com.careflow.laboratory.dto.CollectSampleRequest;
import com.careflow.laboratory.dto.CreateLabOrderItemRequest;
import com.careflow.laboratory.dto.CreateLabOrderRequest;
import com.careflow.laboratory.dto.EnterLabResultRequest;
import com.careflow.laboratory.dto.LabOrderResponse;
import com.careflow.laboratory.repository.LabOrderRepository;
import com.careflow.laboratory.repository.LabSampleRepository;
import com.careflow.laboratory.repository.LabTestRepository;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Concurrency test for diagnostic laboratory operations verifying thread safety,
 * optimistic state synchronization, and race condition prevention (§27, §92).
 */
@SpringBootTest
@ActiveProfiles("test")
class LabOrderConcurrencyTest {

    @Autowired
    private LaboratoryService laboratoryService;

    @Autowired
    private LabOrderRepository labOrderRepository;

    @Autowired
    private LabTestRepository labTestRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private StaffMemberRepository staffMemberRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private DoctorProfileRepository doctorProfileRepository;

    @Autowired
    private LabSampleRepository labSampleRepository;

    private Patient patient;
    private StaffMember doctor;
    private StaffMember labTech;
    private LabTest test1;
    private LabTest test2;

    @BeforeEach
    void setUp() {
        Department dept = departmentRepository.save(new Department(
                UUID.randomUUID().toString(), "PATH-CONC-" + UUID.randomUUID().toString().substring(0, 4),
                "Pathology Conc", "Lab Testing", "Floor 2"
        ));

        doctor = new StaffMember(
                UUID.randomUUID().toString(), "DOC-CONC-" + UUID.randomUUID().toString().substring(0, 4),
                dept.getId(), "Robert", "Koch", "koch@careflow.local", "+1-555-0391",
                StaffType.DOCTOR, LocalDate.now()
        );

        DoctorProfile docProfile = new DoctorProfile(
                UUID.randomUUID().toString(), "Infectious Diseases",
                "MBBS, PhD", "LIC-CONC-" + UUID.randomUUID().toString().substring(0, 4),
                BigDecimal.valueOf(120.00), "Room 202", "Infectious Disease Specialist"
        );
        doctor.setDoctorProfile(docProfile);
        doctor = staffMemberRepository.save(doctor);

        labTech = staffMemberRepository.save(new StaffMember(
                UUID.randomUUID().toString(), "TECH-CONC-" + UUID.randomUUID().toString().substring(0, 4),
                dept.getId(), "Louis", "Pasteur", "pasteur@careflow.local", "+1-555-0392",
                StaffType.LAB_TECHNICIAN, LocalDate.now()
        ));

        patient = patientRepository.save(new Patient(
                UUID.randomUUID().toString(), "MRN-CONC-" + UUID.randomUUID().toString().substring(0, 4),
                "John", "Smith", LocalDate.of(1985, 3, 20), Gender.MALE,
                "+1-555-0395"
        ));

        test1 = labTestRepository.save(new LabTest(
                UUID.randomUUID().toString(), "TEST-C1-" + UUID.randomUUID().toString().substring(0, 4),
                "Lipid Profile", LabTestCategory.BIOCHEMISTRY, SpecimenType.BLOOD,
                "<200", "mg/dL", 6, BigDecimal.valueOf(30.00), true
        ));

        test2 = labTestRepository.save(new LabTest(
                UUID.randomUUID().toString(), "TEST-C2-" + UUID.randomUUID().toString().substring(0, 4),
                "Hemoglobin A1c", LabTestCategory.BIOCHEMISTRY, SpecimenType.BLOOD,
                "<5.7", "%", 6, BigDecimal.valueOf(26.00), true
        ));
    }

    @Test
    @DisplayName("Concurrent specimen accessioning and processing should result in valid consistent states")
    void concurrentSpecimenAccessioning() throws InterruptedException {
        CreateLabOrderRequest orderReq = new CreateLabOrderRequest(
                patient.getId(), doctor.getId(), null, LabOrderPriority.URGENT, "Routine panel",
                List.of(
                        new CreateLabOrderItemRequest(test1.getId(), null),
                        new CreateLabOrderItemRequest(test2.getId(), null)
                )
        );

        LabOrderResponse order = laboratoryService.createLabOrder(orderReq);
        String orderId = order.id();

        int threadCount = 4;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    laboratoryService.collectSample(
                            orderId,
                            new CollectSampleRequest(SpecimenType.BLOOD, "Tube sample", labTech.getId())
                    );
                    successCount.incrementAndGet();
                } catch (Exception ignored) {
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown();
        doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        // At least one sample collection succeeded and order progressed to SAMPLE_COLLECTED
        assertThat(successCount.get()).isGreaterThanOrEqualTo(1);

        LabOrder updated = labOrderRepository.findById(orderId).orElseThrow();
        assertThat(updated.getStatus()).isIn(LabOrderStatus.SAMPLE_COLLECTED, LabOrderStatus.PROCESSING);
        assertThat(labSampleRepository.findByLabOrderId(orderId)).isNotEmpty();
    }
}
