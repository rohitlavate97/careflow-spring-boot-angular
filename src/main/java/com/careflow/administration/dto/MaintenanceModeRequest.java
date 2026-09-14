package com.careflow.administration.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request payload for toggling hospital system maintenance mode (§38).
 */
public record MaintenanceModeRequest(
        @NotNull(message = "Enabled flag is required")
        Boolean enabled,

        @Size(max = 255, message = "Reason must not exceed 255 characters")
        String reason
) {}
