package com.careflow.document.repository;

import com.careflow.document.domain.DocumentStatus;
import com.careflow.document.domain.DocumentType;
import com.careflow.document.domain.MedicalDocument;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for MedicalDocument entities (§34, §103).
 */
@Repository
public interface MedicalDocumentRepository extends JpaRepository<MedicalDocument, String> {

    Optional<MedicalDocument> findByDocumentNumber(String documentNumber);

    boolean existsByDocumentNumber(String documentNumber);

    Page<MedicalDocument> findByPatientIdAndStatus(String patientId, DocumentStatus status, Pageable pageable);

    Page<MedicalDocument> findByPatientIdAndDocumentTypeAndStatus(
            String patientId,
            DocumentType documentType,
            DocumentStatus status,
            Pageable pageable
    );

    @Query("SELECT d FROM MedicalDocument d WHERE (d.id = :rootId OR d.parentDocumentId = :rootId) AND d.status <> 'DELETED' ORDER BY d.documentVersion ASC")
    List<MedicalDocument> findVersionHistory(@Param("rootId") String rootId);

    @Query("SELECT d FROM MedicalDocument d WHERE (d.id = :rootId OR d.parentDocumentId = :rootId) ORDER BY d.documentVersion DESC")
    List<MedicalDocument> findAllVersionsForRoot(@Param("rootId") String rootId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM MedicalDocument d WHERE d.id = :id")
    Optional<MedicalDocument> findByIdForUpdate(@Param("id") String id);

    long countByPatientIdAndStatus(String patientId, DocumentStatus status);

    long countByDocumentType(DocumentType documentType);
}
