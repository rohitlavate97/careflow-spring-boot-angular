package com.careflow.insurance.service;

import com.careflow.insurance.domain.ClaimItem;
import com.careflow.insurance.domain.ClaimStatus;
import com.careflow.insurance.domain.InsuranceClaim;
import com.careflow.insurance.domain.InsurancePolicy;
import com.careflow.insurance.domain.InsuranceProvider;
import com.careflow.insurance.domain.PolicyRelationship;
import com.careflow.insurance.exception.InvalidClaimStatusTransitionException;
import com.careflow.insurance.repository.InsuranceClaimRepository;
import com.careflow.insurance.repository.InsurancePolicyRepository;
import com.careflow.insurance.repository.InsuranceProviderRepository;
import com.careflow.patient.domain.Gender;
import com.careflow.patient.domain.Patient;
import com.careflow.patient.repository.PatientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Multi-threaded concurrency test verifying claims settlement serialization
 * and race condition prevention using pessimistic locking (§33, §92).
 */
@SpringBootTest
@ActiveProfiles("test")
class InsuranceClaimConcurrencyTest {

    @Autowired
    private InsuranceService insuranceService;

    @Autowired
    private InsuranceClaimRepository claimRepository;

    @Autowired
    private InsurancePolicyRepository policyRepository;

    @Autowired
    private InsuranceProviderRepository providerRepository;

    @Autowired
    private PatientRepository patientRepository;

    private InsuranceClaim claim;

    @BeforeEach
    void setUp() {
        Patient patient = patientRepository.save(new Patient(
                UUID.randomUUID().toString(), "MRN-CLM-CONC-" + UUID.randomUUID().toString().substring(0, 4),
                "Hedy", "Lamarr", LocalDate.of(1992, 4, 15), Gender.FEMALE, "+1-555-0988"
        ));

        InsuranceProvider provider = providerRepository.save(new InsuranceProvider(
                UUID.randomUUID().toString(), "PROV-CONC-" + UUID.randomUUID().toString().substring(0, 4),
                "Humana Health", "PAYER-HUMANA", "claims@humana.demo", "+1-800-555-0989", "Louisville, KY", true
        ));

        InsurancePolicy policy = policyRepository.save(new InsurancePolicy(
                UUID.randomUUID().toString(), "POL-CONC-111", "GRP-CONC", patient.getId(), provider,
                "Hedy Lamarr", PolicyRelationship.SELF, LocalDate.now().minusMonths(1), LocalDate.now().plusMonths(11),
                BigDecimal.valueOf(25.00), BigDecimal.valueOf(80.00), BigDecimal.valueOf(500.00), true
        ));

        claim = new InsuranceClaim(
                UUID.randomUUID().toString(), "CLM-CONC-" + UUID.randomUUID().toString().substring(0, 4),
                policy, patient.getId(), null
        );
        claim.addItem(new ClaimItem(UUID.randomUUID().toString(), claim, null, "CPT-CONC", "Procedure", BigDecimal.valueOf(500.00)));
        claim.submit();
        claim.adjudicate(ClaimStatus.APPROVED, BigDecimal.valueOf(500.00), "Approved for settlement stress test", null);
        claim = claimRepository.save(claim);
    }

    @Test
    @DisplayName("5 concurrent threads attempt to settle the same approved claim -> Exactly 1 succeeds, 4 safely rejected")
    void concurrentClaimSettlement_serializedSafely() throws InterruptedException {
        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger rejectedCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    insuranceService.settleClaim(claim.getId());
                    successCount.incrementAndGet();
                } catch (InvalidClaimStatusTransitionException ex) {
                    rejectedCount.incrementAndGet();
                } catch (Exception ex) {
                    // unexpected
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown(); // Fire all 5 threads simultaneously
        doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        // 1. Exactly 1 thread successfully settles the claim
        assertThat(successCount.get()).isEqualTo(1);

        // 2. Exactly 4 competing threads are safely rejected because claim is already SETTLED
        assertThat(rejectedCount.get()).isEqualTo(4);

        // 3. Database state invariant: Status is SETTLED and settledAt is non-null
        InsuranceClaim reloaded = claimRepository.findById(claim.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(ClaimStatus.SETTLED);
        assertThat(reloaded.getSettledAt()).isNotNull();
    }
}
