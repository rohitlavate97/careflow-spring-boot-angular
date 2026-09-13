package com.careflow.document.service;

import com.careflow.common.dto.PageResponse;
import com.careflow.document.domain.DocumentStatus;
import com.careflow.document.domain.DocumentType;
import com.careflow.document.dto.DocumentResponse;
import com.careflow.document.dto.UploadDocumentRequest;
import com.careflow.document.dto.UploadDocumentVersionRequest;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Service interface for medical document management, versioning, and access control (§34, §103 Phase 13).
 */
public interface DocumentService {

    /**
     * Uploads and registers a new medical document with cryptographic integrity verification.
     */
    DocumentResponse uploadDocument(MultipartFile file, UploadDocumentRequest request, String uploadedById);

    /**
     * Uploads a new revision of an existing document, superseding earlier revisions.
     */
    DocumentResponse uploadNewVersion(String documentId, MultipartFile file, UploadDocumentVersionRequest request, String uploadedById);

    /**
     * Retrieves document metadata by unique identifier.
     */
    DocumentResponse getDocumentById(String id);

    /**
     * Loads physical file resource for secure streaming download.
     */
    Resource downloadDocument(String id);

    /**
     * Retrieves paginated documents belonging to a patient, with optional type and status filters.
     */
    PageResponse<DocumentResponse> getPatientDocuments(String patientId, DocumentType documentType, DocumentStatus status, Pageable pageable);

    /**
     * Retrieves the complete version lineage and revision history for a document family.
     */
    List<DocumentResponse> getDocumentHistory(String documentId);

    /**
     * Transitions an active document into an archived state.
     */
    DocumentResponse archiveDocument(String id);

    /**
     * Soft-deletes a document, retaining metadata for regulatory compliance.
     */
    void deleteDocument(String id);
}
