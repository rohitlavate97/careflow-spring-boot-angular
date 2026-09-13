package com.careflow.pharmacy.repository;

import com.careflow.pharmacy.domain.PharmacyInventoryBatch;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for PharmacyInventoryBatch entity supporting
 * pessimistic write locks and First-Expiring, First-Out (FEFO) retrieval (§25, §26, §92).
 */
@Repository
public interface PharmacyInventoryBatchRepository extends JpaRepository<PharmacyInventoryBatch, String> {

    /**
     * Acquires a row-level exclusive pessimistic write lock (SELECT ... FOR UPDATE)
     * on the inventory batch during stock deduction (§26, §92).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM PharmacyInventoryBatch b WHERE b.id = :id")
    Optional<PharmacyInventoryBatch> findByIdForUpdate(@Param("id") String id);

    /**
     * Queries active non-expired batches for a medication sorted by earliest expiration date (FEFO).
     */
    List<PharmacyInventoryBatch> findByMedicationIdAndExpiryDateAfterAndQuantityAvailableGreaterThanOrderByExpiryDateAsc(
            String medicationId, LocalDate today, int minQuantity);

    List<PharmacyInventoryBatch> findByMedicationIdOrderByExpiryDateAsc(String medicationId);

    Optional<PharmacyInventoryBatch> findByMedicationIdAndBatchNumber(String medicationId, String batchNumber);
}
