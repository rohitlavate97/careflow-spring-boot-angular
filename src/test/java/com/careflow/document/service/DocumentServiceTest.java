package com.careflow.document.service;

import com.careflow.common.dto.PageResponse;
import com.careflow.document.domain.DocumentStatus;
import com.careflow.document.domain.DocumentType;
import com.careflow.document.domain.MedicalDocument;
import com.careflow.document.dto.DocumentResponse;
import com.careflow.document.dto.UploadDocumentRequest;
import com.careflow.document.dto.UploadDocumentVersionRequest;
import com.careflow.document.exception.DocumentNotFoundException;
import com.careflow.document.exception.InvalidDocumentStateException;
import com.careflow.document.mapper.DocumentMapper;
import com.careflow.document.repository.MedicalDocumentRepository;
import com.careflow.document.storage.StorageService;
import com.careflow.document.storage.StoredFile;
import com.careflow.patient.exception.PatientNotFoundException;
import com.careflow.patient.repository.PatientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    private MedicalDocumentRepository documentRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private StorageService storageService;

    private DocumentMapper documentMapper;
    private DocumentServiceImpl documentService;

    @BeforeEach
    void setUp() {
        documentMapper = new DocumentMapper();
        documentService = new DocumentServiceImpl(documentRepository, patientRepository, storageService, documentMapper);
    }

    @Test
    @DisplayName("Successfully uploads new medical document")
    void uploadDocument_Success() {
        String patientId = "pat-100";
        UploadDocumentRequest request = new UploadDocumentRequest(
                "Chest X-Ray Diagnostic",
                DocumentType.DIAGNOSTIC_REPORT,
                patientId,
                "Bilateral clear lung fields",
                "enc-1",
                "CONSULTATION"
        );

        MockMultipartFile file = new MockMultipartFile("file", "xray.png", "image/png", "scan-bytes".getBytes());
        StoredFile stored = new StoredFile("xray.png", "stored-123.png", "patients/pat-100/2026/stored-123.png", "image/png", 10L, "hash123");

        when(patientRepository.existsById(patientId)).thenReturn(true);
        when(storageService.store(eq(file), anyString())).thenReturn(stored);
        when(documentRepository.existsByDocumentNumber(anyString())).thenReturn(false);
        when(documentRepository.save(any(MedicalDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DocumentResponse response = documentService.uploadDocument(file, request, "doc-user");

        assertThat(response).isNotNull();
        assertThat(response.title()).isEqualTo("Chest X-Ray Diagnostic");
        assertThat(response.documentType()).isEqualTo(DocumentType.DIAGNOSTIC_REPORT);
        assertThat(response.patientId()).isEqualTo(patientId);
        assertThat(response.documentVersion()).isEqualTo(1);
        assertThat(response.parentDocumentId()).isNull();
        assertThat(response.status()).isEqualTo(DocumentStatus.ACTIVE);
        assertThat(response.checksumSha256()).isEqualTo("hash123");
    }

    @Test
    @DisplayName("Fails upload when patient ID does not exist")
    void uploadDocument_PatientNotFound_ThrowsException() {
        UploadDocumentRequest request = new UploadDocumentRequest(
                "Lab Result",
                DocumentType.LAB_REPORT,
                "missing-patient",
                "Description",
                null,
                null
        );
        MockMultipartFile file = new MockMultipartFile("file", "lab.pdf", "application/pdf", "data".getBytes());

        when(patientRepository.existsById("missing-patient")).thenReturn(false);

        assertThatThrownBy(() -> documentService.uploadDocument(file, request, "doc-1"))
                .isInstanceOf(PatientNotFoundException.class);
    }

    @Test
    @DisplayName("Uploads new version, increments version number, and supersedes previous active version")
    void uploadNewVersion_Success() {
        String parentId = "doc-v1";
        MedicalDocument v1 = new MedicalDocument(
                parentId,
                "DOC-202609-0001",
                "Initial Discharge Summary",
                DocumentType.DISCHARGE_SUMMARY,
                "pat-100",
                "doc-1",
                "summary_v1.pdf",
                "application/pdf",
                500L,
                "hash1",
                "records/summary_v1.pdf",
                1,
                null,
                DocumentStatus.ACTIVE,
                "Initial draft",
                null,
                null
        );

        UploadDocumentVersionRequest versionRequest = new UploadDocumentVersionRequest(
                "Final Discharge Summary with Cardiology Notes",
                "Approved and finalized"
        );
        MockMultipartFile file = new MockMultipartFile("file", "summary_v2.pdf", "application/pdf", "v2-data".getBytes());
        StoredFile stored = new StoredFile("summary_v2.pdf", "stored-v2.pdf", "patients/pat-100/2026/stored-v2.pdf", "application/pdf", 600L, "hash2");

        when(documentRepository.findByIdForUpdate(parentId)).thenReturn(Optional.of(v1));
        when(documentRepository.findAllVersionsForRoot(parentId)).thenReturn(List.of(v1));
        when(storageService.store(eq(file), anyString())).thenReturn(stored);
        when(documentRepository.existsByDocumentNumber(anyString())).thenReturn(false);
        when(documentRepository.save(any(MedicalDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DocumentResponse response = documentService.uploadNewVersion(parentId, file, versionRequest, "doc-2");

        assertThat(response).isNotNull();
        assertThat(response.title()).isEqualTo("Final Discharge Summary with Cardiology Notes");
        assertThat(response.documentVersion()).isEqualTo(2);
        assertThat(response.parentDocumentId()).isEqualTo(parentId);
        assertThat(response.status()).isEqualTo(DocumentStatus.ACTIVE);

        // Previous active document should be archived
        assertThat(v1.getStatus()).isEqualTo(DocumentStatus.ARCHIVED);
    }

    @Test
    @DisplayName("Throws exception when attempting to version a deleted document")
    void uploadNewVersion_DeletedDocument_ThrowsException() {
        MedicalDocument deletedDoc = new MedicalDocument(
                "doc-del", "DOC-001", "Lab Report", DocumentType.LAB_REPORT,
                "pat-100", "user-1", "lab.pdf", "application/pdf", 100L,
                "hash", "path", 1, null, DocumentStatus.DELETED, null, null, null
        );

        MockMultipartFile file = new MockMultipartFile("file", "lab_v2.pdf", "application/pdf", "new data".getBytes());

        when(documentRepository.findByIdForUpdate("doc-del")).thenReturn(Optional.of(deletedDoc));

        assertThatThrownBy(() -> documentService.uploadNewVersion("doc-del", file, null, "user-2"))
                .isInstanceOf(InvalidDocumentStateException.class)
                .hasMessageContaining("Cannot create a new revision of a deleted document");
    }

    @Test
    @DisplayName("Loads resource when downloading valid active document")
    void downloadDocument_Success() {
        MedicalDocument doc = new MedicalDocument(
                "doc-1", "DOC-001", "Prescription", DocumentType.PRESCRIPTION,
                "pat-100", "doc-1", "rx.pdf", "application/pdf", 200L,
                "hash-rx", "records/rx.pdf", 1, null, DocumentStatus.ACTIVE, null, null, null
        );

        Resource mockResource = new ByteArrayResource("PDF-DATA".getBytes(StandardCharsets.UTF_8));

        when(documentRepository.findById("doc-1")).thenReturn(Optional.of(doc));
        when(storageService.loadAsResource("records/rx.pdf")).thenReturn(mockResource);

        Resource result = documentService.downloadDocument("doc-1");
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(mockResource);
    }

    @Test
    @DisplayName("Throws exception when downloading deleted document")
    void downloadDocument_Deleted_ThrowsException() {
        MedicalDocument doc = new MedicalDocument(
                "doc-1", "DOC-001", "Prescription", DocumentType.PRESCRIPTION,
                "pat-100", "doc-1", "rx.pdf", "application/pdf", 200L,
                "hash-rx", "records/rx.pdf", 1, null, DocumentStatus.DELETED, null, null, null
        );

        when(documentRepository.findById("doc-1")).thenReturn(Optional.of(doc));

        assertThatThrownBy(() -> documentService.downloadDocument("doc-1"))
                .isInstanceOf(InvalidDocumentStateException.class)
                .hasMessageContaining("Cannot download deleted document");
    }

    @Test
    @DisplayName("Throws DocumentNotFoundException when document does not exist")
    void getDocumentById_NotFound_ThrowsException() {
        when(documentRepository.findById("missing-doc")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentService.getDocumentById("missing-doc"))
                .isInstanceOf(DocumentNotFoundException.class);
    }

    @Test
    @DisplayName("Archives active document successfully")
    void archiveDocument_Success() {
        MedicalDocument doc = new MedicalDocument(
                "doc-1", "DOC-001", "Consent Form", DocumentType.CONSENT_FORM,
                "pat-100", "staff-1", "consent.pdf", "application/pdf", 300L,
                "hash", "path", 1, null, DocumentStatus.ACTIVE, null, null, null
        );

        when(documentRepository.findById("doc-1")).thenReturn(Optional.of(doc));
        when(documentRepository.save(any(MedicalDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DocumentResponse response = documentService.archiveDocument("doc-1");
        assertThat(response.status()).isEqualTo(DocumentStatus.ARCHIVED);
    }

    @Test
    @DisplayName("Soft-deletes document successfully")
    void deleteDocument_Success() {
        MedicalDocument doc = new MedicalDocument(
                "doc-1", "DOC-001", "Consent Form", DocumentType.CONSENT_FORM,
                "pat-100", "staff-1", "consent.pdf", "application/pdf", 300L,
                "hash", "path", 1, null, DocumentStatus.ACTIVE, null, null, null
        );

        when(documentRepository.findById("doc-1")).thenReturn(Optional.of(doc));

        documentService.deleteDocument("doc-1");
        assertThat(doc.getStatus()).isEqualTo(DocumentStatus.DELETED);
        verify(documentRepository).save(doc);
    }

    @Test
    @DisplayName("Retrieves version history for document lineage")
    void getDocumentHistory_Success() {
        MedicalDocument v1 = new MedicalDocument("doc-1", "DOC-1", "V1", DocumentType.LAB_REPORT, "p1", "u1", "f1", "application/pdf", 10L, "h1", "p1", 1, null, DocumentStatus.ARCHIVED, null, null, null);
        MedicalDocument v2 = new MedicalDocument("doc-2", "DOC-2", "V2", DocumentType.LAB_REPORT, "p1", "u1", "f2", "application/pdf", 12L, "h2", "p2", 2, "doc-1", DocumentStatus.ACTIVE, null, null, null);

        when(documentRepository.findById("doc-2")).thenReturn(Optional.of(v2));
        when(documentRepository.findVersionHistory("doc-1")).thenReturn(List.of(v1, v2));

        List<DocumentResponse> history = documentService.getDocumentHistory("doc-2");
        assertThat(history).hasSize(2);
        assertThat(history.get(0).documentVersion()).isEqualTo(1);
        assertThat(history.get(1).documentVersion()).isEqualTo(2);
    }

    @Test
    @DisplayName("Retrieves paginated documents for patient")
    void getPatientDocuments_Success() {
        MedicalDocument doc = new MedicalDocument("doc-1", "DOC-1", "V1", DocumentType.LAB_REPORT, "pat-100", "u1", "f1", "application/pdf", 10L, "h1", "p1", 1, null, DocumentStatus.ACTIVE, null, null, null);
        Page<MedicalDocument> page = new PageImpl<>(List.of(doc), PageRequest.of(0, 10), 1);

        when(patientRepository.existsById("pat-100")).thenReturn(true);
        when(documentRepository.findByPatientIdAndDocumentTypeAndStatus(eq("pat-100"), eq(DocumentType.LAB_REPORT), eq(DocumentStatus.ACTIVE), any(Pageable.class)))
                .thenReturn(page);

        PageResponse<DocumentResponse> result = documentService.getPatientDocuments("pat-100", DocumentType.LAB_REPORT, DocumentStatus.ACTIVE, PageRequest.of(0, 10));
        assertThat(result.content()).hasSize(1);
        assertThat(result.totalElements()).isEqualTo(1);
    }
}
