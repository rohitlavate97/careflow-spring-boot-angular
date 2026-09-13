package com.careflow.queue.controller;

import com.careflow.department.domain.Department;
import com.careflow.department.repository.DepartmentRepository;
import com.careflow.patient.domain.Gender;
import com.careflow.patient.domain.Patient;
import com.careflow.patient.repository.PatientRepository;
import com.careflow.queue.domain.QueueEntry;
import com.careflow.queue.domain.QueuePriority;
import com.careflow.queue.domain.QueueStatus;
import com.careflow.queue.dto.EnqueuePatientRequest;
import com.careflow.queue.repository.QueueEntryRepository;
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
import java.util.Map;
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
class QueueControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private StaffMemberRepository staffMemberRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private QueueEntryRepository queueEntryRepository;

    private Department seededDept;
    private StaffMember seededDoctor;
    private Patient seededPatient;
    private QueueEntry seededQueueEntry;

    @BeforeEach
    void setUp() {
        seededDept = new Department(
                UUID.randomUUID().toString(), "CARD-CTRL", "Cardiology Clinic", "Desc", "Floor 2"
        );
        departmentRepository.save(seededDept);

        seededDoctor = new StaffMember(
                UUID.randomUUID().toString(), "DOC-Q-01", seededDept.getId(),
                "Charles", "Xavier", "xavier@careflow.local", "+1-555-0801",
                StaffType.DOCTOR, LocalDate.of(2023, 1, 1)
        );
        DoctorProfile profile = new DoctorProfile(
                UUID.randomUUID().toString(), "Cardiology", "MD", "LIC-Q-01",
                BigDecimal.valueOf(150), "Room 201", null
        );
        seededDoctor.setDoctorProfile(profile);
        staffMemberRepository.save(seededDoctor);

        seededPatient = new Patient(
                UUID.randomUUID().toString(), "CF-Q-CTRL-01", "Tony", "Stark",
                LocalDate.of(1975, 5, 29), Gender.MALE, "+1-555-0802"
        );
        seededPatient.setEmail("tony.stark@test.local");
        patientRepository.save(seededPatient);

        seededQueueEntry = new QueueEntry(
                UUID.randomUUID().toString(),
                seededDept.getId(),
                seededDoctor.getId(),
                seededPatient.getId(),
                null,
                LocalDate.now(),
                1,
                "CARD-CTRL-001",
                QueuePriority.NORMAL,
                Instant.now(),
                "Initial consultation"
        );
        queueEntryRepository.save(seededQueueEntry);
    }

    @Test
    @DisplayName("POST /api/v1/queue/entries should return 201 Created with Location header (§21, §89)")
    @WithMockUser(roles = "RECEPTIONIST")
    void enqueuePatient_shouldReturn201() throws Exception {
        Patient newPatient = new Patient(
                UUID.randomUUID().toString(), "CF-Q-CTRL-02", "Steve", "Rogers",
                LocalDate.of(1920, 7, 4), Gender.MALE, "+1-555-0803"
        );
        patientRepository.save(newPatient);

        EnqueuePatientRequest request = new EnqueuePatientRequest(
                newPatient.getId(),
                seededDept.getId(),
                seededDoctor.getId(),
                null,
                QueuePriority.URGENT,
                "Walk-in triage"
        );

        mockMvc.perform(post("/api/v1/queue/entries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/v1/queue/entries/")))
                .andExpect(jsonPath("$.tokenDisplay").value("CARD-CTRL-002"))
                .andExpect(jsonPath("$.priority").value("URGENT"))
                .andExpect(jsonPath("$.status").value("WAITING"));
    }

    @Test
    @DisplayName("POST /api/v1/queue/entries should return 401 when unauthenticated (§44, §45)")
    void enqueuePatient_shouldReturn401_whenUnauthenticated() throws Exception {
        EnqueuePatientRequest request = new EnqueuePatientRequest(
                seededPatient.getId(), seededDept.getId(), null, null, QueuePriority.NORMAL, null
        );

        mockMvc.perform(post("/api/v1/queue/entries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/queue/entries/{id} should return ticket details and patients ahead (§21)")
    @WithMockUser(roles = "PATIENT")
    void getQueueEntry_shouldReturn200() throws Exception {
        mockMvc.perform(get("/api/v1/queue/entries/" + seededQueueEntry.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(seededQueueEntry.getId()))
                .andExpect(jsonPath("$.tokenDisplay").value("CARD-CTRL-001"))
                .andExpect(jsonPath("$.status").value("WAITING"));
    }

    @Test
    @DisplayName("POST /api/v1/queue/departments/{id}/call-next should call next waiting patient (§21)")
    @WithMockUser(roles = "DOCTOR")
    void callNextPatient_shouldReturn200() throws Exception {
        mockMvc.perform(post("/api/v1/queue/departments/" + seededDept.getId() + "/call-next")
                        .param("doctorId", seededDoctor.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(seededQueueEntry.getId()))
                .andExpect(jsonPath("$.status").value("CALLED"))
                .andExpect(jsonPath("$.doctorId").value(seededDoctor.getId()));
    }

    @Test
    @DisplayName("Queue lifecycle transitions (call -> start -> complete) should succeed (§21)")
    @WithMockUser(roles = "DOCTOR")
    void lifecycleTransitions_shouldSucceed() throws Exception {
        // 1. Call next: WAITING -> CALLED
        mockMvc.perform(post("/api/v1/queue/departments/" + seededDept.getId() + "/call-next"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CALLED"));

        // 2. Start consultation: CALLED -> IN_CONSULTATION
        mockMvc.perform(post("/api/v1/queue/entries/" + seededQueueEntry.getId() + "/start-consultation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_CONSULTATION"));

        // 3. Complete: IN_CONSULTATION -> COMPLETED
        mockMvc.perform(post("/api/v1/queue/entries/" + seededQueueEntry.getId() + "/complete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("POST /api/v1/queue/entries/{id}/skip and requeue should update states (§21)")
    @WithMockUser(roles = "RECEPTIONIST")
    void skipAndRequeue_shouldSucceed() throws Exception {
        // Must be in CALLED to skip
        seededQueueEntry.setStatus(QueueStatus.CALLED);
        queueEntryRepository.saveAndFlush(seededQueueEntry);

        // 1. Skip
        mockMvc.perform(post("/api/v1/queue/entries/" + seededQueueEntry.getId() + "/skip"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SKIPPED"));

        // 2. Requeue
        mockMvc.perform(post("/api/v1/queue/entries/" + seededQueueEntry.getId() + "/requeue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WAITING"));
    }

    @Test
    @DisplayName("POST /api/v1/queue/entries/{id}/cancel should transition to CANCELLED (§21)")
    @WithMockUser(roles = "RECEPTIONIST")
    void cancelQueueEntry_shouldReturn200() throws Exception {
        mockMvc.perform(post("/api/v1/queue/entries/" + seededQueueEntry.getId() + "/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("reason", "Patient had emergency at home"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.notes").value(containsString("Cancelled: Patient had emergency at home")));
    }

    @Test
    @DisplayName("GET /api/v1/queue/departments/{id}/live should return live monitor payload (§21)")
    @WithMockUser(roles = "RECEPTIONIST")
    void getDepartmentLiveStatus_shouldReturn200() throws Exception {
        mockMvc.perform(get("/api/v1/queue/departments/" + seededDept.getId() + "/live"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.departmentId").value(seededDept.getId()))
                .andExpect(jsonPath("$.totalWaiting").value(1))
                .andExpect(jsonPath("$.waitingList[0].tokenDisplay").value("CARD-CTRL-001"));
    }
}
