package com.careflow.document.controller;

import com.careflow.common.dto.PageResponse;
import com.careflow.document.domain.DocumentStatus;
import com.careflow.document.domain.DocumentType;
import com.careflow.document.dto.DocumentResponse;
import com.careflow.document.dto.UploadDocumentRequest;
import com.careflow.document.dto.UploadDocumentVersionRequest;
import com.careflow.document.service.DocumentService;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * REST controller for Medical Document Management, Revisions, and Binary Streaming (§34, §91).
 */
@RestController
@RequestMapping("/api/v1/documents")
@Validated
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('DOCTOR', 'NURSE', 'ADMIN', 'LAB_TECHNICIAN', 'BILLING_OFFICER')")
    public ResponseEntity<DocumentResponse> uploadDocument(
            @RequestPart("file") MultipartFile file,
            @Valid @RequestPart("metadata") UploadDocumentRequest metadata,
            Authentication authentication) {

        String uploaderId = resolvePrincipalName(authentication);
        DocumentResponse response = documentService.uploadDocument(file, metadata, uploaderId);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .replacePath("/api/v1/documents/{id}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @PostMapping(value = "/{id}/versions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('DOCTOR', 'NURSE', 'ADMIN', 'LAB_TECHNICIAN', 'BILLING_OFFICER')")
    public ResponseEntity<DocumentResponse> uploadNewVersion(
            @PathVariable("id") String id,
            @RequestPart("file") MultipartFile file,
            @Valid @RequestPart(value = "metadata", required = false) UploadDocumentVersionRequest metadata,
            Authentication authentication) {

        String uploaderId = resolvePrincipalName(authentication);
        DocumentResponse response = documentService.uploadNewVersion(id, file, metadata, uploaderId);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .replacePath("/api/v1/documents/{id}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.status(HttpStatus.CREATED).location(location).body(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'NURSE', 'ADMIN', 'LAB_TECHNICIAN', 'BILLING_OFFICER', 'PATIENT')")
    public ResponseEntity<DocumentResponse> getDocumentById(@PathVariable("id") String id) {
        return ResponseEntity.ok(documentService.getDocumentById(id));
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("hasAnyRole('DOCTOR', 'NURSE', 'ADMIN', 'LAB_TECHNICIAN', 'BILLING_OFFICER', 'PATIENT')")
    public ResponseEntity<Resource> downloadDocument(@PathVariable("id") String id) {
        DocumentResponse metadata = documentService.getDocumentById(id);
        Resource resource = documentService.downloadDocument(id);

        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(metadata.mimeType());
        } catch (Exception e) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + metadata.fileName() + "\"")
                .header("X-Checksum-SHA256", metadata.checksumSha256())
                .body(resource);
    }

    @GetMapping("/patient/{patientId}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'NURSE', 'ADMIN', 'BILLING_OFFICER', 'PATIENT')")
    public ResponseEntity<PageResponse<DocumentResponse>> getPatientDocuments(
            @PathVariable("patientId") String patientId,
            @RequestParam(value = "documentType", required = false) DocumentType documentType,
            @RequestParam(value = "status", required = false) DocumentStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        return ResponseEntity.ok(documentService.getPatientDocuments(patientId, documentType, status, pageable));
    }

    @GetMapping("/{id}/history")
    @PreAuthorize("hasAnyRole('DOCTOR', 'NURSE', 'ADMIN', 'LAB_TECHNICIAN', 'BILLING_OFFICER', 'PATIENT')")
    public ResponseEntity<List<DocumentResponse>> getDocumentHistory(@PathVariable("id") String id) {
        return ResponseEntity.ok(documentService.getDocumentHistory(id));
    }

    @PatchMapping("/{id}/archive")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<DocumentResponse> archiveDocument(@PathVariable("id") String id) {
        return ResponseEntity.ok(documentService.archiveDocument(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteDocument(@PathVariable("id") String id) {
        documentService.deleteDocument(id);
        return ResponseEntity.noContent().build();
    }

    private String resolvePrincipalName(Authentication authentication) {
        return (authentication != null && authentication.getName() != null)
                ? authentication.getName()
                : "SYSTEM";
    }
}
