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

    @Query("SELECT COUNT(b), COALESCE(SUM(b.quantityAvailable), 0), " +
           "SUM(CASE WHEN b.expiryDate < :currentDate THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN b.expiryDate >= :currentDate AND b.expiryDate <= :nearExpiryDate THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN b.quantityAvailable <= b.reorderThreshold THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN b.quantityAvailable = 0 THEN 1 ELSE 0 END) " +
           "FROM PharmacyInventoryBatch b")
    List<Object[]> getPharmacyInventoryAggregates(@Param("currentDate") LocalDate currentDate, @Param("nearExpiryDate") LocalDate nearExpiryDate);

    @Query("SELECT b FROM PharmacyInventoryBatch b WHERE b.quantityAvailable <= b.reorderThreshold ORDER BY b.quantityAvailable ASC")
    List<PharmacyInventoryBatch> findCriticalStockBatches(org.springframework.data.domain.Pageable pageable);

    @Query("SELECT COUNT(b) FROM PharmacyInventoryBatch b WHERE b.quantityAvailable <= b.reorderThreshold OR b.expiryDate <= :nearExpiryDate")
    long countStockAlerts(@Param("nearExpiryDate") LocalDate nearExpiryDate);
}
