package com.careflow.pharmacy.repository;

import com.careflow.pharmacy.domain.DispenseRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for DispenseRecord entity (§25).
 */
@Repository
public interface DispenseRecordRepository extends JpaRepository<DispenseRecord, String> {

    List<DispenseRecord> findByPrescriptionIdOrderByDispensedAtAsc(String prescriptionId);

    List<DispenseRecord> findByPrescriptionItemId(String prescriptionItemId);

    Page<DispenseRecord> findByPharmacistIdOrderByDispensedAtDesc(String pharmacistId, Pageable pageable);
}
