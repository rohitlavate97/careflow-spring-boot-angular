package com.careflow.prescription.repository;

import com.careflow.prescription.domain.Prescription;
import com.careflow.prescription.domain.PrescriptionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for Prescription aggregate root (§24, §103 Phase 8).
 */
@Repository
public interface PrescriptionRepository extends JpaRepository<Prescription, String> {

    @EntityGraph(attributePaths = {"items"})
    Optional<Prescription> findWithItemsById(String id);

    Page<Prescription> findByPatientIdOrderByPrescribedAtDesc(String patientId, Pageable pageable);

    Page<Prescription> findByDoctorIdOrderByPrescribedAtDesc(String doctorId, Pageable pageable);

    Page<Prescription> findByStatusOrderByPrescribedAtDesc(PrescriptionStatus status, Pageable pageable);

    Optional<Prescription> findByConsultationId(String consultationId);
}
