package com.careflow.identity.controller;

import com.careflow.identity.dto.LoginRequest;
import com.careflow.identity.dto.RegisterRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthenticationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("End-to-End Registration, Login, and Authenticated /me Flow (§44, §45)")
    void fullAuthenticationLifecycle_shouldSucceed() throws Exception {
        // 1. Register a new user
        RegisterRequest registerRequest = new RegisterRequest(
                "auth.test.user",
                "auth.test@hospital.org",
                "SecurePassword123!",
                "Auth",
                "Tester",
                "+1-555-0988"
        );

        MvcResult registerResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.user.username").value("auth.test.user"))
                .andExpect(jsonPath("$.user.roles[0]").value("ROLE_PATIENT"))
                .andReturn();

        // 2. Login with correct credentials
        LoginRequest loginRequest = new LoginRequest("auth.test.user", "SecurePassword123!");

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.username").value("auth.test.user"))
                .andReturn();

        String responseJson = loginResult.getResponse().getContentAsString();
        String token = objectMapper.readTree(responseJson).get("token").asText();

        // 3. Access protected /me endpoint with Bearer token
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("auth.test.user"))
                .andExpect(jsonPath("$.email").value("auth.test@hospital.org"))
                .andExpect(jsonPath("$.fullName").value("Auth Tester"));
    }

    @Test
    @DisplayName("login should return 401 Unauthorized with standard error contract on invalid password (§42, §45)")
    void login_shouldReturn401_whenPasswordIsWrong() throws Exception {
        LoginRequest badRequest = new LoginRequest("non.existent.user", "WrongPassword123!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.correlationId").exists());
    }

    @Test
    @DisplayName("register should return 400 Bad Request with validation breakdown on invalid payload (§41, §42)")
    void register_shouldReturn400_whenPayloadFailsValidation() throws Exception {
        RegisterRequest invalidRequest = new RegisterRequest(
                "", // Blank username
                "invalid-email-format", // Bad email
                "short", // Password under 8 chars
                "",
                "",
                null
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.validationErrors").isArray());
    }

    @Test
    @DisplayName("GET /api/v1/auth/me should return 401 Unauthorized with standard error contract when unauthenticated (§45)")
    void me_shouldReturn401_whenNoTokenSupplied() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"))
                .andExpect(jsonPath("$.status").value(401));
    }
}
