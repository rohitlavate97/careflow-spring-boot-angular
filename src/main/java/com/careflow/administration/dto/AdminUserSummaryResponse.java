package com.careflow.administration.dto;

import com.careflow.identity.domain.RoleType;
import com.careflow.identity.domain.UserStatus;

import java.time.Instant;
import java.util.Set;

/**
 * Summary view of a user account for administrative consoles (§38, §40).
 */
public record AdminUserSummaryResponse(
        String id,
        String username,
        String email,
        String firstName,
        String lastName,
        String phone,
        UserStatus status,
        Set<RoleType> roles,
        Instant createdAt
) {}
