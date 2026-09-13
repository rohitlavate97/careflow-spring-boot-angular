package com.careflow.insurance.repository;

import com.careflow.insurance.domain.InsurancePolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for patient insurance policies (§33).
 */
@Repository
public interface InsurancePolicyRepository extends JpaRepository<InsurancePolicy, String> {

    List<InsurancePolicy> findByPatientId(String patientId);

    List<InsurancePolicy> findByPatientIdAndActiveTrue(String patientId);

    Optional<InsurancePolicy> findByPolicyNumber(String policyNumber);
}
