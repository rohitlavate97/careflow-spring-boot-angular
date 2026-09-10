package com.careflow.patient.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Data transfer object representing patient next-of-kin / emergency contact (§16).
 */
public record EmergencyContactDto(
        @NotBlank(message = "Emergency contact name is required")
        @Size(max = 100, message = "Emergency contact name must not exceed 100 characters")
        String name,

        @NotBlank(message = "Emergency contact relationship is required")
        @Size(max = 50, message = "Relationship must not exceed 50 characters")
        String relationship,

        @NotBlank(message = "Emergency contact phone is required")
        @Pattern(regexp = "^\\+?[0-9. ()-]{7,25}$", message = "Emergency contact phone number format is invalid")
        String phone
) {
}
