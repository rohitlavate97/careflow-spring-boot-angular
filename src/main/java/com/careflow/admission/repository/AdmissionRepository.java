package com.careflow.admission.repository;

import com.careflow.admission.domain.Admission;
import com.careflow.admission.domain.AdmissionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for inpatient admissions (§28, §96).
 */
@Repository
public interface AdmissionRepository extends JpaRepository<Admission, String> {

    Optional<Admission> findByAdmissionNumber(String admissionNumber);

    Page<Admission> findByPatientIdOrderByAdmittedAtDesc(String patientId, Pageable pageable);

    Page<Admission> findByStatusOrderByAdmittedAtDesc(AdmissionStatus status, Pageable pageable);

    Page<Admission> findByAdmittingDoctorIdOrderByAdmittedAtDesc(String admittingDoctorId, Pageable pageable);

    boolean existsByPatientIdAndStatusIn(String patientId, Collection<AdmissionStatus> statuses);

    @Query("SELECT a FROM Admission a WHERE a.patientId = :patientId AND a.status IN :statuses")
    Optional<Admission> findActiveAdmissionByPatientId(@Param("patientId") String patientId,
                                                      @Param("statuses") Collection<AdmissionStatus> statuses);

    @Query("SELECT a FROM Admission a LEFT JOIN FETCH a.currentBed b LEFT JOIN FETCH b.room r LEFT JOIN FETCH r.ward w WHERE a.id = :id")
    Optional<Admission> findByIdWithBedDetails(@Param("id") String id);

    long countByStatus(AdmissionStatus status);

    @Query("SELECT a.status, COUNT(a) FROM Admission a GROUP BY a.status")
    List<Object[]> countAdmissionsByStatus();
}
