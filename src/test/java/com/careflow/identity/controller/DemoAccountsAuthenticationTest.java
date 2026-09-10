package com.careflow.identity.controller;

import com.careflow.identity.domain.Role;
import com.careflow.identity.domain.RoleType;
import com.careflow.identity.domain.User;
import com.careflow.identity.dto.LoginRequest;
import com.careflow.identity.repository.RoleRepository;
import com.careflow.identity.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies that all standard CareFlow demo accounts (§101) can authenticate and obtain expected roles.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DemoAccountsAuthenticationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void seedDemoAccountsIfMissing() {
        seedAccount("user-admin-001", "admin@careflow.local", "Admin@123", "Admin", "System", RoleType.ROLE_ADMIN);
        seedAccount("user-doctor-001", "doctor@careflow.local", "Doctor@123", "Alexander", "Fleming", RoleType.ROLE_DOCTOR);
        seedAccount("user-nurse-001", "nurse@careflow.local", "Nurse@123", "Florence", "Nightingale", RoleType.ROLE_NURSE);
        seedAccount("user-receptionist-001", "receptionist@careflow.local", "Receptionist@123", "Clara", "Barton", RoleType.ROLE_RECEPTIONIST);
        seedAccount("user-pharmacist-001", "pharmacist@careflow.local", "Pharmacist@123", "John", "Pemberton", RoleType.ROLE_PHARMACIST);
        seedAccount("user-lab-001", "lab@careflow.local", "Lab@123", "Marie", "Curie", RoleType.ROLE_LAB_TECHNICIAN);
        seedAccount("user-billing-001", "billing@careflow.local", "Billing@123", "Ada", "Lovelace", RoleType.ROLE_BILLING_OFFICER);
    }

    private void seedAccount(String id, String email, String rawPassword, String first, String last, RoleType roleType) {
        if (userRepository.findByEmail(email).isEmpty()) {
            Role role = roleRepository.findByName(roleType)
                    .orElseGet(() -> roleRepository.save(new Role(UUID.randomUUID().toString(), roleType, roleType.name())));

            User user = new User(id, email, email, passwordEncoder.encode(rawPassword), first, last, "+1-555-0100");
            user.addRole(role);
            userRepository.save(user);
        }
    }

    @ParameterizedTest(name = "Demo account {0} successfully logs in with role {2}")
    @CsvSource({
            "admin@careflow.local, Admin@123, ROLE_ADMIN",
            "doctor@careflow.local, Doctor@123, ROLE_DOCTOR",
            "nurse@careflow.local, Nurse@123, ROLE_NURSE",
            "receptionist@careflow.local, Receptionist@123, ROLE_RECEPTIONIST",
            "pharmacist@careflow.local, Pharmacist@123, ROLE_PHARMACIST",
            "lab@careflow.local, Lab@123, ROLE_LAB_TECHNICIAN",
            "billing@careflow.local, Billing@123, ROLE_BILLING_OFFICER"
    })
    @DisplayName("All standard demo accounts authenticate and return correct roles (§101)")
    void demoAccounts_shouldAuthenticateSuccessfully(String email, String password, String expectedRole) throws Exception {
        LoginRequest request = new LoginRequest(email, password);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.email").value(email))
                .andExpect(jsonPath("$.user.roles[0]").value(expectedRole));
    }
}
