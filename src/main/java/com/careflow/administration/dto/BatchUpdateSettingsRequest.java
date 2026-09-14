package com.careflow.administration.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.Map;

/**
 * Request payload for atomically updating multiple configuration settings (§38).
 */
public record BatchUpdateSettingsRequest(
        @NotEmpty(message = "Settings map must not be empty")
        Map<String, String> settings
) {}
