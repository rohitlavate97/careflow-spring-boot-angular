package com.careflow.billing.service;

import com.careflow.billing.domain.BillingSource;
import com.careflow.billing.domain.Invoice;
import com.careflow.billing.domain.InvoiceItem;
import com.careflow.billing.domain.InvoiceStatus;
import com.careflow.billing.domain.Payment;
import com.careflow.billing.domain.PaymentMethod;
import com.careflow.billing.dto.PaymentResponse;
import com.careflow.billing.dto.ProcessPaymentRequest;
import com.careflow.billing.exception.OverpaymentException;
import com.careflow.billing.repository.InvoiceRepository;
import com.careflow.billing.repository.PaymentRepository;
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
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Concurrency Lab 5: Multi-threaded test verifying duplicate payment prevention
 * via database unique constraints and pessimistic locking (§31, §32, §57 Lab 5, §92).
 */
@SpringBootTest
@ActiveProfiles("test")
class PaymentIdempotencyConcurrencyTest {

    @Autowired
    private BillingService billingService;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PatientRepository patientRepository;

    private Patient patient;
    private Invoice invoice;

    @BeforeEach
    void setUp() {
        patient = patientRepository.save(new Patient(
                UUID.randomUUID().toString(), "MRN-PAY-CONC-" + UUID.randomUUID().toString().substring(0, 4),
                "Grace", "Hopper", LocalDate.of(1982, 12, 9), Gender.FEMALE, "+1-555-0903"
        ));

        invoice = new Invoice(
                UUID.randomUUID().toString(), "INV-PAY-CONC-" + UUID.randomUUID().toString().substring(0, 4),
                patient.getId(), null, null, "USD", "Concurrency lab invoice"
        );
        InvoiceItem item = new InvoiceItem(
                UUID.randomUUID().toString(), invoice, BillingSource.CONSULTATION,
                "SURG-FEE", "Surgical Procedure", BigDecimal.valueOf(150.00), 1, null
        );
        invoice.addItem(item);
        invoice.issue(LocalDate.now().plusDays(30), BigDecimal.ZERO, BigDecimal.ZERO);
        invoice = invoiceRepository.save(invoice);
    }

    @Test
    @DisplayName("Concurrency Lab 5: 5 threads submit identical Idempotency-Key -> Exactly 1 payment created, no duplicate charges")
    void concurrentDuplicatePayment_idempotencyPrevented() throws InterruptedException {
        int threadCount = 5;
        String sharedIdempotencyKey = "IDEMP-KEY-STRESS-" + UUID.randomUUID();
        BigDecimal paymentAmount = BigDecimal.valueOf(150.00);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        Set<String> returnedPaymentIds = Collections.newSetFromMap(new ConcurrentHashMap<>());
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    ProcessPaymentRequest request = new ProcessPaymentRequest(
                            invoice.getId(),
                            paymentAmount,
                            PaymentMethod.CREDIT_CARD,
                            sharedIdempotencyKey,
                            "AUTH-" + UUID.randomUUID(),
                            "Multi-threaded test payment"
                    );
                    PaymentResponse response = billingService.processPayment(request);
                    returnedPaymentIds.add(response.id());
                    successCount.incrementAndGet();
                } catch (Exception ex) {
                    failureCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown(); // Release all 5 threads simultaneously
        doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        // 1. All 5 requests succeed (either as the initial processor or an idempotent replay)
        assertThat(successCount.get()).isEqualTo(threadCount);
        assertThat(failureCount.get()).isEqualTo(0);

        // 2. All 5 threads received the EXACT same payment transaction ID
        assertThat(returnedPaymentIds).hasSize(1);

        // 3. Database verification: Exactly 1 payment row exists for this invoice
        List<Payment> paymentsInDb = paymentRepository.findByInvoiceId(invoice.getId());
        assertThat(paymentsInDb).hasSize(1);
        Payment singlePayment = paymentsInDb.get(0);
        assertThat(singlePayment.getIdempotencyKey()).isEqualTo(sharedIdempotencyKey);
        assertThat(singlePayment.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(150.00));

        // 4. Invariant: Invoice paid amount is exactly $150.00, NOT $750.00!
        Invoice reloadedInvoice = invoiceRepository.findById(invoice.getId()).orElseThrow();
        assertThat(reloadedInvoice.getPaidAmount()).isEqualByComparingTo(BigDecimal.valueOf(150.00));
        assertThat(reloadedInvoice.getBalanceDue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(reloadedInvoice.getStatus()).isEqualTo(InvoiceStatus.PAID);
    }

    @Test
    @DisplayName("Concurrency Lab 5: 2 concurrent distinct threads competing for remaining balance -> Overpayment prevented")
    void concurrentDistinctPayments_overpaymentPrevented() throws InterruptedException {
        // Balance due is $150.00.
        // Two threads each attempt to pay $100.00 (total = $200.00 > $150.00).
        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger overpaymentCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final int threadIdx = i;
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    ProcessPaymentRequest request = new ProcessPaymentRequest(
                            invoice.getId(),
                            BigDecimal.valueOf(100.00),
                            PaymentMethod.DEBIT_CARD,
                            "DISTINCT-KEY-" + threadIdx + "-" + UUID.randomUUID(),
                            "AUTH-DISTINCT",
                            "Competing partial payment"
                    );
                    billingService.processPayment(request);
                    successCount.incrementAndGet();
                } catch (OverpaymentException ex) {
                    overpaymentCount.incrementAndGet();
                } catch (Exception ex) {
                    // unexpected
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown();
        doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        // One thread succeeds (paid $100, remaining $50). The second thread is rejected with OverpaymentException.
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(overpaymentCount.get()).isEqualTo(1);

        // Invariant: Invoice paid amount is strictly $100.00, balance due is $50.00
        Invoice reloadedInvoice = invoiceRepository.findById(invoice.getId()).orElseThrow();
        assertThat(reloadedInvoice.getPaidAmount()).isEqualByComparingTo(BigDecimal.valueOf(100.00));
        assertThat(reloadedInvoice.getBalanceDue()).isEqualByComparingTo(BigDecimal.valueOf(50.00));
        assertThat(reloadedInvoice.getStatus()).isEqualTo(InvoiceStatus.PARTIALLY_PAID);
    }
}
