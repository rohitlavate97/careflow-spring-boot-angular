package com.careflow.administration.dto;

import com.careflow.identity.domain.UserStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request payload for modifying a user's account lifecycle status (§38).
 */
public record UpdateUserStatusRequest(
        @NotNull(message = "Status is required")
        UserStatus status,

        @Size(max = 255, message = "Reason must not exceed 255 characters")
        String reason
) {}
