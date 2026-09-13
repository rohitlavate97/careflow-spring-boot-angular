package com.careflow.document.domain;

import com.careflow.common.domain.BaseAuditEntity;
import com.careflow.document.exception.InvalidDocumentStateException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.util.Objects;

/**
 * Medical document aggregate root managing metadata, integrity checksums,
 * version lineage, and storage references (§34, §103 Phase 13).
 */
@Entity
@Table(
        name = "documents",
        indexes = {
                @Index(name = "idx_documents_patient_id", columnList = "patient_id"),
                @Index(name = "idx_documents_type", columnList = "document_type"),
                @Index(name = "idx_documents_status", columnList = "status"),
                @Index(name = "idx_documents_parent_id", columnList = "parent_document_id"),
                @Index(name = "idx_documents_uploaded_by", columnList = "uploaded_by_id"),
                @Index(name = "idx_documents_reference", columnList = "reference_type, reference_id")
        }
)
public class MedicalDocument extends BaseAuditEntity {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @Column(name = "document_number", length = 64, nullable = false, unique = true)
    private String documentNumber;

    @Column(name = "title", length = 200, nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", length = 50, nullable = false)
    private DocumentType documentType;

    @Column(name = "patient_id", length = 64, nullable = false)
    private String patientId;

    @Column(name = "uploaded_by_id", length = 64, nullable = false)
    private String uploadedById;

    @Column(name = "file_name", length = 255, nullable = false)
    private String fileName;

    @Column(name = "mime_type", length = 100, nullable = false)
    private String mimeType;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "checksum_sha256", length = 64, nullable = false)
    private String checksumSha256;

    @Column(name = "storage_path", length = 500, nullable = false)
    private String storagePath;

    @Column(name = "document_version", nullable = false)
    private Integer documentVersion = 1;

    @Column(name = "parent_document_id", length = 64)
    private String parentDocumentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private DocumentStatus status = DocumentStatus.ACTIVE;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "reference_id", length = 64)
    private String referenceId;

    @Column(name = "reference_type", length = 50)
    private String referenceType;

    protected MedicalDocument() {
        // JPA requirement
    }

    public MedicalDocument(String id,
                           String documentNumber,
                           String title,
                           DocumentType documentType,
                           String patientId,
                           String uploadedById,
                           String fileName,
                           String mimeType,
                           Long fileSize,
                           String checksumSha256,
                           String storagePath,
                           Integer documentVersion,
                           String parentDocumentId,
                           DocumentStatus status,
                           String description,
                           String referenceId,
                           String referenceType) {
        this.id = id;
        this.documentNumber = documentNumber;
        this.title = title;
        this.documentType = documentType;
        this.patientId = patientId;
        this.uploadedById = uploadedById;
        this.fileName = fileName;
        this.mimeType = mimeType;
        this.fileSize = fileSize;
        this.checksumSha256 = checksumSha256;
        this.storagePath = storagePath;
        this.documentVersion = (documentVersion != null && documentVersion >= 1) ? documentVersion : 1;
        this.parentDocumentId = parentDocumentId;
        this.status = (status != null) ? status : DocumentStatus.ACTIVE;
        this.description = description;
        this.referenceId = referenceId;
        this.referenceType = referenceType;
    }

    public void archive() {
        if (this.status == DocumentStatus.DELETED) {
            throw new InvalidDocumentStateException("Cannot archive a deleted document [" + id + "].");
        }
        this.status = DocumentStatus.ARCHIVED;
    }

    public void delete() {
        if (this.status == DocumentStatus.DELETED) {
            throw new InvalidDocumentStateException("Document [" + id + "] is already deleted.");
        }
        this.status = DocumentStatus.DELETED;
    }

    public void restore() {
        if (this.status != DocumentStatus.ARCHIVED) {
            throw new InvalidDocumentStateException("Only archived documents can be restored to active state.");
        }
        this.status = DocumentStatus.ACTIVE;
    }

    public boolean isActive() {
        return this.status == DocumentStatus.ACTIVE;
    }

    public boolean isArchived() {
        return this.status == DocumentStatus.ARCHIVED;
    }

    public boolean isDeleted() {
        return this.status == DocumentStatus.DELETED;
    }

    public String getId() {
        return id;
    }

    public String getDocumentNumber() {
        return documentNumber;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public DocumentType getDocumentType() {
        return documentType;
    }

    public void setDocumentType(DocumentType documentType) {
        this.documentType = documentType;
    }

    public String getPatientId() {
        return patientId;
    }

    public String getUploadedById() {
        return uploadedById;
    }

    public String getFileName() {
        return fileName;
    }

    public String getMimeType() {
        return mimeType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public String getChecksumSha256() {
        return checksumSha256;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public Integer getDocumentVersion() {
        return documentVersion;
    }

    public String getParentDocumentId() {
        return parentDocumentId;
    }

    public void setParentDocumentId(String parentDocumentId) {
        this.parentDocumentId = parentDocumentId;
    }

    public DocumentStatus getStatus() {
        return status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(String referenceType) {
        this.referenceType = referenceType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MedicalDocument that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
