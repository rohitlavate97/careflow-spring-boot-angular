package com.careflow.prescription.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Payload for issuing a medical prescription (§24).
 */
public record CreatePrescriptionRequest(
        @NotBlank(message = "Patient ID is required")
        @Size(max = 64, message = "Patient ID cannot exceed 64 characters")
        String patientId,

        @NotBlank(message = "Doctor ID is required")
        @Size(max = 64, message = "Doctor ID cannot exceed 64 characters")
        String doctorId,

        @Size(max = 64, message = "Consultation ID cannot exceed 64 characters")
        String consultationId,

        String notes,

        @NotEmpty(message = "Prescription must contain at least one medication order item")
        List<@Valid PrescriptionItemRequest> items
) {
}
