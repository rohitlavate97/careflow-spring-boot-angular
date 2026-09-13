package com.careflow.document.dto;

import jakarta.validation.constraints.Size;

/**
 * Optional metadata update payload when uploading a new document revision (§34, §103).
 */
public record UploadDocumentVersionRequest(
        @Size(max = 200, message = "Title cannot exceed 200 characters")
        String title,

        @Size(max = 1000, message = "Description cannot exceed 1000 characters")
        String description
) {
}
