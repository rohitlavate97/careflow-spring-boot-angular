package com.careflow.staff.controller;

import com.careflow.department.domain.Department;
import com.careflow.department.repository.DepartmentRepository;
import com.careflow.staff.domain.DoctorProfile;
import com.careflow.staff.domain.StaffMember;
import com.careflow.staff.domain.StaffStatus;
import com.careflow.staff.domain.StaffType;
import com.careflow.staff.dto.CreateStaffRequest;
import com.careflow.staff.dto.DoctorProfileDto;
import com.careflow.staff.dto.UpdateStaffRequest;
import com.careflow.staff.dto.UpdateStaffStatusRequest;
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
class StaffControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StaffMemberRepository staffMemberRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    private Department seededDepartment;
    private StaffMember seededStaffDoctor;

    @BeforeEach
    void setUp() {
        seededDepartment = new Department(
                UUID.randomUUID().toString(),
                "NEPH",
                "Nephrology",
                "Renal and Kidney Care Unit",
                "Building C, Floor 3"
        );
        departmentRepository.save(seededDepartment);

        seededStaffDoctor = new StaffMember(
                UUID.randomUUID().toString(),
                "DOC-NEPH-01",
                seededDepartment.getId(),
                "Stephen",
                "Strange",
                "strange@careflow.local",
                "+1-555-0922",
                StaffType.DOCTOR,
                LocalDate.of(2023, 5, 1)
        );
        DoctorProfile docProfile = new DoctorProfile(
                UUID.randomUUID().toString(),
                "Nephrology",
                "MD, PhD",
                "MED-NEPH-999",
                BigDecimal.valueOf(250.00),
                "Room 401",
                "Senior consultant in renal replacement therapies."
        );
        seededStaffDoctor.setDoctorProfile(docProfile);
        staffMemberRepository.save(seededStaffDoctor);
    }

    @Test
    @DisplayName("POST /api/v1/staff should return 201 Created with Location header for ADMIN (§17, §89)")
    @WithMockUser(roles = "ADMIN")
    void createStaff_shouldReturn201_whenValidDoctor() throws Exception {
        DoctorProfileDto docDto = new DoctorProfileDto(
                "Nephrology",
                "MD, FASN",
                "MED-NEPH-001",
                BigDecimal.valueOf(220.00),
                "Room 402",
                "Kidney specialist"
        );
        CreateStaffRequest request = new CreateStaffRequest(
                "DOC-NEPH-02",
                null,
                seededDepartment.getId(),
                "Donald",
                "Blake",
                "blake@careflow.local",
                "+1-555-0923",
                StaffType.DOCTOR,
                LocalDate.of(2023, 6, 1),
                docDto
        );

        mockMvc.perform(post("/api/v1/staff")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/v1/staff/")))
                .andExpect(jsonPath("$.staffCode").value("DOC-NEPH-02"))
                .andExpect(jsonPath("$.staffType").value("DOCTOR"))
                .andExpect(jsonPath("$.doctorProfile.specialization").value("Nephrology"))
                .andExpect(jsonPath("$.doctorProfile.medicalLicenseNumber").value("MED-NEPH-001"));
    }

    @Test
    @DisplayName("POST /api/v1/staff should return 400 Bad Request when validation fails (§41)")
    @WithMockUser(roles = "ADMIN")
    void createStaff_shouldReturn400_whenValidationFails() throws Exception {
        CreateStaffRequest invalidRequest = new CreateStaffRequest(
                "", // Blank code
                null,
                "", // Blank department
                "", // Blank first name
                "", // Blank last name
                "invalid-email-format",
                "",
                null, // Null staff type
                LocalDate.now().plusDays(5), // Future date
                null
        );

        mockMvc.perform(post("/api/v1/staff")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.validationErrors").isArray());
    }

    @Test
    @DisplayName("POST /api/v1/staff should return 403 Forbidden for non-ADMIN role (§44, §45)")
    @WithMockUser(roles = "DOCTOR")
    void createStaff_shouldReturn403_forDoctor() throws Exception {
        CreateStaffRequest request = new CreateStaffRequest(
                "NUR-FORBID",
                null,
                seededDepartment.getId(),
                "Jane",
                "Foster",
                "foster@careflow.local",
                "+1-555-0924",
                StaffType.NURSE,
                LocalDate.of(2023, 7, 1),
                null
        );

        mockMvc.perform(post("/api/v1/staff")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/staff should return 401 Unauthorized when unauthenticated (§44, §45)")
    void createStaff_shouldReturn401_whenUnauthenticated() throws Exception {
        CreateStaffRequest request = new CreateStaffRequest(
                "NUR-UNAUTH",
                null,
                seededDepartment.getId(),
                "Jane",
                "Foster",
                "unauth@careflow.local",
                "+1-555-0925",
                StaffType.NURSE,
                LocalDate.of(2023, 7, 1),
                null
        );

        mockMvc.perform(post("/api/v1/staff")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/staff/{id} should return 200 OK with staff response (§17)")
    @WithMockUser(roles = "RECEPTIONIST")
    void getStaffById_shouldReturn200() throws Exception {
        mockMvc.perform(get("/api/v1/staff/" + seededStaffDoctor.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(seededStaffDoctor.getId()))
                .andExpect(jsonPath("$.staffCode").value("DOC-NEPH-01"))
                .andExpect(jsonPath("$.doctorProfile").isNotEmpty())
                .andExpect(jsonPath("$.doctorProfile.medicalLicenseNumber").value("MED-NEPH-999"));
    }

    @Test
    @DisplayName("GET /api/v1/staff/code/{staffCode} should return 200 OK (§17)")
    @WithMockUser(roles = "NURSE")
    void getStaffByCode_shouldReturn200() throws Exception {
        mockMvc.perform(get("/api/v1/staff/code/DOC-NEPH-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.staffCode").value("DOC-NEPH-01"))
                .andExpect(jsonPath("$.firstName").value("Stephen"))
                .andExpect(jsonPath("$.lastName").value("Strange"));
    }

    @Test
    @DisplayName("GET /api/v1/staff should return paginated staff directory (§17, §71)")
    @WithMockUser(roles = "DOCTOR")
    void searchStaff_shouldReturnPaginatedResponse() throws Exception {
        mockMvc.perform(get("/api/v1/staff")
                        .param("departmentId", seededDepartment.getId())
                        .param("staffType", "DOCTOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].staffCode").value("DOC-NEPH-01"));
    }

    @Test
    @DisplayName("GET /api/v1/staff/doctors should return active doctors directory (§17, §18)")
    @WithMockUser(roles = "RECEPTIONIST")
    void searchDoctors_shouldReturnActiveDoctors() throws Exception {
        mockMvc.perform(get("/api/v1/staff/doctors")
                        .param("specialization", "Nephrology"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].specialization").value("Nephrology"))
                .andExpect(jsonPath("$.content[0].doctorName").value("Stephen Strange"));
    }

    @Test
    @DisplayName("PUT /api/v1/staff/{id} should update staff details for ADMIN (§17)")
    @WithMockUser(roles = "ADMIN")
    void updateStaff_shouldSucceed_forAdmin() throws Exception {
        DoctorProfileDto updatedDocDto = new DoctorProfileDto(
                "Nephrology & Dialysis",
                "MD, PhD, FASN",
                "MED-NEPH-999",
                BigDecimal.valueOf(275.00),
                "Room 405",
                "Updated bio"
        );
        UpdateStaffRequest updateReq = new UpdateStaffRequest(
                null,
                seededDepartment.getId(),
                "Stephen",
                "Strange",
                "stephen.strange@careflow.local",
                "+1-555-0999",
                updatedDocDto
        );

        mockMvc.perform(put("/api/v1/staff/" + seededStaffDoctor.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("stephen.strange@careflow.local"))
                .andExpect(jsonPath("$.doctorProfile.specialization").value("Nephrology & Dialysis"))
                .andExpect(jsonPath("$.doctorProfile.consultationFee").value(275.00));
    }

    @Test
    @DisplayName("PATCH /api/v1/staff/{id}/status should transition status for ADMIN (§17, §69)")
    @WithMockUser(roles = "ADMIN")
    void updateStaffStatus_shouldSucceed_forAdmin() throws Exception {
        UpdateStaffStatusRequest statusReq = new UpdateStaffStatusRequest(StaffStatus.ON_LEAVE);

        mockMvc.perform(patch("/api/v1/staff/" + seededStaffDoctor.getId() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ON_LEAVE"));
    }
}
