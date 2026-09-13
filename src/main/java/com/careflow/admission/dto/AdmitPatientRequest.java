package com.careflow.admission.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload for admitting an inpatient to a designated hospital bed (§28, §57 Lab 4).
 */
public record AdmitPatientRequest(
        @NotBlank(message = "Patient ID is required")
        String patientId,

        @NotBlank(message = "Admitting doctor ID is required")
        String admittingDoctorId,

        @NotBlank(message = "Bed ID is required")
        String bedId,

        String encounterId,

        @NotBlank(message = "Admission reason is required")
        @Size(max = 2000, message = "Admission reason must not exceed 2000 characters")
        String admissionReason,

        @Size(max = 2000, message = "Admitting diagnosis must not exceed 2000 characters")
        String admittingDiagnosis
) {
}
