package com.careflow.consultation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload for initiating a consultation encounter (§22).
 */
public record CreateConsultationRequest(
        @NotBlank(message = "Patient ID is required")
        @Size(max = 64, message = "Patient ID cannot exceed 64 characters")
        String patientId,

        @NotBlank(message = "Doctor ID is required")
        @Size(max = 64, message = "Doctor ID cannot exceed 64 characters")
        String doctorId,

        @Size(max = 64, message = "Appointment ID cannot exceed 64 characters")
        String appointmentId,

        @Size(max = 64, message = "Queue entry ID cannot exceed 64 characters")
        String queueEntryId,

        String chiefComplaint
) {
}
