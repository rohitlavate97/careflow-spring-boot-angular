package com.careflow.laboratory.dto;

import com.careflow.laboratory.domain.LabOrderPriority;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Diagnostic requisition submission payload (§27).
 */
public record CreateLabOrderRequest(
        @NotBlank(message = "Patient ID is required")
        String patientId,

        @NotBlank(message = "Ordering doctor ID is required")
        String doctorId,

        String encounterId,

        @NotNull(message = "Priority is required")
        LabOrderPriority priority,

        @Size(max = 2000, message = "Clinical notes must not exceed 2000 characters")
        String clinicalNotes,

        @NotEmpty(message = "At least one lab test item is required")
        @Valid
        List<CreateLabOrderItemRequest> items
) {
}
