package com.careflow.insurance.service;

import com.careflow.billing.domain.PaymentMethod;
import com.careflow.billing.dto.ProcessPaymentRequest;
import com.careflow.billing.repository.InvoiceRepository;
import com.careflow.billing.service.BillingService;
import com.careflow.common.exception.BusinessRuleException;
import com.careflow.common.exception.ResourceNotFoundException;
import com.careflow.insurance.domain.ClaimItem;
import com.careflow.insurance.domain.ClaimStatus;
import com.careflow.insurance.domain.InsuranceClaim;
import com.careflow.insurance.domain.InsurancePolicy;
import com.careflow.insurance.domain.InsuranceProvider;
import com.careflow.insurance.dto.AdjudicateClaimRequest;
import com.careflow.insurance.dto.ClaimSummaryResponse;
import com.careflow.insurance.dto.CreateClaimItemRequest;
import com.careflow.insurance.dto.CreateClaimRequest;
import com.careflow.insurance.dto.CreateInsurancePolicyRequest;
import com.careflow.insurance.dto.CreateInsuranceProviderRequest;
import com.careflow.insurance.dto.InsuranceClaimResponse;
import com.careflow.insurance.dto.InsurancePolicyResponse;
import com.careflow.insurance.dto.InsuranceProviderResponse;
import com.careflow.insurance.exception.InsuranceClaimNotFoundException;
import com.careflow.insurance.exception.InsurancePolicyNotFoundException;
import com.careflow.insurance.exception.InsuranceProviderNotFoundException;
import com.careflow.insurance.exception.PolicyCoverageExpiredException;
import com.careflow.insurance.mapper.InsuranceMapper;
import com.careflow.insurance.repository.ClaimItemRepository;
import com.careflow.insurance.repository.InsuranceClaimRepository;
import com.careflow.insurance.repository.InsurancePolicyRepository;
import com.careflow.insurance.repository.InsuranceProviderRepository;
import com.careflow.patient.repository.PatientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * Implementation of third-party insurance management, policy verification,
 * and claims adjudication with concurrency protection (§33, §92).
 */
@Service
@Transactional(readOnly = true)
public class InsuranceServiceImpl implements InsuranceService {

    private static final Logger log = LoggerFactory.getLogger(InsuranceServiceImpl.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final InsuranceProviderRepository providerRepository;
    private final InsurancePolicyRepository policyRepository;
    private final InsuranceClaimRepository claimRepository;
    private final ClaimItemRepository claimItemRepository;
    private final PatientRepository patientRepository;
    private final InvoiceRepository invoiceRepository;
    private final BillingService billingService;
    private final InsuranceMapper insuranceMapper;

    public InsuranceServiceImpl(InsuranceProviderRepository providerRepository,
                                InsurancePolicyRepository policyRepository,
                                InsuranceClaimRepository claimRepository,
                                ClaimItemRepository claimItemRepository,
                                PatientRepository patientRepository,
                                InvoiceRepository invoiceRepository,
                                BillingService billingService,
                                InsuranceMapper insuranceMapper) {
        this.providerRepository = providerRepository;
        this.policyRepository = policyRepository;
        this.claimRepository = claimRepository;
        this.claimItemRepository = claimItemRepository;
        this.patientRepository = patientRepository;
        this.invoiceRepository = invoiceRepository;
        this.billingService = billingService;
        this.insuranceMapper = insuranceMapper;
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Insurance Providers
    // -----------------------------------------------------------------------------------------------------------------

    @Override
    @Transactional
    public InsuranceProviderResponse createProvider(CreateInsuranceProviderRequest request) {
        log.info("Registering insurance payer with code: {}", request.providerCode());

        if (providerRepository.findByProviderCode(request.providerCode()).isPresent()) {
            throw new BusinessRuleException("PROVIDER_CODE_EXISTS",
                    "Insurance provider with code '" + request.providerCode() + "' already exists.");
        }

        InsuranceProvider provider = new InsuranceProvider(
                UUID.randomUUID().toString(),
                request.providerCode(),
                request.name(),
                request.payerId(),
                request.contactEmail(),
                request.contactPhone(),
                request.address(),
                true
        );

        InsuranceProvider saved = providerRepository.save(provider);
        return insuranceMapper.toProviderResponse(saved);
    }

    @Override
    public InsuranceProviderResponse getProviderById(String id) {
        InsuranceProvider provider = providerRepository.findById(id)
                .orElseThrow(() -> new InsuranceProviderNotFoundException(id));
        return insuranceMapper.toProviderResponse(provider);
    }

    @Override
    public List<InsuranceProviderResponse> getAllActiveProviders() {
        return providerRepository.findByActiveTrue().stream()
                .map(insuranceMapper::toProviderResponse)
                .toList();
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Insurance Policies
    // -----------------------------------------------------------------------------------------------------------------

    @Override
    @Transactional
    public InsurancePolicyResponse createPolicy(CreateInsurancePolicyRequest request) {
        log.info("Registering policy [{}] for patient [{}]", request.policyNumber(), request.patientId());

        if (!patientRepository.existsById(request.patientId())) {
            throw new ResourceNotFoundException("Patient", request.patientId());
        }

        InsuranceProvider provider = providerRepository.findById(request.providerId())
                .orElseThrow(() -> new InsuranceProviderNotFoundException(request.providerId()));

        if (request.coverageEndDate().isBefore(request.coverageStartDate())) {
            throw new BusinessRuleException("INVALID_POLICY_DATES",
                    "Coverage end date cannot be before coverage start date.");
        }

        InsurancePolicy policy = new InsurancePolicy(
                UUID.randomUUID().toString(),
                request.policyNumber(),
                request.groupNumber(),
                request.patientId(),
                provider,
                request.policyHolderName(),
                request.relationship(),
                request.coverageStartDate(),
                request.coverageEndDate(),
                request.coPayAmount(),
                request.coveragePercentage(),
                request.deductible(),
                true
        );

        InsurancePolicy saved = policyRepository.save(policy);
        return insuranceMapper.toPolicyResponse(saved);
    }

    @Override
    public InsurancePolicyResponse getPolicyById(String id) {
        InsurancePolicy policy = policyRepository.findById(id)
                .orElseThrow(() -> new InsurancePolicyNotFoundException(id));
        return insuranceMapper.toPolicyResponse(policy);
    }

    @Override
    public List<InsurancePolicyResponse> getPoliciesByPatient(String patientId) {
        return policyRepository.findByPatientId(patientId).stream()
                .map(insuranceMapper::toPolicyResponse)
                .toList();
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Claims Lifecycle & Adjudication
    // -----------------------------------------------------------------------------------------------------------------

    @Override
    @Transactional
    public InsuranceClaimResponse createClaim(CreateClaimRequest request) {
        log.info("Creating insurance claim for patient [{}] under policy [{}]", request.patientId(), request.policyId());

        if (!patientRepository.existsById(request.patientId())) {
            throw new ResourceNotFoundException("Patient", request.patientId());
        }

        InsurancePolicy policy = policyRepository.findById(request.policyId())
                .orElseThrow(() -> new InsurancePolicyNotFoundException(request.policyId()));

        if (!policy.getPatientId().equals(request.patientId())) {
            throw new BusinessRuleException("POLICY_PATIENT_MISMATCH",
                    "Specified policy does not belong to patient " + request.patientId());
        }

        if (!policy.isCoveredOn(LocalDate.now())) {
            throw new PolicyCoverageExpiredException(
                    "Policy " + policy.getPolicyNumber() + " is expired or not active on date of service.");
        }

        if (request.invoiceId() != null && !invoiceRepository.existsById(request.invoiceId())) {
            throw new ResourceNotFoundException("Invoice", request.invoiceId());
        }

        String claimId = UUID.randomUUID().toString();
        String claimNumber = "CLM-" + LocalDate.now().format(DATE_FORMATTER) + "-"
                + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        InsuranceClaim claim = new InsuranceClaim(
                claimId,
                claimNumber,
                policy,
                request.patientId(),
                request.invoiceId()
        );

        if (request.items() != null) {
            for (CreateClaimItemRequest itemReq : request.items()) {
                ClaimItem item = new ClaimItem(
                        UUID.randomUUID().toString(),
                        claim,
                        itemReq.invoiceItemId(),
                        itemReq.serviceCode(),
                        itemReq.description(),
                        itemReq.claimedAmount()
                );
                claim.addItem(item);
            }
        }

        InsuranceClaim saved = claimRepository.save(claim);
        log.info("Created claim [{}] with initial claimed amount: {}", saved.getClaimNumber(), saved.getTotalClaimedAmount());
        return insuranceMapper.toClaimResponse(saved);
    }

    @Override
    @Transactional
    public InsuranceClaimResponse submitClaim(String claimId) {
        log.info("Submitting insurance claim ID: {}", claimId);
        InsuranceClaim claim = claimRepository.findByIdForUpdate(claimId)
                .orElseThrow(() -> new InsuranceClaimNotFoundException(claimId));

        if (!claim.getPolicy().isCoveredOn(LocalDate.now())) {
            throw new PolicyCoverageExpiredException(
                    "Cannot submit claim: Policy is expired or inactive.");
        }

        claim.submit();
        InsuranceClaim saved = claimRepository.save(claim);
        log.info("Claim [{}] successfully transitioned to SUBMITTED", saved.getClaimNumber());
        return insuranceMapper.toClaimResponse(saved);
    }

    @Override
    @Transactional
    public InsuranceClaimResponse startReview(String claimId) {
        log.info("Starting review on claim ID: {}", claimId);
        InsuranceClaim claim = claimRepository.findByIdForUpdate(claimId)
                .orElseThrow(() -> new InsuranceClaimNotFoundException(claimId));

        claim.startReview();
        InsuranceClaim saved = claimRepository.save(claim);
        return insuranceMapper.toClaimResponse(saved);
    }

    @Override
    @Transactional
    public InsuranceClaimResponse adjudicateClaim(String claimId, AdjudicateClaimRequest request) {
        log.info("Adjudicating claim ID: {} with decision: {}", claimId, request.decision());
        InsuranceClaim claim = claimRepository.findByIdForUpdate(claimId)
                .orElseThrow(() -> new InsuranceClaimNotFoundException(claimId));

        claim.adjudicate(
                request.decision(),
                request.approvedAmount(),
                request.notes(),
                request.denialReason()
        );

        InsuranceClaim saved = claimRepository.save(claim);
        log.info("Claim [{}] adjudicated as [{}]. Approved: {}, Patient Resp: {}",
                saved.getClaimNumber(), saved.getStatus(), saved.getApprovedAmount(), saved.getPatientResponsibility());
        return insuranceMapper.toClaimResponse(saved);
    }

    @Override
    @Transactional
    public InsuranceClaimResponse settleClaim(String claimId) {
        log.info("Settling insurance claim ID: {}", claimId);
        InsuranceClaim claim = claimRepository.findByIdForUpdate(claimId)
                .orElseThrow(() -> new InsuranceClaimNotFoundException(claimId));

        claim.settle();

        // If claim is linked to an invoice and approved amount > 0, credit insurance payout against invoice
        if (claim.getInvoiceId() != null && claim.getApprovedAmount().compareTo(BigDecimal.ZERO) > 0) {
            try {
                billingService.processPayment(new ProcessPaymentRequest(
                        claim.getInvoiceId(),
                        claim.getApprovedAmount(),
                        PaymentMethod.INSURANCE,
                        "CLM-SETTLE-" + claim.getClaimNumber(),
                        "ERA-" + claim.getClaimNumber(),
                        "Insurance reimbursement payout from " +
                                (claim.getPolicy().getProvider() != null ? claim.getPolicy().getProvider().getName() : "Payer")
                ));
                log.info("Applied insurance payout [{}] to invoice [{}]", claim.getApprovedAmount(), claim.getInvoiceId());
            } catch (Exception ex) {
                log.warn("Auto-payment settlement to invoice [{}] skipped or failed: {}", claim.getInvoiceId(), ex.getMessage());
            }
        }

        InsuranceClaim saved = claimRepository.save(claim);
        log.info("Claim [{}] settled successfully at {}", saved.getClaimNumber(), saved.getSettledAt());
        return insuranceMapper.toClaimResponse(saved);
    }

    @Override
    public InsuranceClaimResponse getClaimById(String id) {
        InsuranceClaim claim = claimRepository.findById(id)
                .orElseThrow(() -> new InsuranceClaimNotFoundException(id));
        return insuranceMapper.toClaimResponse(claim);
    }

    @Override
    public InsuranceClaimResponse getClaimByNumber(String claimNumber) {
        InsuranceClaim claim = claimRepository.findByClaimNumber(claimNumber)
                .orElseThrow(() -> new ResourceNotFoundException("InsuranceClaim", claimNumber));
        return insuranceMapper.toClaimResponse(claim);
    }

    @Override
    public Page<ClaimSummaryResponse> getClaimsByPatient(String patientId, Pageable pageable) {
        return claimRepository.findByPatientId(patientId, pageable)
                .map(insuranceMapper::toClaimSummaryResponse);
    }

    @Override
    public Page<ClaimSummaryResponse> getClaimsByStatus(ClaimStatus status, Pageable pageable) {
        return claimRepository.findByStatus(status, pageable)
                .map(insuranceMapper::toClaimSummaryResponse);
    }

    @Override
    public Page<ClaimSummaryResponse> getAllClaims(Pageable pageable) {
        return claimRepository.findAll(pageable)
                .map(insuranceMapper::toClaimSummaryResponse);
    }
}
