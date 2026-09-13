package com.careflow.document.mapper;

import com.careflow.document.domain.MedicalDocument;
import com.careflow.document.dto.DocumentResponse;
import org.springframework.stereotype.Component;

/**
 * Mapper transforming MedicalDocument domain entities to API DTOs (§89).
 */
@Component
public class DocumentMapper {

    public DocumentResponse toResponse(MedicalDocument doc) {
        if (doc == null) {
            return null;
        }

        return new DocumentResponse(
                doc.getId(),
                doc.getDocumentNumber(),
                doc.getTitle(),
                doc.getDocumentType(),
                doc.getPatientId(),
                doc.getUploadedById(),
                doc.getFileName(),
                doc.getMimeType(),
                doc.getFileSize(),
                doc.getChecksumSha256(),
                doc.getDocumentVersion(),
                doc.getParentDocumentId(),
                doc.getStatus(),
                doc.getDescription(),
                doc.getReferenceId(),
                doc.getReferenceType(),
                doc.getCreatedAt(),
                doc.getUpdatedAt()
        );
    }
}
