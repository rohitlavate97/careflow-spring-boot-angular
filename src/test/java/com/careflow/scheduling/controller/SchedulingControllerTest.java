package com.careflow.scheduling.controller;

import com.careflow.scheduling.domain.DoctorLeave;
import com.careflow.scheduling.domain.DoctorSchedule;
import com.careflow.scheduling.domain.LeaveStatus;
import com.careflow.scheduling.dto.CreateDoctorLeaveRequest;
import com.careflow.scheduling.dto.CreateDoctorScheduleRequest;
import com.careflow.scheduling.dto.UpdateDoctorLeaveStatusRequest;
import com.careflow.scheduling.dto.UpdateDoctorScheduleRequest;
import com.careflow.scheduling.repository.DoctorLeaveRepository;
import com.careflow.scheduling.repository.DoctorScheduleRepository;
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
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SchedulingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StaffMemberRepository staffMemberRepository;

    @Autowired
    private DoctorScheduleRepository doctorScheduleRepository;

    @Autowired
    private DoctorLeaveRepository doctorLeaveRepository;

    private StaffMember testDoctor;
    private DoctorSchedule seededSchedule;

    @BeforeEach
    void setUp() {
        testDoctor = new StaffMember(
                UUID.randomUUID().toString(),
                "DOC-SCHED-01",
                "dept-card-001",
                "Leonard",
                "Hofstadter",
                "leonard@careflow.local",
                "+1-555-0822",
                StaffType.DOCTOR,
                LocalDate.of(2023, 4, 1)
        );
        DoctorProfile docProfile = new DoctorProfile(
                UUID.randomUUID().toString(),
                "Cardiology",
                "MD, PhD",
                "MED-LIC-SCHED-01",
                BigDecimal.valueOf(180.00),
                "Room 102",
                "Cardiology specialist"
        );
        testDoctor.setDoctorProfile(docProfile);
        staffMemberRepository.save(testDoctor);

        seededSchedule = new DoctorSchedule(
                UUID.randomUUID().toString(),
                testDoctor.getId(),
                DayOfWeek.MONDAY,
                LocalTime.of(9, 0),
                LocalTime.of(13, 0),
                LocalTime.of(11, 0),
                LocalTime.of(11, 30),
                30
        );
        doctorScheduleRepository.save(seededSchedule);
    }

    @Test
    @DisplayName("POST /api/v1/scheduling/doctors/{doctorId}/schedules should return 201 Created for ADMIN (§18)")
    @WithMockUser(roles = "ADMIN")
    void createSchedule_shouldReturn201_forAdmin() throws Exception {
        CreateDoctorScheduleRequest request = new CreateDoctorScheduleRequest(
                DayOfWeek.TUESDAY,
                LocalTime.of(8, 0),
                LocalTime.of(12, 0),
                null,
                null,
                30
        );

        mockMvc.perform(post("/api/v1/scheduling/doctors/" + testDoctor.getId() + "/schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/v1/scheduling/schedules/")))
                .andExpect(jsonPath("$.dayOfWeek").value("TUESDAY"))
                .andExpect(jsonPath("$.startTime").value("08:00:00"))
                .andExpect(jsonPath("$.slotDurationMinutes").value(30));
    }

    @Test
    @DisplayName("POST /api/v1/scheduling/doctors/{doctorId}/schedules should return 403 Forbidden for NURSE (§44, §45)")
    @WithMockUser(roles = "NURSE")
    void createSchedule_shouldReturn403_forNurse() throws Exception {
        CreateDoctorScheduleRequest request = new CreateDoctorScheduleRequest(
                DayOfWeek.FRIDAY,
                LocalTime.of(8, 0),
                LocalTime.of(12, 0),
                null,
                null,
                30
        );

        mockMvc.perform(post("/api/v1/scheduling/doctors/" + testDoctor.getId() + "/schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/scheduling/doctors/{doctorId}/schedules should return 401 Unauthorized when unauthenticated (§44, §45)")
    void createSchedule_shouldReturn401_whenUnauthenticated() throws Exception {
        CreateDoctorScheduleRequest request = new CreateDoctorScheduleRequest(
                DayOfWeek.FRIDAY,
                LocalTime.of(8, 0),
                LocalTime.of(12, 0),
                null,
                null,
                30
        );

        mockMvc.perform(post("/api/v1/scheduling/doctors/" + testDoctor.getId() + "/schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/scheduling/doctors/{doctorId}/schedules should return 200 OK (§18)")
    @WithMockUser(roles = "RECEPTIONIST")
    void getDoctorSchedules_shouldReturn200() throws Exception {
        mockMvc.perform(get("/api/v1/scheduling/doctors/" + testDoctor.getId() + "/schedules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].dayOfWeek").value("MONDAY"));
    }

    @Test
    @DisplayName("GET /api/v1/scheduling/doctors/{doctorId}/slots should generate available slots (§18, §103 Phase 4)")
    @WithMockUser(roles = "PATIENT")
    void getAvailableSlots_shouldReturnGeneratedSlots() throws Exception {
        // 2026-09-14 is a Monday matching testDoctor's seeded schedule (09:00 - 13:00, break 11:00-11:30, slot 30m)
        mockMvc.perform(get("/api/v1/scheduling/doctors/" + testDoctor.getId() + "/slots")
                        .param("date", "2026-09-14"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(7))
                .andExpect(jsonPath("$[0].startTime").value("09:00:00"))
                .andExpect(jsonPath("$[0].endTime").value("09:30:00"))
                .andExpect(jsonPath("$[0].status").value("AVAILABLE"));
    }

    @Test
    @DisplayName("POST /api/v1/scheduling/doctors/{doctorId}/leaves and PATCH status should manage leave lifecycle (§18, §69)")
    @WithMockUser(roles = "ADMIN")
    void createAndApproveLeave_shouldSucceed() throws Exception {
        CreateDoctorLeaveRequest leaveReq = new CreateDoctorLeaveRequest(
                LocalDate.of(2026, 12, 1),
                LocalDate.of(2026, 12, 5),
                "Winter Holidays"
        );

        String responseContent = mockMvc.perform(post("/api/v1/scheduling/doctors/" + testDoctor.getId() + "/leaves")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(leaveReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn().getResponse().getContentAsString();

        DoctorLeave createdLeave = doctorLeaveRepository.findByDoctorId(testDoctor.getId()).get(0);

        UpdateDoctorLeaveStatusRequest statusReq = new UpdateDoctorLeaveStatusRequest(LeaveStatus.APPROVED);

        mockMvc.perform(patch("/api/v1/scheduling/leaves/" + createdLeave.getId() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    @DisplayName("DELETE /api/v1/scheduling/schedules/{id} should return 204 No Content for ADMIN (§18)")
    @WithMockUser(roles = "ADMIN")
    void deleteSchedule_shouldReturn204() throws Exception {
        mockMvc.perform(delete("/api/v1/scheduling/schedules/" + seededSchedule.getId()))
                .andExpect(status().isNoContent());
    }
}
