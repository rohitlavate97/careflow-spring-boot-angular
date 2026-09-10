package com.careflow.identity.service;

import com.careflow.identity.domain.Role;
import com.careflow.identity.domain.RoleType;
import com.careflow.identity.domain.User;
import com.careflow.identity.domain.UserStatus;
import com.careflow.identity.dto.AuthenticationResponse;
import com.careflow.identity.dto.LoginRequest;
import com.careflow.identity.dto.RegisterRequest;
import com.careflow.identity.exception.AccountLockedException;
import com.careflow.identity.exception.InvalidCredentialsException;
import com.careflow.identity.exception.UserAlreadyExistsException;
import com.careflow.identity.repository.RoleRepository;
import com.careflow.identity.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private JwtService jwtService;
    private AuthenticationService authenticationService;
    private User testUser;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(
                "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970",
                86400000L
        );
        authenticationService = new AuthenticationService(userRepository, roleRepository, passwordEncoder, jwtService);

        testUser = new User(
                UUID.randomUUID().toString(),
                "test.user",
                "test@hospital.org",
                "$2a$12$encodedpasswordhash",
                "Test",
                "User",
                "+1-555-0155"
        );
        Role patientRole = new Role(UUID.randomUUID().toString(), RoleType.ROLE_PATIENT, "Patient Role");
        testUser.addRole(patientRole);
    }

    @Test
    @DisplayName("login should succeed with valid credentials, reset failed attempts, and return JWT")
    void login_shouldSucceed_whenCredentialsAreValid() {
        LoginRequest request = new LoginRequest("test.user", "ValidPassword123!");

        when(userRepository.findByUsernameWithRolesAndPermissions("test.user")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("ValidPassword123!", testUser.getPasswordHash())).thenReturn(true);

        AuthenticationResponse response = authenticationService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.token()).isNotBlank();
        assertThat(jwtService.extractUsername(response.token())).isEqualTo("test.user");
        assertThat(response.user().username()).isEqualTo("test.user");
        assertThat(testUser.getFailedLoginAttempts()).isEqualTo(0);
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("login should throw InvalidCredentialsException and increment failed attempts when password incorrect")
    void login_shouldFailAndIncrementAttempts_whenPasswordIncorrect() {
        LoginRequest request = new LoginRequest("test.user", "WrongPassword!");

        when(userRepository.findByUsernameWithRolesAndPermissions("test.user")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("WrongPassword!", testUser.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() -> authenticationService.login(request))
                .isInstanceOf(InvalidCredentialsException.class);

        assertThat(testUser.getFailedLoginAttempts()).isEqualTo(1);
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("login should lock account when failed attempts reach threshold of 5 (§14)")
    void login_shouldLockAccount_whenFailedAttemptsReachThreshold() {
        testUser.setFailedLoginAttempts(4); // 4 prior failures
        LoginRequest request = new LoginRequest("test.user", "WrongPassword!");

        when(userRepository.findByUsernameWithRolesAndPermissions("test.user")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("WrongPassword!", testUser.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() -> authenticationService.login(request))
                .isInstanceOf(AccountLockedException.class)
                .hasMessageContaining("Account has been locked due to 5 consecutive failed login attempts");

        assertThat(testUser.getStatus()).isEqualTo(UserStatus.LOCKED);
        assertThat(testUser.getLockedUntil()).isNotNull();
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("login should immediately reject when account is already locked")
    void login_shouldReject_whenAccountLocked() {
        testUser.lockAccount(Duration.ofMinutes(15));
        LoginRequest request = new LoginRequest("test.user", "AnyPassword");

        when(userRepository.findByUsernameWithRolesAndPermissions("test.user")).thenReturn(Optional.of(testUser));

        assertThatThrownBy(() -> authenticationService.login(request))
                .isInstanceOf(AccountLockedException.class)
                .hasMessageContaining("Account is locked");
    }

    @Test
    @DisplayName("register should encode password, assign ROLE_PATIENT, and return JWT")
    void register_shouldCreateUser_withRolePatient() {
        RegisterRequest request = new RegisterRequest(
                "new.patient",
                "patient@hospital.org",
                "SecurePassword123!",
                "Alice",
                "Smith",
                "+1-555-0999"
        );

        when(userRepository.existsByUsername("new.patient")).thenReturn(false);
        when(userRepository.existsByEmail("patient@hospital.org")).thenReturn(false);
        when(roleRepository.findByName(RoleType.ROLE_PATIENT)).thenReturn(Optional.of(new Role("r1", RoleType.ROLE_PATIENT, "Patient")));
        when(passwordEncoder.encode("SecurePassword123!")).thenReturn("hashedPassword123");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthenticationResponse response = authenticationService.register(request);

        assertThat(response.token()).isNotBlank();
        assertThat(jwtService.extractUsername(response.token())).isEqualTo("new.patient");
        assertThat(response.user().username()).isEqualTo("new.patient");
        assertThat(response.user().roles()).contains("ROLE_PATIENT");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getPasswordHash()).isEqualTo("hashedPassword123");
    }

    @Test
    @DisplayName("register should throw UserAlreadyExistsException when email is taken")
    void register_shouldThrow_whenEmailExists() {
        RegisterRequest request = new RegisterRequest(
                "new.user",
                "existing@hospital.org",
                "Password123!",
                "First",
                "Last",
                null
        );

        when(userRepository.existsByUsername("new.user")).thenReturn(false);
        when(userRepository.existsByEmail("existing@hospital.org")).thenReturn(true);

        assertThatThrownBy(() -> authenticationService.register(request))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("User with email 'existing@hospital.org' already exists.");
    }
}
