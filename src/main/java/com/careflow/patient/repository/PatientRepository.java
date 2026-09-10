package com.careflow.patient.repository;

import com.careflow.patient.domain.Patient;
import com.careflow.patient.domain.PatientStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for Patient aggregates supporting pagination,
 * indexed coordinate lookups, and JPA specification search (§16, §71, §72).
 */
@Repository
public interface PatientRepository extends JpaRepository<Patient, String>, JpaSpecificationExecutor<Patient> {

    Optional<Patient> findByMrn(String mrn);

    boolean existsByMrn(String mrn);

    List<Patient> findByPhone(String phone);

    Optional<Patient> findByEmail(String email);

    Page<Patient> findByStatus(PatientStatus status, Pageable pageable);

    @Query("SELECT p FROM Patient p WHERE " +
           "LOWER(p.firstName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.lastName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "p.mrn LIKE CONCAT('%', :query, '%') OR " +
           "p.phone LIKE CONCAT('%', :query, '%')")
    Page<Patient> searchPatients(@Param("query") String query, Pageable pageable);
}
