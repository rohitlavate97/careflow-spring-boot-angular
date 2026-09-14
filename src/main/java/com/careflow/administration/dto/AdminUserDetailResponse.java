package com.careflow.administration.dto;

import com.careflow.identity.domain.RoleType;
import com.careflow.identity.domain.UserStatus;

import java.time.Instant;
import java.util.Set;

/**
 * Detailed view of a user account with security details and permissions (§38, §40).
 */
public record AdminUserDetailResponse(
        String id,
        String username,
        String email,
        String firstName,
        String lastName,
        String phone,
        UserStatus status,
        int failedLoginAttempts,
        Instant lockedUntil,
        Set<RoleType> roles,
        Set<String> permissions,
        Instant createdAt,
        Instant updatedAt,
        String createdBy,
        String updatedBy
) {}
