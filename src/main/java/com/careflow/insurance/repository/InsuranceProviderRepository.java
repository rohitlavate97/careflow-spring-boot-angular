package com.careflow.insurance.repository;

import com.careflow.insurance.domain.InsuranceProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for Insurance Provider / Payer directory (§33).
 */
@Repository
public interface InsuranceProviderRepository extends JpaRepository<InsuranceProvider, String> {

    Optional<InsuranceProvider> findByProviderCode(String providerCode);

    Optional<InsuranceProvider> findByPayerId(String payerId);

    List<InsuranceProvider> findByActiveTrue();
}
