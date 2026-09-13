package com.careflow.document.storage;

import com.careflow.document.exception.DocumentStorageException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalStorageServiceTest {

    @TempDir
    Path tempDir;

    private LocalStorageService storageService;

    @BeforeEach
    void setUp() {
        storageService = new LocalStorageService(tempDir.toString());
        storageService.init();
    }

    @Test
    @DisplayName("Successfully stores file and calculates valid SHA-256 digest")
    void storeFile_Success() throws IOException {
        byte[] content = "CareFlow Clinical Laboratory Test Results: Normal".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "blood_test_results.pdf",
                "application/pdf",
                content
        );

        StoredFile stored = storageService.store(file, "patients/pat-001/2026");

        assertThat(stored).isNotNull();
        assertThat(stored.originalFileName()).isEqualTo("blood_test_results.pdf");
        assertThat(stored.mimeType()).isEqualTo("application/pdf");
        assertThat(stored.fileSize()).isEqualTo(content.length);
        assertThat(stored.checksumSha256()).isNotBlank();
        assertThat(stored.checksumSha256()).hasSize(64); // SHA-256 hex length
        assertThat(storageService.exists(stored.storagePath())).isTrue();

        Resource loaded = storageService.loadAsResource(stored.storagePath());
        assertThat(loaded.exists()).isTrue();
        assertThat(loaded.getContentAsByteArray()).isEqualTo(content);
    }

    @Test
    @DisplayName("Throws exception when attempting to store null or empty file")
    void storeFile_EmptyFile_ThrowsException() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.txt", "text/plain", new byte[0]);

        assertThatThrownBy(() -> storageService.store(emptyFile, "sub"))
                .isInstanceOf(DocumentStorageException.class)
                .satisfies(e -> {
                    DocumentStorageException dse = (DocumentStorageException) e;
                    assertThat(dse.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                });
    }

    @Test
    @DisplayName("Rejects filenames attempting directory traversal")
    void storeFile_DirectoryTraversalInFilename_ThrowsException() {
        MockMultipartFile malformedFile = new MockMultipartFile(
                "file",
                "../../etc/passwd",
                "text/plain",
                "malicious data".getBytes(StandardCharsets.UTF_8)
        );

        assertThatThrownBy(() -> storageService.store(malformedFile, "sub"))
                .isInstanceOf(DocumentStorageException.class)
                .satisfies(e -> {
                    DocumentStorageException dse = (DocumentStorageException) e;
                    assertThat(dse.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                });
    }

    @Test
    @DisplayName("Throws NOT_FOUND when attempting to load non-existent resource")
    void loadAsResource_NotFound_ThrowsException() {
        assertThatThrownBy(() -> storageService.loadAsResource("non/existent/file.pdf"))
                .isInstanceOf(DocumentStorageException.class)
                .satisfies(e -> {
                    DocumentStorageException dse = (DocumentStorageException) e;
                    assertThat(dse.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                });
    }

    @Test
    @DisplayName("Deletes stored file successfully")
    void deleteFile_Success() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "discharge_notes.txt",
                "text/plain",
                "Patient recovered well.".getBytes(StandardCharsets.UTF_8)
        );

        StoredFile stored = storageService.store(file, "records");
        assertThat(storageService.exists(stored.storagePath())).isTrue();

        storageService.delete(stored.storagePath());
        assertThat(storageService.exists(stored.storagePath())).isFalse();
    }
}
