package com.careflow.clinical.repository;

import com.careflow.clinical.domain.ClinicalNote;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for ClinicalNote entity (§23, §103 Phase 7).
 */
@Repository
public interface ClinicalNoteRepository extends JpaRepository<ClinicalNote, String> {

    Page<ClinicalNote> findByPatientIdOrderByCreatedAtDesc(String patientId, Pageable pageable);

    List<ClinicalNote> findByConsultationIdOrderByCreatedAtAsc(String consultationId);

    Page<ClinicalNote> findByAuthorIdOrderByCreatedAtDesc(String authorId, Pageable pageable);
}
