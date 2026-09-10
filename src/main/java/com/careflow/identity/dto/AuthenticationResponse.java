package com.careflow.identity.dto;

/**
 * Standard authentication response containing JWT token and user profile summary.
 */
public record AuthenticationResponse(
        String token,
        String tokenType,
        long expiresIn,
        UserSummaryResponse user
) {}
