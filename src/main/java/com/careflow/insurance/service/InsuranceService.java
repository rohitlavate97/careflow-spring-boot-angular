package com.careflow.insurance.service;

import com.careflow.insurance.domain.ClaimStatus;
import com.careflow.insurance.dto.AdjudicateClaimRequest;
import com.careflow.insurance.dto.ClaimSummaryResponse;
import com.careflow.insurance.dto.CreateClaimRequest;
import com.careflow.insurance.dto.CreateInsurancePolicyRequest;
import com.careflow.insurance.dto.CreateInsuranceProviderRequest;
import com.careflow.insurance.dto.InsuranceClaimResponse;
import com.careflow.insurance.dto.InsurancePolicyResponse;
import com.careflow.insurance.dto.InsuranceProviderResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Service interface for health insurance payers, patient policies, claims submission,
 * and reimbursement adjudication workflows (§33).
 */
public interface InsuranceService {

    // Insurance Providers
    InsuranceProviderResponse createProvider(CreateInsuranceProviderRequest request);
    InsuranceProviderResponse getProviderById(String id);
    List<InsuranceProviderResponse> getAllActiveProviders();

    // Patient Policies
    InsurancePolicyResponse createPolicy(CreateInsurancePolicyRequest request);
    InsurancePolicyResponse getPolicyById(String id);
    List<InsurancePolicyResponse> getPoliciesByPatient(String patientId);

    // Claims Lifecycle
    InsuranceClaimResponse createClaim(CreateClaimRequest request);
    InsuranceClaimResponse submitClaim(String claimId);
    InsuranceClaimResponse startReview(String claimId);
    InsuranceClaimResponse adjudicateClaim(String claimId, AdjudicateClaimRequest request);
    InsuranceClaimResponse settleClaim(String claimId);
    InsuranceClaimResponse getClaimById(String id);
    InsuranceClaimResponse getClaimByNumber(String claimNumber);
    Page<ClaimSummaryResponse> getClaimsByPatient(String patientId, Pageable pageable);
    Page<ClaimSummaryResponse> getClaimsByStatus(ClaimStatus status, Pageable pageable);
    Page<ClaimSummaryResponse> getAllClaims(Pageable pageable);
}
