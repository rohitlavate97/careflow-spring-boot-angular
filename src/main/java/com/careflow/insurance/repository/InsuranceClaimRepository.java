package com.careflow.insurance.repository;

import com.careflow.insurance.domain.ClaimStatus;
import com.careflow.insurance.domain.InsuranceClaim;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for Insurance Claim aggregate with pessimistic locking (§33, §92).
 */
@Repository
public interface InsuranceClaimRepository extends JpaRepository<InsuranceClaim, String> {

    Optional<InsuranceClaim> findByClaimNumber(String claimNumber);

    /**
     * Acquires a pessimistic write lock (SELECT ... FOR UPDATE) on the claim row,
     * serializing concurrent adjudication or settlement updates (§33, §92).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM InsuranceClaim c WHERE c.id = :id")
    Optional<InsuranceClaim> findByIdForUpdate(@Param("id") String id);

    Page<InsuranceClaim> findByPatientId(String patientId, Pageable pageable);

    Page<InsuranceClaim> findByPolicyId(String policyId, Pageable pageable);

    Page<InsuranceClaim> findByStatus(ClaimStatus status, Pageable pageable);

    List<InsuranceClaim> findByInvoiceId(String invoiceId);

    @Query("SELECT COUNT(c), COALESCE(SUM(c.totalClaimedAmount), 0), COALESCE(SUM(c.approvedAmount), 0), " +
           "SUM(CASE WHEN c.status = com.careflow.insurance.domain.ClaimStatus.APPROVED THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN c.status = com.careflow.insurance.domain.ClaimStatus.REJECTED THEN 1 ELSE 0 END) " +
           "FROM InsuranceClaim c WHERE c.createdAt >= :start AND c.createdAt <= :end")
    List<Object[]> getInsuranceClaimAggregates(@Param("start") java.time.Instant start, @Param("end") java.time.Instant end);
}
