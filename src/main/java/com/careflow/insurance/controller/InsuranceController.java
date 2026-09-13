package com.careflow.insurance.controller;

import com.careflow.common.dto.PageResponse;
import com.careflow.insurance.domain.ClaimStatus;
import com.careflow.insurance.dto.AdjudicateClaimRequest;
import com.careflow.insurance.dto.ClaimSummaryResponse;
import com.careflow.insurance.dto.CreateClaimRequest;
import com.careflow.insurance.dto.CreateInsurancePolicyRequest;
import com.careflow.insurance.dto.CreateInsuranceProviderRequest;
import com.careflow.insurance.dto.InsuranceClaimResponse;
import com.careflow.insurance.dto.InsurancePolicyResponse;
import com.careflow.insurance.dto.InsuranceProviderResponse;
import com.careflow.insurance.service.InsuranceService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * REST controller for Health Insurance Providers, Patient Policies, and Claims Adjudication (§33, §91).
 */
@RestController
@RequestMapping("/api/v1/insurance")
@Validated
public class InsuranceController {

    private final InsuranceService insuranceService;

    public InsuranceController(InsuranceService insuranceService) {
        this.insuranceService = insuranceService;
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Insurance Providers
    // -----------------------------------------------------------------------------------------------------------------

    @PostMapping("/providers")
    @PreAuthorize("hasAnyRole('BILLING_OFFICER', 'ADMIN')")
    public ResponseEntity<InsuranceProviderResponse> createProvider(
            @Valid @RequestBody CreateInsuranceProviderRequest request) {
        InsuranceProviderResponse response = insuranceService.createProvider(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/providers/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<InsuranceProviderResponse> getProviderById(@PathVariable String id) {
        return ResponseEntity.ok(insuranceService.getProviderById(id));
    }

    @GetMapping("/providers")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<InsuranceProviderResponse>> getAllActiveProviders() {
        return ResponseEntity.ok(insuranceService.getAllActiveProviders());
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Patient Policies
    // -----------------------------------------------------------------------------------------------------------------

    @PostMapping("/policies")
    @PreAuthorize("hasAnyRole('BILLING_OFFICER', 'ADMIN')")
    public ResponseEntity<InsurancePolicyResponse> createPolicy(
            @Valid @RequestBody CreateInsurancePolicyRequest request) {
        InsurancePolicyResponse response = insuranceService.createPolicy(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/policies/{id}")
    @PreAuthorize("hasAnyRole('BILLING_OFFICER', 'ADMIN', 'PATIENT')")
    public ResponseEntity<InsurancePolicyResponse> getPolicyById(@PathVariable String id) {
        return ResponseEntity.ok(insuranceService.getPolicyById(id));
    }

    @GetMapping("/policies/patient/{patientId}")
    @PreAuthorize("hasAnyRole('BILLING_OFFICER', 'ADMIN', 'PATIENT', 'DOCTOR')")
    public ResponseEntity<List<InsurancePolicyResponse>> getPoliciesByPatient(@PathVariable String patientId) {
        return ResponseEntity.ok(insuranceService.getPoliciesByPatient(patientId));
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Claims Lifecycle Endpoints
    // -----------------------------------------------------------------------------------------------------------------

    @PostMapping("/claims")
    @PreAuthorize("hasAnyRole('BILLING_OFFICER', 'ADMIN')")
    public ResponseEntity<InsuranceClaimResponse> createClaim(@Valid @RequestBody CreateClaimRequest request) {
        InsuranceClaimResponse response = insuranceService.createClaim(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PostMapping("/claims/{id}/submit")
    @PreAuthorize("hasAnyRole('BILLING_OFFICER', 'ADMIN')")
    public ResponseEntity<InsuranceClaimResponse> submitClaim(@PathVariable String id) {
        return ResponseEntity.ok(insuranceService.submitClaim(id));
    }

    @PostMapping("/claims/{id}/review")
    @PreAuthorize("hasAnyRole('BILLING_OFFICER', 'ADMIN')")
    public ResponseEntity<InsuranceClaimResponse> startReview(@PathVariable String id) {
        return ResponseEntity.ok(insuranceService.startReview(id));
    }

    @PostMapping("/claims/{id}/adjudicate")
    @PreAuthorize("hasAnyRole('BILLING_OFFICER', 'ADMIN')")
    public ResponseEntity<InsuranceClaimResponse> adjudicateClaim(
            @PathVariable String id,
            @Valid @RequestBody AdjudicateClaimRequest request) {
        return ResponseEntity.ok(insuranceService.adjudicateClaim(id, request));
    }

    @PostMapping("/claims/{id}/settle")
    @PreAuthorize("hasAnyRole('BILLING_OFFICER', 'ADMIN')")
    public ResponseEntity<InsuranceClaimResponse> settleClaim(@PathVariable String id) {
        return ResponseEntity.ok(insuranceService.settleClaim(id));
    }

    @GetMapping("/claims/{id}")
    @PreAuthorize("hasAnyRole('BILLING_OFFICER', 'ADMIN', 'PATIENT', 'DOCTOR')")
    public ResponseEntity<InsuranceClaimResponse> getClaimById(@PathVariable String id) {
        return ResponseEntity.ok(insuranceService.getClaimById(id));
    }

    @GetMapping("/claims/number/{claimNumber}")
    @PreAuthorize("hasAnyRole('BILLING_OFFICER', 'ADMIN')")
    public ResponseEntity<InsuranceClaimResponse> getClaimByNumber(@PathVariable String claimNumber) {
        return ResponseEntity.ok(insuranceService.getClaimByNumber(claimNumber));
    }

    @GetMapping("/claims/patient/{patientId}")
    @PreAuthorize("hasAnyRole('BILLING_OFFICER', 'ADMIN', 'PATIENT', 'DOCTOR')")
    public ResponseEntity<PageResponse<ClaimSummaryResponse>> getClaimsByPatient(
            @PathVariable String patientId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(insuranceService.getClaimsByPatient(patientId, pageable)));
    }

    @GetMapping("/claims")
    @PreAuthorize("hasAnyRole('BILLING_OFFICER', 'ADMIN')")
    public ResponseEntity<PageResponse<ClaimSummaryResponse>> getClaims(
            @RequestParam(required = false) ClaimStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        if (status != null) {
            return ResponseEntity.ok(PageResponse.from(insuranceService.getClaimsByStatus(status, pageable)));
        }
        return ResponseEntity.ok(PageResponse.from(insuranceService.getAllClaims(pageable)));
    }
}
