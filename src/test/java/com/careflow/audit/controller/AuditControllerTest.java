package com.careflow.audit.controller;

import com.careflow.audit.domain.AuditAction;
import com.careflow.audit.domain.AuditLog;
import com.careflow.audit.domain.AuditResourceType;
import com.careflow.audit.domain.AuditStatus;
import com.careflow.audit.dto.RecordAuditEventRequest;
import com.careflow.audit.repository.AuditLogRepository;
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

import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuditControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuditLogRepository auditLogRepository;

    private AuditLog testLog;

    @BeforeEach
    void setUp() {
        testLog = new AuditLog(
                UUID.randomUUID().toString(),
                "admin@careflow.local",
                AuditAction.PATIENT_VIEWED,
                AuditResourceType.PATIENT,
                "pat-ctrl-01",
                "pat-ctrl-01",
                Instant.now(),
                null,
                null,
                "127.0.0.1",
                "corr-ctrl-1",
                AuditStatus.SUCCESS,
                "Integration test patient inspection"
        );
        auditLogRepository.save(testLog);
    }

    @Test
    @DisplayName("POST /api/v1/audit/events succeeds for authenticated user and returns 201 Created")
    @WithMockUser(username = "doctor@careflow.local", roles = "DOCTOR")
    void recordEvent_Authenticated_Returns201() throws Exception {
        RecordAuditEventRequest request = new RecordAuditEventRequest(
                null, // will be resolved from SecurityContext
                AuditAction.CLINICAL_RECORD_VIEWED,
                AuditResourceType.CLINICAL_RECORD,
                "cr-555",
                "pat-ctrl-01",
                null,
                null,
                null,
                null,
                AuditStatus.SUCCESS,
                "Physician chart review"
        );

        mockMvc.perform(post("/api/v1/audit/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/v1/audit/logs/")))
                .andExpect(jsonPath("$.action", is("CLINICAL_RECORD_VIEWED")))
                .andExpect(jsonPath("$.resourceType", is("CLINICAL_RECORD")))
                .andExpect(jsonPath("$.resourceId", is("cr-555")))
                .andExpect(jsonPath("$.patientId", is("pat-ctrl-01")))
                .andExpect(jsonPath("$.status", is("SUCCESS")));
    }

    @Test
    @DisplayName("POST /api/v1/audit/events fails with 400 Bad Request when validation constraints fail")
    @WithMockUser(roles = "DOCTOR")
    void recordEvent_InvalidPayload_Returns400() throws Exception {
        // Missing action and resourceType
        String invalidJson = "{\"details\": \"Missing required attributes\"}";

        mockMvc.perform(post("/api/v1/audit/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_FAILED")));
    }

    @Test
    @DisplayName("POST /api/v1/audit/events returns 401 Unauthorized for unauthenticated request")
    void recordEvent_Unauthenticated_Returns401() throws Exception {
        RecordAuditEventRequest request = new RecordAuditEventRequest(
                "hacker",
                AuditAction.PATIENT_VIEWED,
                AuditResourceType.PATIENT,
                "pat-1",
                "pat-1",
                null,
                null,
                null,
                null,
                AuditStatus.SUCCESS,
                "Unauthorized attempt"
        );

        mockMvc.perform(post("/api/v1/audit/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/audit/logs succeeds for ROLE_ADMIN")
    @WithMockUser(roles = "ADMIN")
    void searchAuditLogs_Admin_Returns200() throws Exception {
        mockMvc.perform(get("/api/v1/audit/logs")
                        .param("resourceType", "PATIENT")
                        .param("action", "PATIENT_VIEWED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.content[0].resourceType", is("PATIENT")));
    }

    @Test
    @DisplayName("GET /api/v1/audit/logs returns 403 Forbidden for non-admin roles")
    @WithMockUser(roles = "DOCTOR")
    void searchAuditLogs_NonAdmin_Returns403() throws Exception {
        mockMvc.perform(get("/api/v1/audit/logs"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/audit/logs/{id} succeeds for ROLE_ADMIN")
    @WithMockUser(roles = "ADMIN")
    void getAuditLogById_Admin_Returns200() throws Exception {
        mockMvc.perform(get("/api/v1/audit/logs/{id}", testLog.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(testLog.getId())))
                .andExpect(jsonPath("$.actorUserId", is(testLog.getActorUserId())))
                .andExpect(jsonPath("$.action", is(testLog.getAction().name())));
    }

    @Test
    @DisplayName("GET /api/v1/audit/logs/{id} returns 404 Not Found when ID does not exist")
    @WithMockUser(roles = "ADMIN")
    void getAuditLogById_NotFound_Returns404() throws Exception {
        mockMvc.perform(get("/api/v1/audit/logs/{id}", "non-existent-uuid"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("RESOURCE_NOT_FOUND")));
    }

    @Test
    @DisplayName("GET /api/v1/audit/patients/{patientId} succeeds for ROLE_ADMIN")
    @WithMockUser(roles = "ADMIN")
    void getPatientAuditTrail_Admin_Returns200() throws Exception {
        mockMvc.perform(get("/api/v1/audit/patients/{patientId}", "pat-ctrl-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.content[0].patientId", is("pat-ctrl-01")));
    }

    @Test
    @DisplayName("GET /api/v1/audit/resources/{resourceType}/{resourceId} succeeds for ROLE_ADMIN")
    @WithMockUser(roles = "ADMIN")
    void getResourceAuditTrail_Admin_Returns200() throws Exception {
        mockMvc.perform(get("/api/v1/audit/resources/{resourceType}/{resourceId}", "PATIENT", "pat-ctrl-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].resourceId", is("pat-ctrl-01")));
    }

    @Test
    @DisplayName("GET /api/v1/audit/reports/summary succeeds for ROLE_ADMIN and returns compliance metrics")
    @WithMockUser(roles = "ADMIN")
    void getAuditSummaryReport_Admin_Returns200() throws Exception {
        mockMvc.perform(get("/api/v1/audit/reports/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEvents", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.eventsByAction.PATIENT_VIEWED", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.eventsByResourceType.PATIENT", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.eventsByStatus.SUCCESS", greaterThanOrEqualTo(1)));
    }

    @Test
    @DisplayName("GET /api/v1/audit/reports/summary returns 403 Forbidden for non-admin roles")
    @WithMockUser(roles = "RECEPTIONIST")
    void getAuditSummaryReport_NonAdmin_Returns403() throws Exception {
        mockMvc.perform(get("/api/v1/audit/reports/summary"))
                .andExpect(status().isForbidden());
    }
}
