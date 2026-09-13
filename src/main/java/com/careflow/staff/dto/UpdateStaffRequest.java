package com.careflow.staff.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for updating an existing staff member's profile (§17, §40).
 */
public record UpdateStaffRequest(
        @Size(max = 64, message = "User ID must not exceed 64 characters")
        String userId,

        @NotBlank(message = "Department ID is required")
        @Size(max = 64, message = "Department ID must not exceed 64 characters")
        String departmentId,

        @NotBlank(message = "First name is required")
        @Size(max = 50, message = "First name must not exceed 50 characters")
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(max = 50, message = "Last name must not exceed 50 characters")
        String lastName,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        @Size(max = 100, message = "Email must not exceed 100 characters")
        String email,

        @NotBlank(message = "Phone number is required")
        @Size(max = 20, message = "Phone number must not exceed 20 characters")
        String phone,

        @Valid
        DoctorProfileDto doctorProfile
) {
}
