package com.careflow.document.dto;

import com.careflow.document.domain.DocumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request payload for creating and uploading a new medical document (§34).
 */
public record UploadDocumentRequest(
        @NotBlank(message = "Document title is required")
        @Size(max = 200, message = "Document title cannot exceed 200 characters")
        String title,

        @NotNull(message = "Document type is required")
        DocumentType documentType,

        @NotBlank(message = "Patient ID is required")
        String patientId,

        @Size(max = 1000, message = "Description cannot exceed 1000 characters")
        String description,

        String referenceId,

        String referenceType
) {
}
