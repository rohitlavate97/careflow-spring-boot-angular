package com.careflow.administration.dto;

import java.time.Instant;

/**
 * Response payload indicating current system maintenance status (§38).
 */
public record MaintenanceModeResponse(
        boolean enabled,
        String reason,
        String updatedBy,
        Instant updatedAt
) {}
