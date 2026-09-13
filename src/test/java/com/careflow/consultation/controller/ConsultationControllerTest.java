package com.careflow.consultation.controller;

import com.careflow.consultation.domain.Consultation;
import com.careflow.consultation.domain.ConsultationDiagnosis;
import com.careflow.consultation.domain.ConsultationStatus;
import com.careflow.consultation.domain.DiagnosisType;
import com.careflow.consultation.dto.AddDiagnosisRequest;
import com.careflow.consultation.dto.CreateConsultationRequest;
import com.careflow.consultation.dto.RecordVitalsRequest;
import com.careflow.consultation.dto.UpdateConsultationRequest;
import com.careflow.consultation.repository.ConsultationRepository;
import com.careflow.department.domain.Department;
import com.careflow.department.repository.DepartmentRepository;
import com.careflow.patient.domain.Gender;
import com.careflow.patient.domain.Patient;
import com.careflow.patient.repository.PatientRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ConsultationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ConsultationRepository consultationRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private StaffMemberRepository staffMemberRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    private Patient seededPatient;
    private StaffMember seededDoctor;
    private Consultation seededConsultation;

    @BeforeEach
    void setUp() {
        Department department = new Department(
                UUID.randomUUID().toString(), "GEN-MED", "General Medicine", "Outpatient clinic", "Block B"
        );
        departmentRepository.save(department);

        seededDoctor = new StaffMember(
                UUID.randomUUID().toString(), "DOC-CONS-01", department.getId(),
                "Leonard", "McCoy", "mccoy.cons@careflow.local", "+1-555-0811",
                StaffType.DOCTOR, LocalDate.of(2022, 5, 1)
        );
        DoctorProfile docProfile = new DoctorProfile(
                UUID.randomUUID().toString(), "General Medicine", "MD", "LIC-CONS-01",
                BigDecimal.valueOf(150), "Room 302", null
        );
        seededDoctor.setDoctorProfile(docProfile);
        staffMemberRepository.save(seededDoctor);

        seededPatient = new Patient(
                UUID.randomUUID().toString(), "CF-2026-CONS", "James", "Kirk",
                LocalDate.of(1985, 3, 22), Gender.MALE, "+1-555-0812"
        );
        patientRepository.save(seededPatient);

        seededConsultation = new Consultation(
                UUID.randomUUID().toString(),
                seededPatient.getId(),
                seededDoctor.getId(),
                null,
                null,
                Instant.now()
        );
        seededConsultation.setChiefComplaint("Persistent headache and dizziness");
        consultationRepository.save(seededConsultation);
    }

    @Test
    @DisplayName("POST /api/v1/consultations should return 201 Created when invoked by DOCTOR")
    @WithMockUser(roles = "DOCTOR")
    void startConsultation_asDoctor_returns201() throws Exception {
        CreateConsultationRequest request = new CreateConsultationRequest(
                seededPatient.getId(),
                seededDoctor.getId(),
                null,
                null,
                "Routine health checkup"
        );

        mockMvc.perform(post("/api/v1/consultations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/v1/consultations/")))
                .andExpect(jsonPath("$.patientId").value(seededPatient.getId()))
                .andExpect(jsonPath("$.doctorId").value(seededDoctor.getId()))
                .andExpect(jsonPath("$.status").value(ConsultationStatus.STARTED.name()));
    }

    @Test
    @DisplayName("POST /api/v1/consultations should return 403 Forbidden when invoked by unauthorized role (RECEPTIONIST)")
    @WithMockUser(roles = "RECEPTIONIST")
    void startConsultation_asReceptionist_returns403() throws Exception {
        CreateConsultationRequest request = new CreateConsultationRequest(
                seededPatient.getId(),
                seededDoctor.getId(),
                null,
                null,
                "Checkup"
        );

        mockMvc.perform(post("/api/v1/consultations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/consultations/{id} should return 200 OK when invoked by NURSE")
    @WithMockUser(roles = "NURSE")
    void getConsultationById_asNurse_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/consultations/{id}", seededConsultation.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(seededConsultation.getId()))
                .andExpect(jsonPath("$.chiefComplaint").value("Persistent headache and dizziness"));
    }

    @Test
    @DisplayName("POST /api/v1/consultations/{id}/vitals should return 200 OK when invoked by NURSE")
    @WithMockUser(roles = "NURSE")
    void recordVitals_asNurse_returns200() throws Exception {
        RecordVitalsRequest vitals = new RecordVitalsRequest(
                120, 80, 72, 16,
                new BigDecimal("36.6"),
                99,
                new BigDecimal("178.0"),
                new BigDecimal("74.50")
        );

        mockMvc.perform(post("/api/v1/consultations/{id}/vitals", seededConsultation.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vitals)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(ConsultationStatus.IN_PROGRESS.name()))
                .andExpect(jsonPath("$.vitals.systolicBp").value(120))
                .andExpect(jsonPath("$.vitals.diastolicBp").value(80))
                .andExpect(jsonPath("$.vitals.bmi").value(23.5));
    }

    @Test
    @DisplayName("POST /api/v1/consultations/{id}/diagnoses should return 201 Created when invoked by DOCTOR")
    @WithMockUser(roles = "DOCTOR")
    void addDiagnosis_asDoctor_returns200() throws Exception {
        AddDiagnosisRequest diagnosis = new AddDiagnosisRequest(
                "G44.2", "Tension-type headache", DiagnosisType.PRIMARY, "MILD", "Stress related"
        );

        mockMvc.perform(post("/api/v1/consultations/{id}/diagnoses", seededConsultation.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(diagnosis)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.diagnoses[0].diagnosisCode").value("G44.2"))
                .andExpect(jsonPath("$.diagnoses[0].diagnosisType").value(DiagnosisType.PRIMARY.name()));
    }

    @Test
    @DisplayName("POST /api/v1/consultations/{id}/complete should return 200 OK after diagnosis is recorded")
    @WithMockUser(roles = "DOCTOR")
    void completeConsultation_asDoctor_returns200() throws Exception {
        // First add a diagnosis so completion rule passes
        ConsultationDiagnosis diagnosis = new ConsultationDiagnosis(
                UUID.randomUUID().toString(),
                seededConsultation,
                "G44.2",
                "Tension-type headache",
                DiagnosisType.PRIMARY,
                "MILD",
                "Rest prescribed"
        );
        seededConsultation.addDiagnosis(diagnosis);
        consultationRepository.save(seededConsultation);

        mockMvc.perform(post("/api/v1/consultations/{id}/complete", seededConsultation.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(ConsultationStatus.COMPLETED.name()))
                .andExpect(jsonPath("$.completedAt").isNotEmpty());
    }

    @Test
    @DisplayName("GET /api/v1/consultations/{id} should return 401 Unauthorized for unauthenticated caller")
    void getConsultationById_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/consultations/{id}", seededConsultation.getId()))
                .andExpect(status().isUnauthorized());
    }
}
