package com.careflow.admission.dto;

import com.careflow.admission.domain.WardType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Payload for defining a new hospital ward (§28).
 */
public record CreateWardRequest(
        @NotBlank(message = "Ward code is required")
        @Size(max = 32, message = "Ward code must not exceed 32 characters")
        String wardCode,

        @NotBlank(message = "Ward name is required")
        @Size(max = 100, message = "Ward name must not exceed 100 characters")
        String name,

        @NotBlank(message = "Department ID is required")
        String departmentId,

        @NotNull(message = "Ward type is required")
        WardType wardType,

        @Size(max = 50, message = "Floor must not exceed 50 characters")
        String floor,

        @Min(value = 0, message = "Total beds cannot be negative")
        int totalBeds
) {
}
