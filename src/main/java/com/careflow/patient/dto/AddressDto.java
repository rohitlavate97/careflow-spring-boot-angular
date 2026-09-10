package com.careflow.patient.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Data transfer object representing patient residential address coordinates (§16).
 */
public record AddressDto(
        @NotBlank(message = "Address line 1 is required")
        @Size(max = 150, message = "Address line 1 must not exceed 150 characters")
        String addressLine1,

        @Size(max = 150, message = "Address line 2 must not exceed 150 characters")
        String addressLine2,

        @NotBlank(message = "City is required")
        @Size(max = 50, message = "City must not exceed 50 characters")
        String city,

        @NotBlank(message = "State or province is required")
        @Size(max = 50, message = "State or province must not exceed 50 characters")
        String state,

        @NotBlank(message = "Postal code is required")
        @Size(max = 20, message = "Postal code must not exceed 20 characters")
        String postalCode,

        @NotBlank(message = "Country is required")
        @Size(max = 50, message = "Country must not exceed 50 characters")
        String country
) {
}
