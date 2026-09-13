package com.careflow.consultation.repository;

import com.careflow.consultation.domain.ConsultationDiagnosis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for ConsultationDiagnosis entity (§22).
 */
@Repository
public interface ConsultationDiagnosisRepository extends JpaRepository<ConsultationDiagnosis, String> {

    List<ConsultationDiagnosis> findByConsultationId(String consultationId);

    List<ConsultationDiagnosis> findByConsultationPatientId(String patientId);
}
