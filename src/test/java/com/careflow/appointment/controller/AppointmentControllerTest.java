package com.careflow.appointment.controller;

import com.careflow.appointment.domain.Appointment;
import com.careflow.appointment.domain.AppointmentStatus;
import com.careflow.appointment.dto.BookAppointmentRequest;
import com.careflow.appointment.dto.CancelAppointmentRequest;
import com.careflow.appointment.dto.RescheduleAppointmentRequest;
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
import java.time.LocalDateTime;
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
class AppointmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private StaffMemberRepository staffMemberRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    private Patient seededPatient;
    private StaffMember seededDoctor;
    private Department seededDepartment;
    private Appointment seededAppointment;

    @BeforeEach
    void setUp() {
        seededDepartment = new Department(
                UUID.randomUUID().toString(), "CARD-CTRL", "Cardiology Control", "Desc", "Building A"
        );
        departmentRepository.save(seededDepartment);

        seededDoctor = new StaffMember(
                UUID.randomUUID().toString(), "DOC-CTRL-01", seededDepartment.getId(),
                "Stephen", "Strange", "strange.ctrl@careflow.local", "+1-555-0421",
                StaffType.DOCTOR, LocalDate.of(2023, 1, 1)
        );
        DoctorProfile docProfile = new DoctorProfile(
                UUID.randomUUID().toString(), "Cardiology", "MD, PhD", "LIC-CTRL-01",
                BigDecimal.valueOf(200), "Room 10", null
        );
        seededDoctor.setDoctorProfile(docProfile);
        staffMemberRepository.save(seededDoctor);

        seededPatient = new Patient(
                UUID.randomUUID().toString(), "CF-2026-CTRL", "Peter", "Parker",
                LocalDate.of(2001, 8, 10), Gender.MALE, "+1-555-0422"
        );
        seededPatient.setEmail("peter.ctrl@test.local");
        patientRepository.save(seededPatient);

        LocalDateTime aptTime = LocalDateTime.now().plusDays(4).withHour(14).withMinute(0).withSecond(0).withNano(0);
        seededAppointment = new Appointment(
                UUID.randomUUID().toString(),
                seededPatient.getId(),
                seededDoctor.getId(),
                seededDepartment.getId(),
                aptTime,
                30,
                "Follow up"
        );
        appointmentRepository.save(seededAppointment);
    }

    @Test
    @DisplayName("POST /api/v1/appointments should return 201 Created with Location header (§19, §89)")
    @WithMockUser(roles = "PATIENT")
    void bookAppointment_shouldReturn201_whenValid() throws Exception {
        LocalDateTime bookTime = LocalDateTime.now().plusDays(7).withHour(9).withMinute(0).withSecond(0).withNano(0);
        BookAppointmentRequest request = new BookAppointmentRequest(
                seededPatient.getId(),
                seededDoctor.getId(),
                seededDepartment.getId(),
                bookTime,
                30,
                "Chest pain consultation"
        );

        mockMvc.perform(post("/api/v1/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/v1/appointments/")))
                .andExpect(jsonPath("$.status").value("REQUESTED"))
                .andExpect(jsonPath("$.doctorId").value(seededDoctor.getId()))
                .andExpect(jsonPath("$.patientId").value(seededPatient.getId()));
    }

    @Test
    @DisplayName("POST /api/v1/appointments should return 409 Conflict when slot already booked (§20, §92)")
    @WithMockUser(roles = "PATIENT")
    void bookAppointment_shouldReturn409_whenDoubleBooking() throws Exception {
        // Attempting to book the exact same slot as seededAppointment
        BookAppointmentRequest duplicateRequest = new BookAppointmentRequest(
                seededPatient.getId(),
                seededDoctor.getId(),
                seededDepartment.getId(),
                seededAppointment.getAppointmentDateTime(),
                30,
                "Conflicting booking"
        );

        mockMvc.perform(post("/api/v1/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DOUBLE_BOOKING_CONFLICT"));
    }

    @Test
    @DisplayName("POST /api/v1/appointments should return 401 Unauthorized when unauthenticated (§44, §45)")
    void bookAppointment_shouldReturn401_whenUnauthenticated() throws Exception {
        BookAppointmentRequest request = new BookAppointmentRequest(
                seededPatient.getId(),
                seededDoctor.getId(),
                seededDepartment.getId(),
                LocalDateTime.now().plusDays(7).withHour(10).withMinute(0).withSecond(0).withNano(0),
                30,
                "Consultation"
        );

        mockMvc.perform(post("/api/v1/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Appointment lifecycle transition endpoints (confirm, check-in, start, complete) should transition states (§19, §69)")
    @WithMockUser(roles = {"RECEPTIONIST", "DOCTOR", "ADMIN"})
    void lifecycleTransitions_shouldSucceed() throws Exception {
        // 1. Confirm: REQUESTED -> CONFIRMED
        mockMvc.perform(post("/api/v1/appointments/" + seededAppointment.getId() + "/confirm"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        // 2. Check-in: CONFIRMED -> CHECKED_IN
        mockMvc.perform(post("/api/v1/appointments/" + seededAppointment.getId() + "/check-in"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CHECKED_IN"));

        // 3. Start: CHECKED_IN -> IN_PROGRESS
        mockMvc.perform(post("/api/v1/appointments/" + seededAppointment.getId() + "/start"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        // 4. Complete: IN_PROGRESS -> COMPLETED
        mockMvc.perform(post("/api/v1/appointments/" + seededAppointment.getId() + "/complete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("POST /api/v1/appointments/{id}/cancel should transition status to CANCELLED (§19)")
    @WithMockUser(roles = "PATIENT")
    void cancelAppointment_shouldReturn200() throws Exception {
        CancelAppointmentRequest cancelReq = new CancelAppointmentRequest("Patient feeling better");

        mockMvc.perform(post("/api/v1/appointments/" + seededAppointment.getId() + "/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cancelReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancellationReason").value("Patient feeling better"));
    }

    @Test
    @DisplayName("POST /api/v1/appointments/{id}/reschedule should update appointment date time (§19)")
    @WithMockUser(roles = "RECEPTIONIST")
    void rescheduleAppointment_shouldReturn200() throws Exception {
        LocalDateTime newTime = LocalDateTime.now().plusDays(10).withHour(11).withMinute(0).withSecond(0).withNano(0);
        RescheduleAppointmentRequest request = new RescheduleAppointmentRequest(newTime);

        mockMvc.perform(post("/api/v1/appointments/" + seededAppointment.getId() + "/reschedule")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(seededAppointment.getId()));
    }

    @Test
    @DisplayName("GET /api/v1/appointments should return paginated list (§19, §71)")
    @WithMockUser(roles = "RECEPTIONIST")
    void searchAppointments_shouldReturn200() throws Exception {
        mockMvc.perform(get("/api/v1/appointments")
                        .param("doctorId", seededDoctor.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value(seededAppointment.getId()));
    }
}
