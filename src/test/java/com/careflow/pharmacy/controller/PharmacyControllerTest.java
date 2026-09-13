package com.careflow.pharmacy.controller;

import com.careflow.department.domain.Department;
import com.careflow.department.repository.DepartmentRepository;
import com.careflow.patient.domain.Gender;
import com.careflow.patient.domain.Patient;
import com.careflow.patient.repository.PatientRepository;
import com.careflow.pharmacy.domain.Medication;
import com.careflow.pharmacy.domain.MedicationForm;
import com.careflow.pharmacy.domain.PharmacyInventoryBatch;
import com.careflow.pharmacy.dto.AddInventoryBatchRequest;
import com.careflow.pharmacy.dto.CreateMedicationRequest;
import com.careflow.pharmacy.dto.DispenseMedicationRequest;
import com.careflow.pharmacy.repository.MedicationRepository;
import com.careflow.pharmacy.repository.PharmacyInventoryBatchRepository;
import com.careflow.prescription.domain.Prescription;
import com.careflow.prescription.domain.PrescriptionItem;
import com.careflow.prescription.repository.PrescriptionRepository;
import com.careflow.staff.domain.DoctorProfile;
import com.careflow.staff.domain.StaffMember;
import com.careflow.staff.domain.StaffType;
import com.careflow.staff.repository.StaffMemberRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PharmacyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MedicationRepository medicationRepository;

    @Autowired
    private PharmacyInventoryBatchRepository batchRepository;

    @Autowired
    private PrescriptionRepository prescriptionRepository;

    @Autowired
    private StaffMemberRepository staffMemberRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    private StaffMember pharmacist;
    private StaffMember doctor;
    private Medication medication;
    private PharmacyInventoryBatch batch;
    private Prescription prescription;
    private PrescriptionItem prescriptionItem;

    @BeforeEach
    void setUp() {
        Department dept = new Department(
                UUID.randomUUID().toString(), "PHARM-CTRL", "Pharmacy Ctrl", "Dept", "Block E"
        );
        departmentRepository.save(dept);

        pharmacist = new StaffMember(
                UUID.randomUUID().toString(), "PHARM-CTRL-01", dept.getId(),
                "Donna", "Noble", "donna.ctrl@careflow.local", "+1-555-0301",
                StaffType.PHARMACIST, LocalDate.of(2023, 1, 1)
        );
        staffMemberRepository.save(pharmacist);

        doctor = new StaffMember(
                UUID.randomUUID().toString(), "DOC-PH-01", dept.getId(),
                "Stephen", "Strange", "strange.ph@careflow.local", "+1-555-0302",
                StaffType.DOCTOR, LocalDate.of(2023, 1, 1)
        );
        DoctorProfile docProfile = new DoctorProfile(
                UUID.randomUUID().toString(), "Internal Medicine", "MD", "LIC-PH-01",
                BigDecimal.valueOf(180), "Room 102", null
        );
        doctor.setDoctorProfile(docProfile);
        staffMemberRepository.save(doctor);

        Patient patient = new Patient(
                UUID.randomUUID().toString(), "CF-2026-PH", "Wanda", "Maximoff",
                LocalDate.of(1989, 2, 10), Gender.FEMALE, "+1-555-0303"
        );
        patientRepository.save(patient);

        medication = new Medication(
                UUID.randomUUID().toString(), "MED-IBU-400", "Ibuprofen Ctrl", "Ibuprofen",
                MedicationForm.TABLET, "400 mg", BigDecimal.valueOf(8.50), 25
        );
        medicationRepository.save(medication);

        batch = new PharmacyInventoryBatch(
                UUID.randomUUID().toString(), medication.getId(), "IBU-2026-01", LocalDate.now().plusMonths(12), 100, 20
        );
        batchRepository.save(batch);

        prescription = new Prescription(
                UUID.randomUUID().toString(), patient.getId(), doctor.getId(), null, "Pain relief", Instant.now()
        );
        prescriptionItem = new PrescriptionItem(
                UUID.randomUUID().toString(), prescription, medication.getId(), "400 mg", "Every 8 hours", "3 days", 9, null
        );
        prescription.addItem(prescriptionItem);
        prescriptionRepository.save(prescription);
    }

    @Test
    @DisplayName("POST /api/v1/pharmacy/medications should return 201 Created when invoked by PHARMACIST")
    @WithMockUser(roles = "PHARMACIST")
    void registerMedication_asPharmacist_returns201() throws Exception {
        CreateMedicationRequest request = new CreateMedicationRequest(
                "MED-ASP-100", "Aspirin", "Acetylsalicylic Acid", MedicationForm.TABLET, "100 mg", BigDecimal.valueOf(6.00), 20
        );

        mockMvc.perform(post("/api/v1/pharmacy/medications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/v1/pharmacy/medications/")))
                .andExpect(jsonPath("$.code").value("MED-ASP-100"))
                .andExpect(jsonPath("$.name").value("Aspirin"));
    }

    @Test
    @DisplayName("POST /api/v1/pharmacy/batches should return 201 Created when adding an inventory batch")
    @WithMockUser(roles = "PHARMACIST")
    void addInventoryBatch_asPharmacist_returns201() throws Exception {
        AddInventoryBatchRequest request = new AddInventoryBatchRequest(
                medication.getId(), "BATCH-IBU-NEW", LocalDate.now().plusMonths(18), 200, 30
        );

        mockMvc.perform(post("/api/v1/pharmacy/batches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/v1/pharmacy/batches/")))
                .andExpect(jsonPath("$.batchNumber").value("BATCH-IBU-NEW"))
                .andExpect(jsonPath("$.quantityAvailable").value(200));
    }

    @Test
    @DisplayName("POST /api/v1/pharmacy/dispense should return 200 OK when dispensing medication")
    @WithMockUser(roles = "PHARMACIST")
    void dispenseMedication_asPharmacist_returns200() throws Exception {
        DispenseMedicationRequest request = new DispenseMedicationRequest(
                prescription.getId(), prescriptionItem.getId(), batch.getId(), pharmacist.getId(), 9, "Dispensed full course"
        );

        mockMvc.perform(post("/api/v1/pharmacy/dispense")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantityDispensed").value(9))
                .andExpect(jsonPath("$.prescriptionId").value(prescription.getId()));
    }

    @Test
    @DisplayName("POST /api/v1/pharmacy/dispense should return 403 Forbidden when invoked by DOCTOR")
    @WithMockUser(roles = "DOCTOR")
    void dispenseMedication_asDoctor_returns403() throws Exception {
        DispenseMedicationRequest request = new DispenseMedicationRequest(
                prescription.getId(), prescriptionItem.getId(), batch.getId(), doctor.getId(), 9, null
        );

        mockMvc.perform(post("/api/v1/pharmacy/dispense")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
