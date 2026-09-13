package com.careflow.prescription.repository;

import com.careflow.prescription.domain.PrescriptionItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for PrescriptionItem entity (§24).
 */
@Repository
public interface PrescriptionItemRepository extends JpaRepository<PrescriptionItem, String> {

    List<PrescriptionItem> findByPrescriptionId(String prescriptionId);

    List<PrescriptionItem> findByMedicationId(String medicationId);
}
