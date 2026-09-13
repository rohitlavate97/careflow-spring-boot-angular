package com.careflow.document.controller;

import com.careflow.document.domain.DocumentStatus;
import com.careflow.document.domain.DocumentType;
import com.careflow.document.domain.MedicalDocument;
import com.careflow.document.dto.UploadDocumentRequest;
import com.careflow.document.dto.UploadDocumentVersionRequest;
import com.careflow.document.repository.MedicalDocumentRepository;
import com.careflow.document.storage.StorageService;
import com.careflow.document.storage.StoredFile;
import com.careflow.patient.domain.Gender;
import com.careflow.patient.domain.Patient;
import com.careflow.patient.repository.PatientRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class DocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private MedicalDocumentRepository documentRepository;

    @MockBean
    private StorageService storageService;

    private Patient patient;
    private MedicalDocument activeDoc;

    @BeforeEach
    void setUp() {
        patient = patientRepository.save(new Patient(
                UUID.randomUUID().toString(),
                "MRN-DOC-CTRL-" + UUID.randomUUID().toString().substring(0, 4),
                "Alan", "Turing", LocalDate.of(1980, 6, 23), Gender.MALE, "+1-555-0123"
        ));

        activeDoc = documentRepository.save(new MedicalDocument(
                UUID.randomUUID().toString(),
                "DOC-202609-00998877",
                "MRI Brain Scan Report",
                DocumentType.DIAGNOSTIC_REPORT,
                patient.getId(),
                "doc-turing",
                "brain_scan.pdf",
                "application/pdf",
                2048L,
                "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                "patients/" + patient.getId() + "/2026/brain_scan.pdf",
                1,
                null,
                DocumentStatus.ACTIVE,
                "Brain MRI without contrast",
                "enc-99",
                "CONSULTATION"
        ));

        StoredFile storedMock = new StoredFile(
                "test.pdf",
                "uuid_test.pdf",
                "patients/" + patient.getId() + "/2026/uuid_test.pdf",
                "application/pdf",
                1024L,
                "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
        );
        when(storageService.store(any(), anyString())).thenReturn(storedMock);
        when(storageService.loadAsResource(anyString()))
                .thenReturn(new ByteArrayResource("SAMPLE_PDF_CONTENT".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    @DisplayName("POST /api/v1/documents/upload - Doctor uploads document returns 201 Created")
    @WithMockUser(username = "dr.house", roles = "DOCTOR")
    void uploadDocument_DoctorRole_Returns201() throws Exception {
        UploadDocumentRequest request = new UploadDocumentRequest(
                "Blood Test Results",
                DocumentType.LAB_REPORT,
                patient.getId(),
                "Fasting glucose and lipid panel",
                "ord-123",
                "LAB_ORDER"
        );

        MockMultipartFile filePart = new MockMultipartFile(
                "file", "blood_test.pdf", "application/pdf", "fake-pdf-data".getBytes(StandardCharsets.UTF_8));
        MockMultipartFile metaPart = new MockMultipartFile(
                "metadata", "", MediaType.APPLICATION_JSON_VALUE, objectMapper.writeValueAsBytes(request));

        mockMvc.perform(multipart("/api/v1/documents/upload")
                        .file(filePart)
                        .file(metaPart))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.title").value("Blood Test Results"))
                .andExpect(jsonPath("$.documentType").value("LAB_REPORT"))
                .andExpect(jsonPath("$.patientId").value(patient.getId()))
                .andExpect(jsonPath("$.documentVersion").value(1))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.checksumSha256").isString());
    }

    @Test
    @DisplayName("POST /api/v1/documents/upload - Patient role is forbidden from uploading clinical documents")
    @WithMockUser(username = "patient.user", roles = "PATIENT")
    void uploadDocument_PatientRole_Returns403() throws Exception {
        UploadDocumentRequest request = new UploadDocumentRequest(
                "Self Report",
                DocumentType.OTHER,
                patient.getId(),
                "Description",
                null,
                null
        );

        MockMultipartFile filePart = new MockMultipartFile("file", "doc.pdf", "application/pdf", "data".getBytes());
        MockMultipartFile metaPart = new MockMultipartFile("metadata", "", MediaType.APPLICATION_JSON_VALUE, objectMapper.writeValueAsBytes(request));

        mockMvc.perform(multipart("/api/v1/documents/upload")
                        .file(filePart)
                        .file(metaPart))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/documents/upload - Unauthenticated request returns 401")
    void uploadDocument_Unauthenticated_Returns401() throws Exception {
        MockMultipartFile filePart = new MockMultipartFile("file", "doc.pdf", "application/pdf", "data".getBytes());
        MockMultipartFile metaPart = new MockMultipartFile("metadata", "", MediaType.APPLICATION_JSON_VALUE, "{}".getBytes());

        mockMvc.perform(multipart("/api/v1/documents/upload")
                        .file(filePart)
                        .file(metaPart))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/v1/documents/{id}/versions - Upload new version creates revision 2 and archives revision 1")
    @WithMockUser(username = "dr.house", roles = "DOCTOR")
    void uploadNewVersion_DoctorRole_Returns201() throws Exception {
        UploadDocumentVersionRequest versionRequest = new UploadDocumentVersionRequest(
                "MRI Brain Scan Report - Contrast Enhanced",
                "Updated with post-contrast axial T1 sequences"
        );

        MockMultipartFile filePart = new MockMultipartFile(
                "file", "brain_scan_v2.pdf", "application/pdf", "new-pdf-bytes".getBytes());
        MockMultipartFile metaPart = new MockMultipartFile(
                "metadata", "", MediaType.APPLICATION_JSON_VALUE, objectMapper.writeValueAsBytes(versionRequest));

        mockMvc.perform(multipart("/api/v1/documents/{id}/versions", activeDoc.getId())
                        .file(filePart)
                        .file(metaPart))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.documentVersion").value(2))
                .andExpect(jsonPath("$.parentDocumentId").value(activeDoc.getId()))
                .andExpect(jsonPath("$.title").value("MRI Brain Scan Report - Contrast Enhanced"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("GET /api/v1/documents/{id} - Doctor retrieves document metadata successfully")
    @WithMockUser(username = "dr.house", roles = "DOCTOR")
    void getDocumentById_DoctorRole_Returns200() throws Exception {
        mockMvc.perform(get("/api/v1/documents/{id}", activeDoc.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(activeDoc.getId()))
                .andExpect(jsonPath("$.documentNumber").value(activeDoc.getDocumentNumber()))
                .andExpect(jsonPath("$.title").value("MRI Brain Scan Report"));
    }

    @Test
    @DisplayName("GET /api/v1/documents/{id}/download - Downloads binary stream with Content-Disposition")
    @WithMockUser(username = "dr.house", roles = "DOCTOR")
    void downloadDocument_DoctorRole_Returns200() throws Exception {
        mockMvc.perform(get("/api/v1/documents/{id}/download", activeDoc.getId()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"brain_scan.pdf\""))
                .andExpect(header().string("X-Checksum-SHA256", activeDoc.getChecksumSha256()));
    }

    @Test
    @DisplayName("GET /api/v1/documents/patient/{patientId} - Retrieves paginated documents for patient")
    @WithMockUser(username = "dr.house", roles = "DOCTOR")
    void getPatientDocuments_DoctorRole_Returns200() throws Exception {
        mockMvc.perform(get("/api/v1/documents/patient/{patientId}", patient.getId())
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(activeDoc.getId()));
    }

    @Test
    @DisplayName("GET /api/v1/documents/{id}/history - Retrieves revision history lineage")
    @WithMockUser(username = "dr.house", roles = "DOCTOR")
    void getDocumentHistory_DoctorRole_Returns200() throws Exception {
        mockMvc.perform(get("/api/v1/documents/{id}/history", activeDoc.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].documentVersion").value(1))
                .andExpect(jsonPath("$[0].id").value(activeDoc.getId()));
    }

    @Test
    @DisplayName("PATCH /api/v1/documents/{id}/archive - Archives active document")
    @WithMockUser(username = "dr.house", roles = "DOCTOR")
    void archiveDocument_DoctorRole_Returns200() throws Exception {
        mockMvc.perform(patch("/api/v1/documents/{id}/archive", activeDoc.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ARCHIVED"));
    }

    @Test
    @DisplayName("DELETE /api/v1/documents/{id} - Doctor cannot delete document (403 Forbidden)")
    @WithMockUser(username = "dr.house", roles = "DOCTOR")
    void deleteDocument_DoctorRole_Returns403() throws Exception {
        mockMvc.perform(delete("/api/v1/documents/{id}", activeDoc.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /api/v1/documents/{id} - Admin soft-deletes document successfully (204 No Content)")
    @WithMockUser(username = "admin.user", roles = "ADMIN")
    void deleteDocument_AdminRole_Returns204() throws Exception {
        mockMvc.perform(delete("/api/v1/documents/{id}", activeDoc.getId()))
                .andExpect(status().isNoContent());

        MedicalDocument updated = documentRepository.findById(activeDoc.getId()).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(updated.getStatus()).isEqualTo(DocumentStatus.DELETED);
    }
}
