package com.careflow.identity.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Inbound login request payload.
 */
public record LoginRequest(
        @NotBlank(message = "Username or email is required")
        String username,

        @NotBlank(message = "Password is required")
        String password
) {}
