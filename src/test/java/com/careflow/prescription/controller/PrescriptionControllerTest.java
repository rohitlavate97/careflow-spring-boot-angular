package com.careflow.prescription.controller;

import com.careflow.department.domain.Department;
import com.careflow.department.repository.DepartmentRepository;
import com.careflow.patient.domain.Gender;
import com.careflow.patient.domain.Patient;
import com.careflow.patient.repository.PatientRepository;
import com.careflow.pharmacy.domain.Medication;
import com.careflow.pharmacy.domain.MedicationForm;
import com.careflow.pharmacy.repository.MedicationRepository;
import com.careflow.prescription.domain.Prescription;
import com.careflow.prescription.domain.PrescriptionItem;
import com.careflow.prescription.dto.CreatePrescriptionRequest;
import com.careflow.prescription.dto.PrescriptionItemRequest;
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
import java.util.List;
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
class PrescriptionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PrescriptionRepository prescriptionRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private StaffMemberRepository staffMemberRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private MedicationRepository medicationRepository;

    private Patient patient;
    private StaffMember doctor;
    private Medication medication;
    private Prescription prescription;

    @BeforeEach
    void setUp() {
        Department dept = new Department(
                UUID.randomUUID().toString(), "PHARM-DEPT", "Pharmacy Dept", "Dept", "Block D"
        );
        departmentRepository.save(dept);

        doctor = new StaffMember(
                UUID.randomUUID().toString(), "DOC-RX-01", dept.getId(),
                "Stephen", "Strange", "strange.rx@careflow.local", "+1-555-0601",
                StaffType.DOCTOR, LocalDate.of(2023, 1, 1)
        );
        DoctorProfile docProfile = new DoctorProfile(
                UUID.randomUUID().toString(), "Internal Medicine", "MD", "LIC-RX-01",
                BigDecimal.valueOf(180), "Room 101", null
        );
        doctor.setDoctorProfile(docProfile);
        staffMemberRepository.save(doctor);

        patient = new Patient(
                UUID.randomUUID().toString(), "CF-2026-RX", "Tony", "Stark",
                LocalDate.of(1975, 5, 29), Gender.MALE, "+1-555-0602"
        );
        patientRepository.save(patient);

        medication = new Medication(
                UUID.randomUUID().toString(), "MED-AMX-CTRL", "Amoxicillin Ctrl", "Amoxicillin",
                MedicationForm.CAPSULE, "500 mg", BigDecimal.valueOf(12.00), 20
        );
        medicationRepository.save(medication);

        prescription = new Prescription(
                UUID.randomUUID().toString(), patient.getId(), doctor.getId(), null, "Sample prescription", Instant.now()
        );
        PrescriptionItem item = new PrescriptionItem(
                UUID.randomUUID().toString(), prescription, medication.getId(), "500 mg", "Twice daily", "7 days", 14, "Take with water"
        );
        prescription.addItem(item);
        prescriptionRepository.save(prescription);
    }

    @Test
    @DisplayName("POST /api/v1/prescriptions should return 201 Created when invoked by DOCTOR")
    @WithMockUser(roles = "DOCTOR")
    void issuePrescription_asDoctor_returns201() throws Exception {
        PrescriptionItemRequest itemReq = new PrescriptionItemRequest(
                medication.getId(), "500 mg", "Twice daily", "5 days", 10, "Take after meals"
        );
        CreatePrescriptionRequest request = new CreatePrescriptionRequest(
                patient.getId(), doctor.getId(), null, "Post-consultation prescription", List.of(itemReq)
        );

        mockMvc.perform(post("/api/v1/prescriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/v1/prescriptions/")))
                .andExpect(jsonPath("$.patientId").value(patient.getId()))
                .andExpect(jsonPath("$.doctorId").value(doctor.getId()))
                .andExpect(jsonPath("$.items[0].medicationId").value(medication.getId()));
    }

    @Test
    @DisplayName("POST /api/v1/prescriptions should return 403 Forbidden when invoked by PHARMACIST")
    @WithMockUser(roles = "PHARMACIST")
    void issuePrescription_asPharmacist_returns403() throws Exception {
        PrescriptionItemRequest itemReq = new PrescriptionItemRequest(
                medication.getId(), "500 mg", "Twice daily", "5 days", 10, null
        );
        CreatePrescriptionRequest request = new CreatePrescriptionRequest(
                patient.getId(), doctor.getId(), null, null, List.of(itemReq)
        );

        mockMvc.perform(post("/api/v1/prescriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/prescriptions/{id} should return 200 OK when invoked by authorized user")
    @WithMockUser(roles = "PHARMACIST")
    void getPrescriptionById_asPharmacist_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/prescriptions/{id}", prescription.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(prescription.getId()))
                .andExpect(jsonPath("$.items[0].dosage").value("500 mg"));
    }

    @Test
    @DisplayName("GET /api/v1/prescriptions/pending should return 200 OK paginated list for PHARMACIST")
    @WithMockUser(roles = "PHARMACIST")
    void getPendingPrescriptions_asPharmacist_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/prescriptions/pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }
}
