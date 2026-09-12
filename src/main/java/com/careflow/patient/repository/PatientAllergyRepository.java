package com.careflow.patient.repository;

import com.careflow.patient.domain.AllergySeverity;
import com.careflow.patient.domain.AllergyStatus;
import com.careflow.patient.domain.PatientAllergy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA Repository for PatientAllergy entities (§11, §16).
 */
@Repository
public interface PatientAllergyRepository extends JpaRepository<PatientAllergy, String> {

    List<PatientAllergy> findByPatientIdOrderByCreatedAtDesc(String patientId);

    List<PatientAllergy> findByPatientIdAndStatusOrderByCreatedAtDesc(String patientId, AllergyStatus status);

    Optional<PatientAllergy> findByIdAndPatientId(String id, String patientId);

    boolean existsByPatientIdAndAllergenIgnoreCaseAndStatus(String patientId, String allergen, AllergyStatus status);

    @Query("SELECT COUNT(pa) FROM PatientAllergy pa WHERE pa.patientId = :patientId AND pa.status = :status AND pa.severity IN :severities")
    long countHighRiskAllergies(
            @Param("patientId") String patientId,
            @Param("status") AllergyStatus status,
            @Param("severities") Collection<AllergySeverity> severities
    );
}
