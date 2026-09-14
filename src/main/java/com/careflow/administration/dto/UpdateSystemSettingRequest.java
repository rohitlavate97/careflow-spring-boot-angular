package com.careflow.administration.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for modifying a system configuration setting value (§38).
 */
public record UpdateSystemSettingRequest(
        @NotBlank(message = "Setting value is required")
        @Size(max = 1024, message = "Setting value must not exceed 1024 characters")
        String settingValue
) {}
