package com.careflow.billing.service;

import com.careflow.billing.domain.BillingSource;
import com.careflow.billing.domain.Invoice;
import com.careflow.billing.domain.InvoiceItem;
import com.careflow.billing.domain.InvoiceStatus;
import com.careflow.billing.domain.Payment;
import com.careflow.billing.domain.PaymentMethod;
import com.careflow.billing.domain.PaymentStatus;
import com.careflow.billing.dto.CreateInvoiceItemRequest;
import com.careflow.billing.dto.CreateInvoiceRequest;
import com.careflow.billing.dto.InvoiceResponse;
import com.careflow.billing.dto.IssueInvoiceRequest;
import com.careflow.billing.dto.PaymentResponse;
import com.careflow.billing.dto.ProcessPaymentRequest;
import com.careflow.billing.exception.InvalidInvoiceStatusTransitionException;
import com.careflow.billing.exception.InvoiceNotFoundException;
import com.careflow.billing.exception.OverpaymentException;
import com.careflow.billing.mapper.BillingMapper;
import com.careflow.billing.repository.InvoiceItemRepository;
import com.careflow.billing.repository.InvoiceRepository;
import com.careflow.billing.repository.PaymentRepository;
import com.careflow.common.exception.BusinessRuleException;
import com.careflow.common.exception.ResourceNotFoundException;
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
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BillingServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private InvoiceItemRepository invoiceItemRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PatientRepository patientRepository;

    @Spy
    private BillingMapper billingMapper = new BillingMapper();

    @InjectMocks
    private BillingServiceImpl billingService;

    private String patientId;
    private Invoice testInvoice;

    @BeforeEach
    void setUp() {
        patientId = "pat-123";
        testInvoice = new Invoice(
                "inv-1", "INV-20260913-001", patientId, "enc-1", null, "USD", "Test invoice notes"
        );
    }

    @Test
    @DisplayName("Create invoice with items calculates subtotal, total, and balance due correctly")
    void createInvoice_success() {
        CreateInvoiceItemRequest item1 = new CreateInvoiceItemRequest(
                BillingSource.CONSULTATION, "CONS-GEN", "General Consultation Fee",
                BigDecimal.valueOf(100.00), 1, "cons-001"
        );
        CreateInvoiceItemRequest item2 = new CreateInvoiceItemRequest(
                BillingSource.PHARMACY, "MED-AMOX", "Amoxicillin 500mg",
                BigDecimal.valueOf(25.50), 2, "disp-001"
        );
        CreateInvoiceRequest request = new CreateInvoiceRequest(
                patientId, "enc-1", null, "USD", "Initial invoice", List.of(item1, item2)
        );

        when(patientRepository.existsById(patientId)).thenReturn(true);
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InvoiceResponse response = billingService.createInvoice(request);

        assertThat(response).isNotNull();
        assertThat(response.patientId()).isEqualTo(patientId);
        assertThat(response.status()).isEqualTo(InvoiceStatus.DRAFT);
        assertThat(response.subtotal()).isEqualByComparingTo(BigDecimal.valueOf(151.00));
        assertThat(response.totalAmount()).isEqualByComparingTo(BigDecimal.valueOf(151.00));
        assertThat(response.balanceDue()).isEqualByComparingTo(BigDecimal.valueOf(151.00));
        assertThat(response.items()).hasSize(2);
        verify(invoiceRepository).save(any(Invoice.class));
    }

    @Test
    @DisplayName("Create invoice throws ResourceNotFoundException when patient does not exist")
    void createInvoice_patientNotFound() {
        CreateInvoiceRequest request = new CreateInvoiceRequest(
                "non-existent-pat", null, null, "USD", null, null
        );
        when(patientRepository.existsById("non-existent-pat")).thenReturn(false);

        assertThatThrownBy(() -> billingService.createInvoice(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Patient");
    }

    @Test
    @DisplayName("Add item to draft invoice recalculates subtotal")
    void addItemToInvoice_success() {
        CreateInvoiceItemRequest itemReq = new CreateInvoiceItemRequest(
                BillingSource.LABORATORY, "LAB-CBC", "Complete Blood Count",
                BigDecimal.valueOf(45.00), 1, "lab-001"
        );

        when(invoiceRepository.findById("inv-1")).thenReturn(Optional.of(testInvoice));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InvoiceResponse response = billingService.addItemToInvoice("inv-1", itemReq);

        assertThat(response.items()).hasSize(1);
        assertThat(response.subtotal()).isEqualByComparingTo(BigDecimal.valueOf(45.00));
    }

    @Test
    @DisplayName("Remove item from draft invoice updates subtotal")
    void removeItemFromInvoice_success() {
        InvoiceItem item = new InvoiceItem(
                "item-1", testInvoice, BillingSource.PROCEDURE, "PROC-ECG",
                "Electrocardiogram", BigDecimal.valueOf(80.00), 1, null
        );
        testInvoice.addItem(item);

        when(invoiceRepository.findById("inv-1")).thenReturn(Optional.of(testInvoice));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InvoiceResponse response = billingService.removeItemFromInvoice("inv-1", "item-1");

        assertThat(response.items()).isEmpty();
        assertThat(response.subtotal()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Issue draft invoice transitions to ISSUED and applies discount and tax")
    void issueInvoice_success() {
        InvoiceItem item = new InvoiceItem(
                "item-1", testInvoice, BillingSource.CONSULTATION, "CONS-SPEC",
                "Specialist Consultation", BigDecimal.valueOf(200.00), 1, null
        );
        testInvoice.addItem(item);

        IssueInvoiceRequest issueRequest = new IssueInvoiceRequest(
                LocalDate.now().plusDays(15), BigDecimal.valueOf(20.00), BigDecimal.valueOf(10.00), "Net 15 days"
        );

        when(invoiceRepository.findById("inv-1")).thenReturn(Optional.of(testInvoice));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InvoiceResponse response = billingService.issueInvoice("inv-1", issueRequest);

        assertThat(response.status()).isEqualTo(InvoiceStatus.ISSUED);
        assertThat(response.subtotal()).isEqualByComparingTo(BigDecimal.valueOf(200.00));
        assertThat(response.discountAmount()).isEqualByComparingTo(BigDecimal.valueOf(20.00));
        assertThat(response.taxAmount()).isEqualByComparingTo(BigDecimal.valueOf(10.00));
        // Total = 200 - 20 + 10 = 190.00
        assertThat(response.totalAmount()).isEqualByComparingTo(BigDecimal.valueOf(190.00));
        assertThat(response.balanceDue()).isEqualByComparingTo(BigDecimal.valueOf(190.00));
    }

    @Test
    @DisplayName("Issue invoice fails when there are no line items")
    void issueInvoice_emptyItems_throws() {
        IssueInvoiceRequest issueRequest = new IssueInvoiceRequest(
                LocalDate.now().plusDays(30), BigDecimal.ZERO, BigDecimal.ZERO, null
        );
        when(invoiceRepository.findById("inv-1")).thenReturn(Optional.of(testInvoice));

        assertThatThrownBy(() -> billingService.issueInvoice("inv-1", issueRequest))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("no line items");
    }

    @Test
    @DisplayName("Cancel invoice marks status as CANCELLED")
    void cancelInvoice_success() {
        when(invoiceRepository.findById("inv-1")).thenReturn(Optional.of(testInvoice));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InvoiceResponse response = billingService.cancelInvoice("inv-1", "Billing error corrected");

        assertThat(response.status()).isEqualTo(InvoiceStatus.CANCELLED);
        assertThat(response.notes()).contains("Cancelled: Billing error corrected");
    }

    @Test
    @DisplayName("Cancel invoice throws exception when invoice already paid")
    void cancelInvoice_alreadyPaid_throws() {
        testInvoice.setStatus(InvoiceStatus.PAID);
        when(invoiceRepository.findById("inv-1")).thenReturn(Optional.of(testInvoice));

        assertThatThrownBy(() -> billingService.cancelInvoice("inv-1", "Want to cancel"))
                .isInstanceOf(InvalidInvoiceStatusTransitionException.class)
                .hasMessageContaining("recorded payments");
    }

    @Test
    @DisplayName("Process payment with existing idempotency key returns cached payment without re-processing")
    void processPayment_idempotentReplay() {
        String idempotencyKey = "PAY-IDEMP-KEY-111";
        Payment existingPayment = new Payment(
                "pay-1", "PAY-2026-001", testInvoice, idempotencyKey,
                PaymentMethod.CREDIT_CARD, BigDecimal.valueOf(100.00), PaymentStatus.SUCCESS,
                "TXN-EXISTING", "Already paid", Instant.now()
        );

        when(paymentRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.of(existingPayment));

        ProcessPaymentRequest request = new ProcessPaymentRequest(
                "inv-1", BigDecimal.valueOf(100.00), PaymentMethod.CREDIT_CARD,
                idempotencyKey, "TXN-REPLAY", null
        );

        PaymentResponse response = billingService.processPayment(request);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo("pay-1");
        assertThat(response.idempotencyKey()).isEqualTo(idempotencyKey);
        // Verify invoice was never locked or modified
        verify(invoiceRepository, never()).findByIdForUpdate(any());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Process partial payment transitions invoice to PARTIALLY_PAID")
    void processPayment_partialPayment() {
        InvoiceItem item = new InvoiceItem(
                "item-1", testInvoice, BillingSource.ADMISSION, "WARD-ICU",
                "ICU Daily Rate", BigDecimal.valueOf(300.00), 1, null
        );
        testInvoice.addItem(item);
        testInvoice.issue(LocalDate.now().plusDays(7), BigDecimal.ZERO, BigDecimal.ZERO);

        when(paymentRepository.findByIdempotencyKey("KEY-PARTIAL-1")).thenReturn(Optional.empty());
        when(invoiceRepository.findByIdForUpdate("inv-1")).thenReturn(Optional.of(testInvoice));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProcessPaymentRequest request = new ProcessPaymentRequest(
                "inv-1", BigDecimal.valueOf(100.00), PaymentMethod.CASH,
                "KEY-PARTIAL-1", "CASH-REC-1", "Deposit payment"
        );

        PaymentResponse response = billingService.processPayment(request);

        assertThat(response.amount()).isEqualByComparingTo(BigDecimal.valueOf(100.00));
        assertThat(testInvoice.getStatus()).isEqualTo(InvoiceStatus.PARTIALLY_PAID);
        assertThat(testInvoice.getPaidAmount()).isEqualByComparingTo(BigDecimal.valueOf(100.00));
        assertThat(testInvoice.getBalanceDue()).isEqualByComparingTo(BigDecimal.valueOf(200.00));
        verify(paymentRepository).save(any(Payment.class));
        verify(invoiceRepository).save(testInvoice);
    }

    @Test
    @DisplayName("Process full payment settles invoice balance to 0 and transitions to PAID")
    void processPayment_fullPayment() {
        InvoiceItem item = new InvoiceItem(
                "item-1", testInvoice, BillingSource.CONSULTATION, "CONS-FEE",
                "Consultation", BigDecimal.valueOf(150.00), 1, null
        );
        testInvoice.addItem(item);
        testInvoice.issue(LocalDate.now().plusDays(30), BigDecimal.ZERO, BigDecimal.ZERO);

        when(paymentRepository.findByIdempotencyKey("KEY-FULL-1")).thenReturn(Optional.empty());
        when(invoiceRepository.findByIdForUpdate("inv-1")).thenReturn(Optional.of(testInvoice));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProcessPaymentRequest request = new ProcessPaymentRequest(
                "inv-1", BigDecimal.valueOf(150.00), PaymentMethod.DEBIT_CARD,
                "KEY-FULL-1", "POS-AUTH-999", "Full settlement"
        );

        PaymentResponse response = billingService.processPayment(request);

        assertThat(response.amount()).isEqualByComparingTo(BigDecimal.valueOf(150.00));
        assertThat(testInvoice.getStatus()).isEqualTo(InvoiceStatus.PAID);
        assertThat(testInvoice.getBalanceDue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(testInvoice.getPaidAt()).isNotNull();
    }

    @Test
    @DisplayName("Process payment exceeding balance throws OverpaymentException")
    void processPayment_overpayment_throws() {
        InvoiceItem item = new InvoiceItem(
                "item-1", testInvoice, BillingSource.LABORATORY, "LAB-PANEL",
                "Metabolic Panel", BigDecimal.valueOf(80.00), 1, null
        );
        testInvoice.addItem(item);
        testInvoice.issue(LocalDate.now().plusDays(30), BigDecimal.ZERO, BigDecimal.ZERO);

        when(paymentRepository.findByIdempotencyKey("KEY-OVERPAY")).thenReturn(Optional.empty());
        when(invoiceRepository.findByIdForUpdate("inv-1")).thenReturn(Optional.of(testInvoice));

        ProcessPaymentRequest request = new ProcessPaymentRequest(
                "inv-1", BigDecimal.valueOf(100.00), PaymentMethod.CREDIT_CARD,
                "KEY-OVERPAY", null, null
        );

        assertThatThrownBy(() -> billingService.processPayment(request))
                .isInstanceOf(OverpaymentException.class)
                .hasMessageContaining("exceeds remaining balance due");
    }

    @Test
    @DisplayName("Process payment on unissued DRAFT invoice throws InvalidInvoiceStatusTransitionException")
    void processPayment_draftInvoice_throws() {
        when(paymentRepository.findByIdempotencyKey("KEY-DRAFT")).thenReturn(Optional.empty());
        when(invoiceRepository.findByIdForUpdate("inv-1")).thenReturn(Optional.of(testInvoice));

        ProcessPaymentRequest request = new ProcessPaymentRequest(
                "inv-1", BigDecimal.valueOf(50.00), PaymentMethod.CASH,
                "KEY-DRAFT", null, null
        );

        assertThatThrownBy(() -> billingService.processPayment(request))
                .isInstanceOf(InvalidInvoiceStatusTransitionException.class)
                .hasMessageContaining("ISSUED or PARTIALLY_PAID");
    }
}
