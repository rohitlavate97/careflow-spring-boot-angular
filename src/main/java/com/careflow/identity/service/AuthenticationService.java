package com.careflow.identity.service;

import com.careflow.identity.domain.Permission;
import com.careflow.identity.domain.Role;
import com.careflow.identity.domain.RoleType;
import com.careflow.identity.domain.User;
import com.careflow.identity.domain.UserStatus;
import com.careflow.identity.dto.AuthenticationResponse;
import com.careflow.identity.dto.LoginRequest;
import com.careflow.identity.dto.RegisterRequest;
import com.careflow.identity.dto.UserSummaryResponse;
import com.careflow.identity.exception.AccountLockedException;
import com.careflow.identity.exception.InvalidCredentialsException;
import com.careflow.identity.exception.UserAlreadyExistsException;
import com.careflow.identity.repository.RoleRepository;
import com.careflow.identity.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * Core Authentication and Credential Lifecycle Service (§14, §44).
 * Enforces BCrypt verification, failed attempt counters, and 5-strike account lockout policy.
 */
@Service
public class AuthenticationService {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationService.class);
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final Duration LOCKOUT_DURATION = Duration.ofMinutes(15);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthenticationService(UserRepository userRepository,
                                 RoleRepository roleRepository,
                                 PasswordEncoder passwordEncoder,
                                 JwtService jwtService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthenticationResponse login(LoginRequest request) {
        User user = userRepository.findByUsernameWithRolesAndPermissions(request.username())
                .or(() -> userRepository.findByEmailWithRolesAndPermissions(request.username()))
                .orElseThrow(() -> {
                    log.warn("Authentication failed: User identifier '{}' not found", request.username());
                    return new InvalidCredentialsException();
                });

        // 1. Account status check
        if (user.getStatus() == UserStatus.SUSPENDED || user.getStatus() == UserStatus.INACTIVE) {
            log.warn("Authentication blocked: User '{}' status is {}", user.getUsername(), user.getStatus());
            throw new InvalidCredentialsException("Account is " + user.getStatus().name().toLowerCase() + ". Please contact administrator.");
        }

        // 2. Lockout evaluation (§14)
        if (!user.isAccountNonLocked()) {
            log.warn("Authentication blocked: User '{}' account is locked until {}", user.getUsername(), user.getLockedUntil());
            throw new AccountLockedException("Account is locked due to consecutive failed login attempts. Try again later.");
        }

        // 3. Password verification
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            user.incrementFailedLoginAttempts();
            log.warn("Authentication failed: Bad password for user '{}'. Failed attempts: {}/{}",
                    user.getUsername(), user.getFailedLoginAttempts(), MAX_FAILED_ATTEMPTS);

            if (user.getFailedLoginAttempts() >= MAX_FAILED_ATTEMPTS) {
                user.lockAccount(LOCKOUT_DURATION);
                userRepository.save(user);
                log.error("Security Alert: User '{}' account locked for {} minutes after {} failed attempts",
                        user.getUsername(), LOCKOUT_DURATION.toMinutes(), user.getFailedLoginAttempts());
                throw new AccountLockedException("Account has been locked due to 5 consecutive failed login attempts. Try again in 15 minutes.");
            }

            userRepository.save(user);
            throw new InvalidCredentialsException();
        }

        // 4. Successful login
        user.resetFailedLoginAttempts();
        userRepository.save(user);

        log.info("User '{}' successfully authenticated", user.getUsername());
        String token = jwtService.generateToken(user);
        UserSummaryResponse summary = mapToUserSummary(user);

        return new AuthenticationResponse(
                token,
                "Bearer",
                jwtService.getExpirationDurationSeconds(),
                summary
        );
    }

    @Transactional
    public AuthenticationResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new UserAlreadyExistsException("username", request.username());
        }

        if (userRepository.existsByEmail(request.email())) {
            throw new UserAlreadyExistsException("email", request.email());
        }

        Role patientRole = roleRepository.findByName(RoleType.ROLE_PATIENT)
                .orElseGet(() -> roleRepository.save(new Role(UUID.randomUUID().toString(), RoleType.ROLE_PATIENT, "Default patient self-service role")));

        User user = new User(
                UUID.randomUUID().toString(),
                request.username(),
                request.email(),
                passwordEncoder.encode(request.password()),
                request.firstName(),
                request.lastName(),
                request.phone()
        );
        user.addRole(patientRole);

        User savedUser = userRepository.save(user);
        log.info("Registered new user '{}' with id '{}'", savedUser.getUsername(), savedUser.getId());

        String token = jwtService.generateToken(savedUser);
        UserSummaryResponse summary = mapToUserSummary(savedUser);

        return new AuthenticationResponse(
                token,
                "Bearer",
                jwtService.getExpirationDurationSeconds(),
                summary
        );
    }

    @Transactional(readOnly = true)
    public UserSummaryResponse getCurrentUserSummary(String username) {
        User user = userRepository.findByUsernameWithRolesAndPermissions(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        return mapToUserSummary(user);
    }

    private UserSummaryResponse mapToUserSummary(User user) {
        List<String> roles = user.getRoles().stream()
                .map(r -> r.getName().name())
                .toList();

        List<String> permissions = user.getRoles().stream()
                .flatMap(r -> r.getPermissions().stream())
                .map(Permission::getName)
                .distinct()
                .toList();

        return new UserSummaryResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getFirstName() + " " + user.getLastName(),
                user.getPhone(),
                user.getStatus().name(),
                roles,
                permissions
        );
    }
}
