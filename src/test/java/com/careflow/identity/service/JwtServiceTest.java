package com.careflow.identity.service;

import com.careflow.identity.domain.Permission;
import com.careflow.identity.domain.Role;
import com.careflow.identity.domain.RoleType;
import com.careflow.identity.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;
    private User testUser;

    @BeforeEach
    void setUp() {
        // Standard 256-bit test secret
        String testSecret = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
        jwtService = new JwtService(testSecret, 3600000); // 1 hour

        testUser = new User(
                UUID.randomUUID().toString(),
                "test.doctor",
                "doctor@hospital.org",
                "hashedpassword",
                "Doctor",
                "Who",
                "+1-555-0100"
        );
        Role role = new Role(UUID.randomUUID().toString(), RoleType.ROLE_DOCTOR, "Doctor Role");
        role.addPermission(new Permission(UUID.randomUUID().toString(), "patients:read", "Read patients"));
        testUser.addRole(role);
    }

    @Test
    @DisplayName("generateToken should generate non-blank JWT containing user claims")
    void generateToken_shouldProduceValidJwt_withUserClaims() {
        String token = jwtService.generateToken(testUser);

        assertThat(token).isNotBlank();
        assertThat(jwtService.extractUsername(token)).isEqualTo("test.doctor");
        assertThat(jwtService.extractUserId(token)).isEqualTo(testUser.getId());
        assertThat(jwtService.isTokenExpired(token)).isFalse();
    }

    @Test
    @DisplayName("isTokenValid should return true for matching UserDetails and false for mismatched")
    void isTokenValid_shouldValidateUserIdentity() {
        String token = jwtService.generateToken(testUser);
        CustomUserDetails userDetails = new CustomUserDetails(testUser);

        assertThat(jwtService.isTokenValid(token, userDetails)).isTrue();

        User differentUser = new User(
                UUID.randomUUID().toString(),
                "other.user",
                "other@hospital.org",
                "hash",
                "Other",
                "User",
                null
        );
        CustomUserDetails differentDetails = new CustomUserDetails(differentUser);
        assertThat(jwtService.isTokenValid(token, differentDetails)).isFalse();
    }
}
