package com.careflow.department.controller;

import com.careflow.department.domain.Department;
import com.careflow.department.domain.DepartmentStatus;
import com.careflow.department.dto.CreateDepartmentRequest;
import com.careflow.department.dto.UpdateDepartmentRequest;
import com.careflow.department.dto.UpdateDepartmentStatusRequest;
import com.careflow.department.repository.DepartmentRepository;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

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
class DepartmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DepartmentRepository departmentRepository;

    private Department seededDepartment;

    @BeforeEach
    void setUp() {
        seededDepartment = new Department(
                UUID.randomUUID().toString(),
                "GASTRO",
                "Gastroenterology",
                "Digestive and Liver Disease Center",
                "Building A, Floor 4"
        );
        departmentRepository.save(seededDepartment);
    }

    @Test
    @DisplayName("POST /api/v1/departments should return 201 Created with Location header for ADMIN (§17, §89)")
    @WithMockUser(roles = "ADMIN")
    void createDepartment_shouldReturn201_whenValid() throws Exception {
        CreateDepartmentRequest request = new CreateDepartmentRequest(
                "PULM",
                "Pulmonology",
                "Respiratory and Pulmonary Medicine",
                "+1-555-0811",
                "pulmonology@careflow.local",
                "Building B, Floor 3",
                null
        );

        mockMvc.perform(post("/api/v1/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/v1/departments/")))
                .andExpect(jsonPath("$.code").value("PULM"))
                .andExpect(jsonPath("$.name").value("Pulmonology"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("POST /api/v1/departments should return 400 Bad Request when validation fails (§41)")
    @WithMockUser(roles = "ADMIN")
    void createDepartment_shouldReturn400_whenInvalid() throws Exception {
        CreateDepartmentRequest invalidRequest = new CreateDepartmentRequest(
                "X", // Too short (regex requires 2-20)
                "",  // Blank name
                null,
                null,
                "invalid-email", // Malformed email
                null,
                null
        );

        mockMvc.perform(post("/api/v1/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.validationErrors").isArray());
    }

    @Test
    @DisplayName("POST /api/v1/departments should return 409 Conflict when department code already exists (§17, §92)")
    @WithMockUser(roles = "ADMIN")
    void createDepartment_shouldReturn409_whenDuplicateCode() throws Exception {
        CreateDepartmentRequest duplicateRequest = new CreateDepartmentRequest(
                "GASTRO",
                "Gastroenterology Duplicate",
                null,
                null,
                null,
                null,
                null
        );

        mockMvc.perform(post("/api/v1/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_DEPARTMENT_CODE"));
    }

    @Test
    @DisplayName("POST /api/v1/departments should return 403 Forbidden for non-ADMIN role (§44, §45)")
    @WithMockUser(roles = "DOCTOR")
    void createDepartment_shouldReturn403_forDoctor() throws Exception {
        CreateDepartmentRequest request = new CreateDepartmentRequest(
                "ENDO",
                "Endocrinology",
                null,
                null,
                null,
                null,
                null
        );

        mockMvc.perform(post("/api/v1/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/departments should return 401 Unauthorized when unauthenticated (§44, §45)")
    void createDepartment_shouldReturn401_whenUnauthenticated() throws Exception {
        CreateDepartmentRequest request = new CreateDepartmentRequest(
                "ENDO",
                "Endocrinology",
                null,
                null,
                null,
                null,
                null
        );

        mockMvc.perform(post("/api/v1/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/departments should return 200 OK for any authenticated user (§17, §44)")
    @WithMockUser(roles = "NURSE")
    void getAllDepartments_shouldReturn200_forAuthenticatedUser() throws Exception {
        mockMvc.perform(get("/api/v1/departments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("GET /api/v1/departments/code/{code} should return department by code (§17)")
    @WithMockUser(roles = "RECEPTIONIST")
    void getDepartmentByCode_shouldReturn200() throws Exception {
        mockMvc.perform(get("/api/v1/departments/code/GASTRO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("GASTRO"))
                .andExpect(jsonPath("$.name").value("Gastroenterology"));
    }

    @Test
    @DisplayName("PUT and PATCH on department should modify details and operational status (§17, §69)")
    @WithMockUser(roles = "ADMIN")
    void updateAndTransitionDepartment_shouldSucceed() throws Exception {
        UpdateDepartmentRequest updateReq = new UpdateDepartmentRequest(
                "Gastroenterology and Hepatology",
                "Expanded hepatology and endoscopy suite",
                "+1-555-0819",
                "gastro@careflow.local",
                "Building A, Suite 400",
                null
        );

        mockMvc.perform(put("/api/v1/departments/" + seededDepartment.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Gastroenterology and Hepatology"));

        UpdateDepartmentStatusRequest statusReq = new UpdateDepartmentStatusRequest(DepartmentStatus.SUSPENDED);

        mockMvc.perform(patch("/api/v1/departments/" + seededDepartment.getId() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUSPENDED"));

        mockMvc.perform(delete("/api/v1/departments/" + seededDepartment.getId()))
                .andExpect(status().isNoContent());
    }
}
