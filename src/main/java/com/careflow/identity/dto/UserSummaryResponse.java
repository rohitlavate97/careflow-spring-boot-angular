package com.careflow.identity.dto;

import java.util.List;

/**
 * Publicly exposable user profile summary (passwords and internal audit hashes strictly excluded).
 */
public record UserSummaryResponse(
        String id,
        String username,
        String email,
        String firstName,
        String lastName,
        String fullName,
        String phone,
        String status,
        List<String> roles,
        List<String> permissions
) {}
