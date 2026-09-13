package com.careflow.clinical.dto;

import com.careflow.clinical.domain.NoteType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Payload for creating a clinical documentation note (§23).
 */
public record CreateClinicalNoteRequest(
        @NotBlank(message = "Patient ID is required")
        @Size(max = 64, message = "Patient ID cannot exceed 64 characters")
        String patientId,

        @Size(max = 64, message = "Consultation ID cannot exceed 64 characters")
        String consultationId,

        @NotBlank(message = "Author ID is required")
        @Size(max = 64, message = "Author ID cannot exceed 64 characters")
        String authorId,

        @NotNull(message = "Note type is required")
        NoteType noteType,

        @NotBlank(message = "Note title is required")
        @Size(max = 200, message = "Note title cannot exceed 200 characters")
        String title,

        @NotBlank(message = "Note content is required")
        String content
) {
}
