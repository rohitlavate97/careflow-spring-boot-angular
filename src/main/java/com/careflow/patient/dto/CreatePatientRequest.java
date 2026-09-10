package com.careflow.patient.dto;

import com.careflow.patient.domain.BloodGroup;
import com.careflow.patient.domain.Gender;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Request payload for patient registration (§16).
 */
public record CreatePatientRequest(
        @NotBlank(message = "First name is required")
        @Size(min = 1, max = 50, message = "First name must be between 1 and 50 characters")
        String firstName,

        @Size(max = 50, message = "Middle name must not exceed 50 characters")
        String middleName,

        @NotBlank(message = "Last name is required")
        @Size(min = 1, max = 50, message = "Last name must be between 1 and 50 characters")
        String lastName,

        @NotNull(message = "Date of birth is required")
        @Past(message = "Date of birth must be in the past")
        LocalDate dateOfBirth,

        @NotNull(message = "Gender is required")
        Gender gender,

        BloodGroup bloodGroup,

        @Email(message = "Email format is invalid")
        @Size(max = 100, message = "Email must not exceed 100 characters")
        String email,

        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = "^\\+?[0-9. ()-]{7,25}$", message = "Phone number format is invalid")
        String phone,

        @Valid
        AddressDto address,

        @Valid
        EmergencyContactDto emergencyContact
) {
}
