package com.careflow.queue.dto;

import com.careflow.queue.domain.QueuePriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for placing a patient into an outpatient department queue (§21, §40).
 */
public record EnqueuePatientRequest(
        @NotBlank(message = "Patient ID is required")
        @Size(max = 64, message = "Patient ID must not exceed 64 characters")
        String patientId,

        @NotBlank(message = "Department ID is required")
        @Size(max = 64, message = "Department ID must not exceed 64 characters")
        String departmentId,

        @Size(max = 64, message = "Doctor ID must not exceed 64 characters")
        String doctorId,

        @Size(max = 64, message = "Appointment ID must not exceed 64 characters")
        String appointmentId,

        QueuePriority priority,

        @Size(max = 500, message = "Notes must not exceed 500 characters")
        String notes
) {
    public QueuePriority resolvedPriority() {
        return priority != null ? priority : QueuePriority.NORMAL;
    }
}
