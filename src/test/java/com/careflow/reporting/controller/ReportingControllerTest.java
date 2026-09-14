package com.careflow.reporting.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ReportingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /api/v1/reporting/dashboard succeeds for ROLE_ADMIN")
    @WithMockUser(roles = "ADMIN")
    void getDashboardSummary_Admin_Returns200() throws Exception {
        mockMvc.perform(get("/api/v1/reporting/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.generatedAt", notNullValue()))
                .andExpect(jsonPath("$.todayAppointments", notNullValue()))
                .andExpect(jsonPath("$.bedOccupancyRate", notNullValue()));
    }

    @Test
    @DisplayName("GET /api/v1/reporting/dashboard succeeds for ROLE_BILLING_OFFICER")
    @WithMockUser(roles = "BILLING_OFFICER")
    void getDashboardSummary_BillingOfficer_Returns200() throws Exception {
        mockMvc.perform(get("/api/v1/reporting/dashboard"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/reporting/dashboard returns 403 Forbidden for unauthorized roles")
    @WithMockUser(roles = "PATIENT")
    void getDashboardSummary_Patient_Returns403() throws Exception {
        mockMvc.perform(get("/api/v1/reporting/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/reporting/dashboard returns 401 Unauthorized when unauthenticated")
    void getDashboardSummary_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/reporting/dashboard"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/reporting/appointments succeeds for ROLE_DOCTOR")
    @WithMockUser(roles = "DOCTOR")
    void getAppointmentReport_Doctor_Returns200() throws Exception {
        mockMvc.perform(get("/api/v1/reporting/appointments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAppointments", notNullValue()))
                .andExpect(jsonPath("$.dailyTrend").isArray());
    }

    @Test
    @DisplayName("GET /api/v1/reporting/appointments returns 403 Forbidden for unauthorized role")
    @WithMockUser(roles = "PHARMACIST")
    void getAppointmentReport_Pharmacist_Returns403() throws Exception {
        mockMvc.perform(get("/api/v1/reporting/appointments"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/reporting/queue succeeds for ROLE_RECEPTIONIST")
    @WithMockUser(roles = "RECEPTIONIST")
    void getQueueReport_Receptionist_Returns200() throws Exception {
        mockMvc.perform(get("/api/v1/reporting/queue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalQueued", notNullValue()))
                .andExpect(jsonPath("$.averageWaitTimeMinutes", notNullValue()));
    }

    @Test
    @DisplayName("GET /api/v1/reporting/admissions succeeds for ROLE_NURSE")
    @WithMockUser(roles = "NURSE")
    void getInpatientReport_Nurse_Returns200() throws Exception {
        mockMvc.perform(get("/api/v1/reporting/admissions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalBeds", notNullValue()))
                .andExpect(jsonPath("$.wardOccupancy").isArray());
    }

    @Test
    @DisplayName("GET /api/v1/reporting/laboratory succeeds for ROLE_LAB_TECHNICIAN")
    @WithMockUser(roles = "LAB_TECHNICIAN")
    void getLabReport_LabTech_Returns200() throws Exception {
        mockMvc.perform(get("/api/v1/reporting/laboratory"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalOrders", notNullValue()))
                .andExpect(jsonPath("$.topTests").isArray());
    }

    @Test
    @DisplayName("GET /api/v1/reporting/pharmacy succeeds for ROLE_PHARMACIST")
    @WithMockUser(roles = "PHARMACIST")
    void getPharmacyReport_Pharmacist_Returns200() throws Exception {
        mockMvc.perform(get("/api/v1/reporting/pharmacy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalBatches", notNullValue()))
                .andExpect(jsonPath("$.criticalAlerts").isArray());
    }

    @Test
    @DisplayName("GET /api/v1/reporting/financial succeeds for ROLE_BILLING_OFFICER")
    @WithMockUser(roles = "BILLING_OFFICER")
    void getFinancialReport_BillingOfficer_Returns200() throws Exception {
        mockMvc.perform(get("/api/v1/reporting/financial"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalInvoiced", notNullValue()))
                .andExpect(jsonPath("$.insuranceClaims", notNullValue()));
    }

    @Test
    @DisplayName("GET /api/v1/reporting/history succeeds for ROLE_ADMIN")
    @WithMockUser(roles = "ADMIN")
    void getReportExecutionHistory_Admin_Returns200() throws Exception {
        mockMvc.perform(get("/api/v1/reporting/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @DisplayName("GET /api/v1/reporting/history returns 403 Forbidden for non-admin")
    @WithMockUser(roles = "DOCTOR")
    void getReportExecutionHistory_Doctor_Returns403() throws Exception {
        mockMvc.perform(get("/api/v1/reporting/history"))
                .andExpect(status().isForbidden());
    }
}
