package com.careflow.document.service;

import com.careflow.document.domain.DocumentStatus;
import com.careflow.document.domain.DocumentType;
import com.careflow.document.dto.DocumentResponse;
import com.careflow.document.dto.UploadDocumentRequest;
import com.careflow.document.dto.UploadDocumentVersionRequest;
import com.careflow.patient.domain.Gender;
import com.careflow.patient.domain.Patient;
import com.careflow.patient.repository.PatientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class DocumentVersioningConcurrencyTest {

    @Autowired
    private DocumentService documentService;

    @Autowired
    private PatientRepository patientRepository;

    private Patient patient;
    private DocumentResponse rootDoc;

    @BeforeEach
    void setUp() {
        patient = patientRepository.save(new Patient(
                UUID.randomUUID().toString(),
                "MRN-DOC-CONC-" + UUID.randomUUID().toString().substring(0, 4),
                "Margaret", "Hamilton", LocalDate.of(1975, 8, 17), Gender.FEMALE, "+1-555-9988"
        ));

        MockMultipartFile initialFile = new MockMultipartFile(
                "file", "initial_spec.pdf", "application/pdf", "v1 content".getBytes()
        );
        UploadDocumentRequest initialRequest = new UploadDocumentRequest(
                "Surgical Pathology Report",
                DocumentType.DIAGNOSTIC_REPORT,
                patient.getId(),
                "Initial biopsy specimen report",
                null,
                null
        );

        rootDoc = documentService.uploadDocument(initialFile, initialRequest, "dr.surgeon");
    }

    @Test
    @DisplayName("Concurrent version uploads execute safely without version collisions")
    void concurrentVersionUploads_SafeExecution() throws InterruptedException {
        int threadCount = 4;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        List<Throwable> exceptions = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    startGate.await();
                    MockMultipartFile file = new MockMultipartFile(
                            "file", "revision_" + index + ".pdf", "application/pdf", ("revision " + index).getBytes()
                    );
                    UploadDocumentVersionRequest versionRequest = new UploadDocumentVersionRequest(
                            "Surgical Pathology Report - Addendum " + index,
                            "Addendum by reviewer " + index
                    );

                    documentService.uploadNewVersion(rootDoc.id(), file, versionRequest, "reviewer-" + index);
                    successCount.incrementAndGet();
                } catch (Throwable t) {
                    exceptions.add(t);
                } finally {
                    endGate.countDown();
                }
            });
        }

        startGate.countDown();
        boolean completed = endGate.await(15, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).isTrue();
        assertThat(successCount.get() + exceptions.size()).isEqualTo(threadCount);

        // Verify version history: all successful versions have unique version numbers
        List<DocumentResponse> history = documentService.getDocumentHistory(rootDoc.id());
        assertThat(history).isNotEmpty();
        List<Integer> versions = history.stream().map(DocumentResponse::documentVersion).toList();
        assertThat(versions).doesNotHaveDuplicates();

        // Exactly one document in the family is ACTIVE
        long activeCount = history.stream().filter(d -> d.status() == DocumentStatus.ACTIVE).count();
        assertThat(activeCount).isEqualTo(1);
    }
}
