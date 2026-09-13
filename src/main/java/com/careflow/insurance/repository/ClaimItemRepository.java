package com.careflow.insurance.repository;

import com.careflow.insurance.domain.ClaimItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for insurance claim line items (§33).
 */
@Repository
public interface ClaimItemRepository extends JpaRepository<ClaimItem, String> {

    List<ClaimItem> findByClaimId(String claimId);
}
