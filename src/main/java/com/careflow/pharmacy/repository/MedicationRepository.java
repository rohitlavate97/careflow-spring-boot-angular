package com.careflow.pharmacy.repository;

import com.careflow.pharmacy.domain.Medication;
import com.careflow.pharmacy.domain.MedicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for Medication formulary catalog (§25).
 */
@Repository
public interface MedicationRepository extends JpaRepository<Medication, String> {

    Optional<Medication> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    Page<Medication> findByStatus(MedicationStatus status, Pageable pageable);

    Page<Medication> findByNameContainingIgnoreCaseOrGenericNameContainingIgnoreCase(
            String name, String genericName, Pageable pageable);
}
