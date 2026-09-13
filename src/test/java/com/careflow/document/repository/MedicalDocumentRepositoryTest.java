package com.careflow.document.repository;

import com.careflow.document.domain.DocumentStatus;
import com.careflow.document.domain.DocumentType;
import com.careflow.document.domain.MedicalDocument;
import com.careflow.patient.domain.Gender;
import com.careflow.patient.domain.Patient;
import com.careflow.patient.repository.PatientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class MedicalDocumentRepositoryTest {

    @Autowired
    private MedicalDocumentRepository documentRepository;

    @Autowired
    private PatientRepository patientRepository;

    private Patient patient;
    private MedicalDocument docV1;
    private MedicalDocument docV2;

    @BeforeEach
    void setUp() {
        patient = patientRepository.save(new Patient(
                UUID.randomUUID().toString(), "MRN-DOC-" + UUID.randomUUID().toString().substring(0, 4),
                "Grace", "Hopper", LocalDate.of(1985, 5, 20), Gender.FEMALE, "+1-555-0811"
        ));

        String rootId = UUID.randomUUID().toString();
        docV1 = documentRepository.save(new MedicalDocument(
                rootId,
                "DOC-202609-00000001",
                "CBC Blood Panel Lab Report",
                DocumentType.LAB_REPORT,
                patient.getId(),
                "staff-tech-01",
                "cbc_panel_v1.pdf",
                "application/pdf",
                1024L,
                "a1b2c3d4e5f678901234567890abcdef1234567890abcdef1234567890abcdef",
                "patients/" + patient.getId() + "/2026/cbc_v1.pdf",
                1,
                null,
                DocumentStatus.ARCHIVED,
                "Initial lab results",
                "order-1",
                "LAB_ORDER"
        ));

        docV2 = documentRepository.save(new MedicalDocument(
                UUID.randomUUID().toString(),
                "DOC-202609-00000002",
                "CBC Blood Panel Lab Report - Revised",
                DocumentType.LAB_REPORT,
                patient.getId(),
                "staff-tech-01",
                "cbc_panel_v2.pdf",
                "application/pdf",
                1120L,
                "b2c3d4e5f678901234567890abcdef1234567890abcdef1234567890abcdefa1",
                "patients/" + patient.getId() + "/2026/cbc_v2.pdf",
                2,
                rootId,
                DocumentStatus.ACTIVE,
                "Corrected reference range values",
                "order-1",
                "LAB_ORDER"
        ));
    }

    @Test
    @DisplayName("Finds document by business document number")
    void findByDocumentNumber_Success() {
        Optional<MedicalDocument> found = documentRepository.findByDocumentNumber("DOC-202609-00000001");
        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("CBC Blood Panel Lab Report");
        assertThat(found.get().getDocumentVersion()).isEqualTo(1);
    }

    @Test
    @DisplayName("Finds paginated active documents for patient")
    void findByPatientIdAndStatus_Success() {
        Page<MedicalDocument> activeDocs = documentRepository.findByPatientIdAndStatus(
                patient.getId(), DocumentStatus.ACTIVE, PageRequest.of(0, 10));

        assertThat(activeDocs.getTotalElements()).isEqualTo(1);
        assertThat(activeDocs.getContent().get(0).getId()).isEqualTo(docV2.getId());
    }

    @Test
    @DisplayName("Finds documents filtered by type and status")
    void findByPatientIdAndDocumentTypeAndStatus_Success() {
        Page<MedicalDocument> labReports = documentRepository.findByPatientIdAndDocumentTypeAndStatus(
                patient.getId(), DocumentType.LAB_REPORT, DocumentStatus.ACTIVE, PageRequest.of(0, 10));

        assertThat(labReports.getTotalElements()).isEqualTo(1);

        Page<MedicalDocument> prescriptions = documentRepository.findByPatientIdAndDocumentTypeAndStatus(
                patient.getId(), DocumentType.PRESCRIPTION, DocumentStatus.ACTIVE, PageRequest.of(0, 10));

        assertThat(prescriptions.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("Retrieves version history in ascending version order excluding deleted records")
    void findVersionHistory_Success() {
        List<MedicalDocument> history = documentRepository.findVersionHistory(docV1.getId());
        assertThat(history).hasSize(2);
        assertThat(history.get(0).getDocumentVersion()).isEqualTo(1);
        assertThat(history.get(1).getDocumentVersion()).isEqualTo(2);
    }

    @Test
    @DisplayName("Loads entity with pessimistic write lock")
    void findByIdForUpdate_Success() {
        Optional<MedicalDocument> locked = documentRepository.findByIdForUpdate(docV2.getId());
        assertThat(locked).isPresent();
        assertThat(locked.get().getId()).isEqualTo(docV2.getId());
    }

    @Test
    @DisplayName("Counts documents by patient and type")
    void counts_Success() {
        long activeCount = documentRepository.countByPatientIdAndStatus(patient.getId(), DocumentStatus.ACTIVE);
        long totalLabReports = documentRepository.countByDocumentType(DocumentType.LAB_REPORT);

        assertThat(activeCount).isEqualTo(1);
        assertThat(totalLabReports).isEqualTo(2);
    }
}
