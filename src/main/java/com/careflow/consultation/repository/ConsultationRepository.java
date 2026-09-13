package com.careflow.consultation.repository;

import com.careflow.consultation.domain.Consultation;
import com.careflow.consultation.domain.ConsultationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;

/**
 * Spring Data JPA repository for Consultation aggregate root (§22, §103 Phase 7).
 */
@Repository
public interface ConsultationRepository extends JpaRepository<Consultation, String> {

    @EntityGraph(attributePaths = {"diagnoses"})
    Optional<Consultation> findWithDiagnosesById(String id);

    Page<Consultation> findByPatientIdOrderByStartedAtDesc(String patientId, Pageable pageable);

    Page<Consultation> findByDoctorIdOrderByStartedAtDesc(String doctorId, Pageable pageable);

    Page<Consultation> findByStatusOrderByStartedAtDesc(ConsultationStatus status, Pageable pageable);

    Optional<Consultation> findByAppointmentId(String appointmentId);

    Optional<Consultation> findByQueueEntryId(String queueEntryId);

    boolean existsByPatientIdAndStatusIn(String patientId, Collection<ConsultationStatus> statuses);

    boolean existsByDoctorIdAndStatusIn(String doctorId, Collection<ConsultationStatus> statuses);
}
