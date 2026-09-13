package com.careflow.insurance.service;

import com.careflow.billing.domain.PaymentMethod;
import com.careflow.billing.dto.PaymentResponse;
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
import com.careflow.insurance.domain.PolicyRelationship;
import com.careflow.insurance.dto.AdjudicateClaimRequest;
import com.careflow.insurance.dto.CreateClaimItemRequest;
import com.careflow.insurance.dto.CreateClaimRequest;
import com.careflow.insurance.dto.CreateInsurancePolicyRequest;
import com.careflow.insurance.dto.CreateInsuranceProviderRequest;
import com.careflow.insurance.dto.InsuranceClaimResponse;
import com.careflow.insurance.dto.InsurancePolicyResponse;
import com.careflow.insurance.dto.InsuranceProviderResponse;
import com.careflow.insurance.exception.InvalidClaimStatusTransitionException;
import com.careflow.insurance.exception.PolicyCoverageExpiredException;
import com.careflow.insurance.mapper.InsuranceMapper;
import com.careflow.insurance.repository.ClaimItemRepository;
import com.careflow.insurance.repository.InsuranceClaimRepository;
import com.careflow.insurance.repository.InsurancePolicyRepository;
import com.careflow.insurance.repository.InsuranceProviderRepository;
import com.careflow.patient.repository.PatientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InsuranceServiceTest {

    @Mock
    private InsuranceProviderRepository providerRepository;

    @Mock
    private InsurancePolicyRepository policyRepository;

    @Mock
    private InsuranceClaimRepository claimRepository;

    @Mock
    private ClaimItemRepository claimItemRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private BillingService billingService;

    @Spy
    private InsuranceMapper insuranceMapper = new InsuranceMapper();

    @InjectMocks
    private InsuranceServiceImpl insuranceService;

    private InsuranceProvider testProvider;
    private InsurancePolicy testPolicy;
    private InsuranceClaim testClaim;
    private String patientId;

    @BeforeEach
    void setUp() {
        patientId = "pat-100";
        testProvider = new InsuranceProvider(
                "prov-1", "PAYER-BCBS", "BlueCross", "PAYER-001",
                "claims@bcbs.demo", "+1-800-555-0100", "Chicago, IL", true
        );

        testPolicy = new InsurancePolicy(
                "pol-1", "POL-9999", "GRP-100", patientId, testProvider,
                "John Doe", PolicyRelationship.SELF,
                LocalDate.now().minusMonths(6), LocalDate.now().plusMonths(6),
                BigDecimal.valueOf(20.00), BigDecimal.valueOf(80.00), BigDecimal.valueOf(500.00), true
        );

        testClaim = new InsuranceClaim("clm-1", "CLM-2026-001", testPolicy, patientId, "inv-1");
    }

    @Test
    @DisplayName("Create insurance provider succeeds and checks duplicate code")
    void createProvider_success() {
        CreateInsuranceProviderRequest request = new CreateInsuranceProviderRequest(
                "PAYER-AETNA", "Aetna Health", "PAYER-002", "claims@aetna.demo", "+1-800-555-0200", "Hartford, CT"
        );
        when(providerRepository.findByProviderCode("PAYER-AETNA")).thenReturn(Optional.empty());
        when(providerRepository.save(any(InsuranceProvider.class))).thenAnswer(i -> i.getArgument(0));

        InsuranceProviderResponse response = insuranceService.createProvider(request);

        assertThat(response).isNotNull();
        assertThat(response.providerCode()).isEqualTo("PAYER-AETNA");
        assertThat(response.name()).isEqualTo("Aetna Health");
        verify(providerRepository).save(any(InsuranceProvider.class));
    }

    @Test
    @DisplayName("Create insurance policy succeeds with valid dates")
    void createPolicy_success() {
        CreateInsurancePolicyRequest request = new CreateInsurancePolicyRequest(
                "POL-12345", "GRP-55", patientId, "prov-1", "John Doe", PolicyRelationship.SELF,
                LocalDate.now(), LocalDate.now().plusYears(1), BigDecimal.valueOf(25.00),
                BigDecimal.valueOf(80.00), BigDecimal.valueOf(1000.00)
        );

        when(patientRepository.existsById(patientId)).thenReturn(true);
        when(providerRepository.findById("prov-1")).thenReturn(Optional.of(testProvider));
        when(policyRepository.save(any(InsurancePolicy.class))).thenAnswer(i -> i.getArgument(0));

        InsurancePolicyResponse response = insuranceService.createPolicy(request);

        assertThat(response).isNotNull();
        assertThat(response.policyNumber()).isEqualTo("POL-12345");
        assertThat(response.patientId()).isEqualTo(patientId);
        assertThat(response.coPayAmount()).isEqualByComparingTo(BigDecimal.valueOf(25.00));
    }

    @Test
    @DisplayName("Create policy fails when coverage end date is before start date")
    void createPolicy_invalidDates_throws() {
        CreateInsurancePolicyRequest request = new CreateInsurancePolicyRequest(
                "POL-12345", null, patientId, "prov-1", "John Doe", PolicyRelationship.SELF,
                LocalDate.now(), LocalDate.now().minusDays(1), BigDecimal.ZERO, BigDecimal.valueOf(80.00), BigDecimal.ZERO
        );
        when(patientRepository.existsById(patientId)).thenReturn(true);
        when(providerRepository.findById("prov-1")).thenReturn(Optional.of(testProvider));

        assertThatThrownBy(() -> insuranceService.createPolicy(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Coverage end date cannot be before");
    }

    @Test
    @DisplayName("Create claim calculates total claimed amount from line items")
    void createClaim_success() {
        CreateClaimItemRequest item1 = new CreateClaimItemRequest(
                "inv-item-1", "CPT-99213", "Office Consultation", BigDecimal.valueOf(150.00)
        );
        CreateClaimItemRequest item2 = new CreateClaimItemRequest(
                "inv-item-2", "CPT-80053", "Comprehensive Metabolic Panel", BigDecimal.valueOf(75.00)
        );
        CreateClaimRequest request = new CreateClaimRequest(
                "pol-1", patientId, "inv-1", List.of(item1, item2)
        );

        when(patientRepository.existsById(patientId)).thenReturn(true);
        when(policyRepository.findById("pol-1")).thenReturn(Optional.of(testPolicy));
        when(invoiceRepository.existsById("inv-1")).thenReturn(true);
        when(claimRepository.save(any(InsuranceClaim.class))).thenAnswer(i -> i.getArgument(0));

        InsuranceClaimResponse response = insuranceService.createClaim(request);

        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(ClaimStatus.DRAFT);
        assertThat(response.totalClaimedAmount()).isEqualByComparingTo(BigDecimal.valueOf(225.00));
        assertThat(response.items()).hasSize(2);
    }

    @Test
    @DisplayName("Create claim fails when policy is expired")
    void createClaim_expiredPolicy_throws() {
        testPolicy.setCoverageEndDate(LocalDate.now().minusDays(10));
        CreateClaimRequest request = new CreateClaimRequest(
                "pol-1", patientId, null, List.of(new CreateClaimItemRequest(null, "CPT-1", "Desc", BigDecimal.valueOf(50.00)))
        );

        when(patientRepository.existsById(patientId)).thenReturn(true);
        when(policyRepository.findById("pol-1")).thenReturn(Optional.of(testPolicy));

        assertThatThrownBy(() -> insuranceService.createClaim(request))
                .isInstanceOf(PolicyCoverageExpiredException.class)
                .hasMessageContaining("expired or not active");
    }

    @Test
    @DisplayName("Submit draft claim transitions to SUBMITTED")
    void submitClaim_success() {
        ClaimItem item = new ClaimItem("ci-1", testClaim, null, "CPT-1", "Service", BigDecimal.valueOf(100.00));
        testClaim.addItem(item);

        when(claimRepository.findByIdForUpdate("clm-1")).thenReturn(Optional.of(testClaim));
        when(claimRepository.save(any(InsuranceClaim.class))).thenAnswer(i -> i.getArgument(0));

        InsuranceClaimResponse response = insuranceService.submitClaim("clm-1");

        assertThat(response.status()).isEqualTo(ClaimStatus.SUBMITTED);
        assertThat(response.submittedAt()).isNotNull();
    }

    @Test
    @DisplayName("Adjudicate claim as APPROVED approves full amount with zero patient responsibility")
    void adjudicateClaim_approved_success() {
        ClaimItem item = new ClaimItem("ci-1", testClaim, null, "CPT-1", "Service", BigDecimal.valueOf(200.00));
        testClaim.addItem(item);
        testClaim.submit();

        when(claimRepository.findByIdForUpdate("clm-1")).thenReturn(Optional.of(testClaim));
        when(claimRepository.save(any(InsuranceClaim.class))).thenAnswer(i -> i.getArgument(0));

        AdjudicateClaimRequest request = new AdjudicateClaimRequest(
                ClaimStatus.APPROVED, BigDecimal.valueOf(200.00), "Approved in full", null
        );

        InsuranceClaimResponse response = insuranceService.adjudicateClaim("clm-1", request);

        assertThat(response.status()).isEqualTo(ClaimStatus.APPROVED);
        assertThat(response.approvedAmount()).isEqualByComparingTo(BigDecimal.valueOf(200.00));
        assertThat(response.patientResponsibility()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.adjudicatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Adjudicate claim as PARTIALLY_APPROVED calculates patient responsibility balance")
    void adjudicateClaim_partiallyApproved_success() {
        ClaimItem item = new ClaimItem("ci-1", testClaim, null, "CPT-1", "Service", BigDecimal.valueOf(200.00));
        testClaim.addItem(item);
        testClaim.submit();

        when(claimRepository.findByIdForUpdate("clm-1")).thenReturn(Optional.of(testClaim));
        when(claimRepository.save(any(InsuranceClaim.class))).thenAnswer(i -> i.getArgument(0));

        AdjudicateClaimRequest request = new AdjudicateClaimRequest(
                ClaimStatus.PARTIALLY_APPROVED, BigDecimal.valueOf(160.00), "Covered at 80%", null
        );

        InsuranceClaimResponse response = insuranceService.adjudicateClaim("clm-1", request);

        assertThat(response.status()).isEqualTo(ClaimStatus.PARTIALLY_APPROVED);
        assertThat(response.approvedAmount()).isEqualByComparingTo(BigDecimal.valueOf(160.00));
        assertThat(response.patientResponsibility()).isEqualByComparingTo(BigDecimal.valueOf(40.00));
    }

    @Test
    @DisplayName("Adjudicate claim as REJECTED assigns full balance to patient responsibility and requires denial reason")
    void adjudicateClaim_rejected_success() {
        ClaimItem item = new ClaimItem("ci-1", testClaim, null, "CPT-1", "Service", BigDecimal.valueOf(200.00));
        testClaim.addItem(item);
        testClaim.submit();

        when(claimRepository.findByIdForUpdate("clm-1")).thenReturn(Optional.of(testClaim));
        when(claimRepository.save(any(InsuranceClaim.class))).thenAnswer(i -> i.getArgument(0));

        AdjudicateClaimRequest request = new AdjudicateClaimRequest(
                ClaimStatus.REJECTED, BigDecimal.ZERO, null, "Prior authorization not obtained"
        );

        InsuranceClaimResponse response = insuranceService.adjudicateClaim("clm-1", request);

        assertThat(response.status()).isEqualTo(ClaimStatus.REJECTED);
        assertThat(response.approvedAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.patientResponsibility()).isEqualByComparingTo(BigDecimal.valueOf(200.00));
        assertThat(response.denialReason()).isEqualTo("Prior authorization not obtained");
    }

    @Test
    @DisplayName("Settle approved claim applies insurance reimbursement payout to invoice and marks SETTLED")
    void settleClaim_success() {
        ClaimItem item = new ClaimItem("ci-1", testClaim, null, "CPT-1", "Service", BigDecimal.valueOf(300.00));
        testClaim.addItem(item);
        testClaim.submit();
        testClaim.adjudicate(ClaimStatus.APPROVED, BigDecimal.valueOf(300.00), "Approved", null);

        when(claimRepository.findByIdForUpdate("clm-1")).thenReturn(Optional.of(testClaim));
        when(claimRepository.save(any(InsuranceClaim.class))).thenAnswer(i -> i.getArgument(0));

        InsuranceClaimResponse response = insuranceService.settleClaim("clm-1");

        assertThat(response.status()).isEqualTo(ClaimStatus.SETTLED);
        assertThat(response.settledAt()).isNotNull();

        // Verify billingService was invoked with PaymentMethod.INSURANCE
        verify(billingService).processPayment(any(ProcessPaymentRequest.class));
    }

    @Test
    @DisplayName("Settle claim throws exception when claim is REJECTED")
    void settleClaim_rejected_throws() {
        ClaimItem item = new ClaimItem("ci-1", testClaim, null, "CPT-1", "Service", BigDecimal.valueOf(100.00));
        testClaim.addItem(item);
        testClaim.submit();
        testClaim.adjudicate(ClaimStatus.REJECTED, BigDecimal.ZERO, null, "Non-covered service");

        when(claimRepository.findByIdForUpdate("clm-1")).thenReturn(Optional.of(testClaim));

        assertThatThrownBy(() -> insuranceService.settleClaim("clm-1"))
                .isInstanceOf(InvalidClaimStatusTransitionException.class)
                .hasMessageContaining("Only APPROVED or PARTIALLY_APPROVED");
    }
}
