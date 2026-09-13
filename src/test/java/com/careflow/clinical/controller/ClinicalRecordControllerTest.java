package com.careflow.clinical.controller;

import com.careflow.clinical.domain.ClinicalNote;
import com.careflow.clinical.domain.NoteType;
import com.careflow.clinical.dto.CreateClinicalNoteRequest;
import com.careflow.clinical.repository.ClinicalNoteRepository;
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
class ClinicalRecordControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ClinicalNoteRepository clinicalNoteRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private StaffMemberRepository staffMemberRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    private Patient patient;
    private StaffMember doctor;
    private ClinicalNote clinicalNote;

    @BeforeEach
    void setUp() {
        Department dept = new Department(
                UUID.randomUUID().toString(), "CLIN-DEPT", "Clinical Dept", "Dept", "Wing C"
        );
        departmentRepository.save(dept);

        doctor = new StaffMember(
                UUID.randomUUID().toString(), "DOC-NOTE-01", dept.getId(),
                "Beverly", "Crusher", "crusher@careflow.local", "+1-555-0771",
                StaffType.DOCTOR, LocalDate.of(2023, 1, 1)
        );
        DoctorProfile docProfile = new DoctorProfile(
                UUID.randomUUID().toString(), "Internal Medicine", "MD", "LIC-NOTE-01",
                BigDecimal.valueOf(180), "Room 205", null
        );
        doctor.setDoctorProfile(docProfile);
        staffMemberRepository.save(doctor);

        patient = new Patient(
                UUID.randomUUID().toString(), "CF-2026-NOTE", "Jean-Luc", "Picard",
                LocalDate.of(1970, 7, 13), Gender.MALE, "+1-555-0772"
        );
        patientRepository.save(patient);

        clinicalNote = new ClinicalNote(
                UUID.randomUUID().toString(),
                patient.getId(),
                null,
                doctor.getId(),
                NoteType.SOAP_ASSESSMENT,
                "Cardiac Assessment",
                "Patient exhibits normal sinus rhythm."
        );
        clinicalNoteRepository.save(clinicalNote);
    }

    @Test
    @DisplayName("POST /api/v1/clinical-records/notes should return 201 Created when invoked by DOCTOR")
    @WithMockUser(roles = "DOCTOR")
    void createClinicalNote_asDoctor_returns201() throws Exception {
        CreateClinicalNoteRequest request = new CreateClinicalNoteRequest(
                patient.getId(),
                null,
                doctor.getId(),
                NoteType.SOAP_PLAN,
                "Treatment Plan",
                "Continue standard maintenance dose."
        );

        mockMvc.perform(post("/api/v1/clinical-records/notes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/v1/clinical-records/notes/")))
                .andExpect(jsonPath("$.patientId").value(patient.getId()))
                .andExpect(jsonPath("$.noteType").value(NoteType.SOAP_PLAN.name()))
                .andExpect(jsonPath("$.title").value("Treatment Plan"));
    }

    @Test
    @DisplayName("POST /api/v1/clinical-records/notes should return 403 Forbidden for RECEPTIONIST")
    @WithMockUser(roles = "RECEPTIONIST")
    void createClinicalNote_asReceptionist_returns403() throws Exception {
        CreateClinicalNoteRequest request = new CreateClinicalNoteRequest(
                patient.getId(),
                null,
                doctor.getId(),
                NoteType.GENERAL,
                "Note",
                "Content"
        );

        mockMvc.perform(post("/api/v1/clinical-records/notes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/clinical-records/notes/{id} should return 200 OK when invoked by NURSE")
    @WithMockUser(roles = "NURSE")
    void getClinicalNoteById_asNurse_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/clinical-records/notes/{id}", clinicalNote.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(clinicalNote.getId()))
                .andExpect(jsonPath("$.title").value("Cardiac Assessment"));
    }

    @Test
    @DisplayName("GET /api/v1/clinical-records/patient/{patientId}/notes should return 200 OK paginated list")
    @WithMockUser(roles = "DOCTOR")
    void getNotesByPatient_asDoctor_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/clinical-records/patient/{patientId}/notes", patient.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].patientId").value(patient.getId()));
    }
}
