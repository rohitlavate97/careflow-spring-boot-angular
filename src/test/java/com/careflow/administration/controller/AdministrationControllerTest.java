package com.careflow.administration.controller;

import com.careflow.identity.domain.RoleType;
import com.careflow.identity.domain.User;
import com.careflow.identity.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import com.careflow.administration.domain.SettingCategory;
import com.careflow.administration.domain.SettingDataType;
import com.careflow.administration.domain.SystemSetting;
import com.careflow.administration.repository.SystemSettingRepository;
import com.careflow.identity.domain.Role;
import com.careflow.identity.repository.RoleRepository;
import org.junit.jupiter.api.BeforeEach;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdministrationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private SystemSettingRepository systemSettingRepository;

    @BeforeEach
    void setUp() {
        Role doctorRole = roleRepository.findByName(RoleType.ROLE_DOCTOR)
                .orElseGet(() -> roleRepository.save(new Role(UUID.randomUUID().toString(), RoleType.ROLE_DOCTOR, "Doctor")));

        if (userRepository.findByUsername("doctor@careflow.local").isEmpty()) {
            User user = new User("u-test-doc", "doctor@careflow.local", "doc@careflow.local", "hash", "Alexander", "Fleming", "+1-555");
            user.getRoles().add(doctorRole);
            userRepository.save(user);
        }

        if (systemSettingRepository.findBySettingKey("careflow.facility.name").isEmpty()) {
            systemSettingRepository.save(new SystemSetting(
                    "set-1",
                    "careflow.facility.name",
                    "CareFlow Central Hospital",
                    SettingCategory.FACILITY,
                    SettingDataType.STRING,
                    "Facility name",
                    false,
                    true
            ));
        }

        if (systemSettingRepository.findBySettingKey("careflow.facility.currency").isEmpty()) {
            systemSettingRepository.save(new SystemSetting(
                    "set-2",
                    "careflow.facility.currency",
                    "USD",
                    SettingCategory.BILLING,
                    SettingDataType.STRING,
                    "Currency",
                    false,
                    true
            ));
        }
    }

    @Test
    @DisplayName("GET /api/v1/admin/overview succeeds for ROLE_ADMIN")
    @WithMockUser(username = "admin@careflow.local", roles = "ADMIN")
    void getAdminOverview_Admin_Returns200() throws Exception {
        mockMvc.perform(get("/api/v1/admin/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers", notNullValue()))
                .andExpect(jsonPath("$.facilityName", notNullValue()))
                .andExpect(jsonPath("$.serverTime", notNullValue()));
    }

    @Test
    @DisplayName("GET /api/v1/admin/overview returns 403 Forbidden for non-admin roles")
    @WithMockUser(username = "doctor@careflow.local", roles = "DOCTOR")
    void getAdminOverview_Doctor_Returns403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/overview"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/admin/overview returns 401 Unauthorized when unauthenticated")
    void getAdminOverview_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/overview"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/admin/settings succeeds for ROLE_ADMIN")
    @WithMockUser(username = "admin@careflow.local", roles = "ADMIN")
    void getSettings_Admin_Returns200() throws Exception {
        mockMvc.perform(get("/api/v1/admin/settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", notNullValue()));
    }

    @Test
    @DisplayName("GET /api/v1/admin/settings/{key} returns 200 for seeded setting")
    @WithMockUser(username = "admin@careflow.local", roles = "ADMIN")
    void getSettingByKey_Admin_Returns200() throws Exception {
        mockMvc.perform(get("/api/v1/admin/settings/careflow.facility.name"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.settingKey").value("careflow.facility.name"))
                .andExpect(jsonPath("$.settingValue", notNullValue()));
    }

    @Test
    @DisplayName("GET /api/v1/admin/settings/{key} returns 404 for unknown setting")
    @WithMockUser(username = "admin@careflow.local", roles = "ADMIN")
    void getSettingByKey_Unknown_Returns404() throws Exception {
        mockMvc.perform(get("/api/v1/admin/settings/non.existent.key"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /api/v1/admin/settings/{key} updates setting value")
    @WithMockUser(username = "admin@careflow.local", roles = "ADMIN")
    void updateSetting_Admin_Returns200() throws Exception {
        Map<String, String> request = Map.of("settingValue", "CareFlow Regional Medical Center");

        mockMvc.perform(put("/api/v1/admin/settings/careflow.facility.name")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.settingValue").value("CareFlow Regional Medical Center"));
    }

    @Test
    @DisplayName("POST /api/v1/admin/settings/batch updates multiple settings")
    @WithMockUser(username = "admin@careflow.local", roles = "ADMIN")
    void batchUpdateSettings_Admin_Returns200() throws Exception {
        Map<String, Object> request = Map.of(
                "settings", Map.of("careflow.facility.currency", "EUR")
        );

        mockMvc.perform(post("/api/v1/admin/settings/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/admin/users lists users with pagination for ROLE_ADMIN")
    @WithMockUser(username = "admin@careflow.local", roles = "ADMIN")
    void getUsers_Admin_Returns200() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", notNullValue()))
                .andExpect(jsonPath("$.totalElements", notNullValue()));
    }

    @Test
    @DisplayName("GET /api/v1/admin/users returns 403 Forbidden for ROLE_RECEPTIONIST")
    @WithMockUser(username = "rec@careflow.local", roles = "RECEPTIONIST")
    void getUsers_Receptionist_Returns403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/admin/users/{userId} returns user details")
    @WithMockUser(username = "admin@careflow.local", roles = "ADMIN")
    void getUserById_Admin_Returns200() throws Exception {
        User user = userRepository.findByUsername("doctor@careflow.local")
                .orElseThrow();

        mockMvc.perform(get("/api/v1/admin/users/" + user.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("doctor@careflow.local"))
                .andExpect(jsonPath("$.roles", notNullValue()));
    }

    @Test
    @DisplayName("PUT /api/v1/admin/users/{userId}/status updates user status")
    @WithMockUser(username = "admin@careflow.local", roles = "ADMIN")
    void updateUserStatus_Admin_Returns200() throws Exception {
        User user = userRepository.findByUsername("doctor@careflow.local")
                .orElseThrow();

        Map<String, String> request = Map.of("status", "ACTIVE", "reason", "Re-verified credentials");

        mockMvc.perform(put("/api/v1/admin/users/" + user.getId() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("PUT /api/v1/admin/users/{userId}/roles updates user roles")
    @WithMockUser(username = "admin@careflow.local", roles = "ADMIN")
    void updateUserRoles_Admin_Returns200() throws Exception {
        User user = userRepository.findByUsername("doctor@careflow.local")
                .orElseThrow();

        Map<String, Object> request = Map.of("roles", Set.of("ROLE_DOCTOR"));

        mockMvc.perform(put("/api/v1/admin/users/" + user.getId() + "/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/v1/admin/users/{userId}/reset-password succeeds for ROLE_ADMIN")
    @WithMockUser(username = "admin@careflow.local", roles = "ADMIN")
    void resetUserPassword_Admin_Returns204() throws Exception {
        User user = userRepository.findByUsername("doctor@careflow.local")
                .orElseThrow();

        Map<String, String> request = Map.of("newPassword", "UpdatedPass@2026");

        mockMvc.perform(post("/api/v1/admin/users/" + user.getId() + "/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("GET & POST /api/v1/admin/maintenance controls maintenance mode")
    @WithMockUser(username = "admin@careflow.local", roles = "ADMIN")
    void maintenanceMode_Admin_Success() throws Exception {
        mockMvc.perform(get("/api/v1/admin/maintenance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled", notNullValue()));

        Map<String, Object> request = Map.of("enabled", true, "reason", "Scheduled DB Index Rebuild");

        mockMvc.perform(post("/api/v1/admin/maintenance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.reason").value("Scheduled DB Index Rebuild"));
    }
}
