package com.careflow.document.dto;

import com.careflow.document.domain.DocumentStatus;
import com.careflow.document.domain.DocumentType;

import java.time.Instant;

/**
 * Standard API response representation of a medical document (§34, §89).
 */
public record DocumentResponse(
        String id,
        String documentNumber,
        String title,
        DocumentType documentType,
        String patientId,
        String uploadedById,
        String fileName,
        String mimeType,
        Long fileSize,
        String checksumSha256,
        Integer documentVersion,
        String parentDocumentId,
        DocumentStatus status,
        String description,
        String referenceId,
        String referenceType,
        Instant createdAt,
        Instant updatedAt
) {
}
