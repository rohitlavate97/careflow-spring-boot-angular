package com.careflow.insurance.repository;

import com.careflow.insurance.domain.ClaimItem;
import com.careflow.insurance.domain.ClaimStatus;
import com.careflow.insurance.domain.InsuranceClaim;
import com.careflow.insurance.domain.InsurancePolicy;
import com.careflow.insurance.domain.InsuranceProvider;
import com.careflow.insurance.domain.PolicyRelationship;
import com.careflow.patient.domain.Gender;
import com.careflow.patient.domain.Patient;
import com.careflow.patient.repository.PatientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class InsuranceClaimRepositoryTest {

    @Autowired
    private InsuranceClaimRepository claimRepository;

    @Autowired
    private InsurancePolicyRepository policyRepository;

    @Autowired
    private InsuranceProviderRepository providerRepository;

    @Autowired
    private PatientRepository patientRepository;

    private Patient patient;
    private InsuranceProvider provider;
    private InsurancePolicy policy;
    private InsuranceClaim claimDraft;
    private InsuranceClaim claimSubmitted;

    @BeforeEach
    void setUp() {
        patient = patientRepository.save(new Patient(
                UUID.randomUUID().toString(), "MRN-INS-" + UUID.randomUUID().toString().substring(0, 4),
                "Ada", "Byron", LocalDate.of(1988, 12, 10), Gender.FEMALE, "+1-555-0951"
        ));

        provider = providerRepository.save(new InsuranceProvider(
                UUID.randomUUID().toString(), "PROV-TEST-" + UUID.randomUUID().toString().substring(0, 4),
                "Test Health Plan", "PAYER-TEST", "claims@test.demo", "+1-800-555-0952", "New York, NY", true
        ));

        policy = policyRepository.save(new InsurancePolicy(
                UUID.randomUUID().toString(), "POL-TEST-001", "GRP-1", patient.getId(), provider,
                "Ada Byron", PolicyRelationship.SELF, LocalDate.now().minusMonths(3), LocalDate.now().plusMonths(9),
                BigDecimal.valueOf(15.00), BigDecimal.valueOf(80.00), BigDecimal.valueOf(250.00), true
        ));

        claimDraft = new InsuranceClaim(
                UUID.randomUUID().toString(), "CLM-TEST-001", policy, patient.getId(), null
        );
        claimDraft.addItem(new ClaimItem(UUID.randomUUID().toString(), claimDraft, null, "CPT-1", "Consultation", BigDecimal.valueOf(100.00)));
        claimDraft = claimRepository.save(claimDraft);

        claimSubmitted = new InsuranceClaim(
                UUID.randomUUID().toString(), "CLM-TEST-002", policy, patient.getId(), null
        );
        claimSubmitted.addItem(new ClaimItem(UUID.randomUUID().toString(), claimSubmitted, null, "CPT-2", "Lab Test", BigDecimal.valueOf(80.00)));
        claimSubmitted.submit();
        claimSubmitted = claimRepository.save(claimSubmitted);
    }

    @Test
    @DisplayName("Find claim by unique claim number")
    void findByClaimNumber_success() {
        Optional<InsuranceClaim> found = claimRepository.findByClaimNumber("CLM-TEST-001");
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(claimDraft.getId());
        assertThat(found.get().getStatus()).isEqualTo(ClaimStatus.DRAFT);
    }

    @Test
    @DisplayName("Retrieve claim with pessimistic write lock (findByIdForUpdate)")
    void findByIdForUpdate_success() {
        Optional<InsuranceClaim> locked = claimRepository.findByIdForUpdate(claimSubmitted.getId());
        assertThat(locked).isPresent();
        assertThat(locked.get().getStatus()).isEqualTo(ClaimStatus.SUBMITTED);
        assertThat(locked.get().getTotalClaimedAmount()).isEqualByComparingTo(BigDecimal.valueOf(80.00));
    }

    @Test
    @DisplayName("Find claims by patient ID with pagination")
    void findByPatientId_success() {
        Page<InsuranceClaim> page = claimRepository.findByPatientId(patient.getId(), PageRequest.of(0, 10));
        assertThat(page.getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("Find claims by status filter")
    void findByStatus_success() {
        Page<InsuranceClaim> submitted = claimRepository.findByStatus(ClaimStatus.SUBMITTED, PageRequest.of(0, 10));
        assertThat(submitted.getContent()).extracting(InsuranceClaim::getClaimNumber).contains("CLM-TEST-002");
    }
}
