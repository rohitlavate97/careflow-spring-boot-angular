package com.careflow.department.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for updating department details (§17, §40).
 */
public record UpdateDepartmentRequest(
        @NotBlank(message = "Department name is required")
        @Size(max = 100, message = "Department name must not exceed 100 characters")
        String name,

        @Size(max = 500, message = "Description must not exceed 500 characters")
        String description,

        @Size(max = 20, message = "Contact phone must not exceed 20 characters")
        String contactPhone,

        @Email(message = "Contact email must be valid")
        @Size(max = 100, message = "Contact email must not exceed 100 characters")
        String contactEmail,

        @Size(max = 100, message = "Location must not exceed 100 characters")
        String location,

        String headOfDepartmentId
) {
}
