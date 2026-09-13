package com.careflow.staff.dto;

import com.careflow.staff.domain.StaffType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Request payload for onboarding a new hospital staff member (§17, §40).
 */
public record CreateStaffRequest(
        @NotBlank(message = "Staff code is required")
        @Pattern(regexp = "^[A-Za-z0-9_-]{2,32}$", message = "Staff code must be 2 to 32 alphanumeric characters")
        String staffCode,

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

        @NotNull(message = "Staff type is required")
        StaffType staffType,

        @NotNull(message = "Date of joining is required")
        @PastOrPresent(message = "Date of joining cannot be in the future")
        LocalDate dateOfJoining,

        @Valid
        DoctorProfileDto doctorProfile
) {
}
