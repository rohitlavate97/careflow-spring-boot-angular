package com.careflow.billing.service;

import com.careflow.billing.domain.InvoiceStatus;
import com.careflow.billing.dto.CreateInvoiceItemRequest;
import com.careflow.billing.dto.CreateInvoiceRequest;
import com.careflow.billing.dto.InvoiceResponse;
import com.careflow.billing.dto.InvoiceSummaryResponse;
import com.careflow.billing.dto.IssueInvoiceRequest;
import com.careflow.billing.dto.PaymentResponse;
import com.careflow.billing.dto.ProcessPaymentRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Service interface for hospital billing, invoice lifecycle management, and idempotent payments (§29, §30, §31, §32, §57 Lab 5).
 */
public interface BillingService {

    InvoiceResponse createInvoice(CreateInvoiceRequest request);

    InvoiceResponse addItemToInvoice(String invoiceId, CreateInvoiceItemRequest request);

    InvoiceResponse removeItemFromInvoice(String invoiceId, String itemId);

    InvoiceResponse issueInvoice(String invoiceId, IssueInvoiceRequest request);

    InvoiceResponse cancelInvoice(String invoiceId, String reason);

    InvoiceResponse getInvoiceById(String id);

    InvoiceResponse getInvoiceByNumber(String invoiceNumber);

    Page<InvoiceSummaryResponse> getInvoicesByPatient(String patientId, Pageable pageable);

    Page<InvoiceSummaryResponse> getInvoicesByStatus(InvoiceStatus status, Pageable pageable);

    Page<InvoiceSummaryResponse> getAllInvoices(Pageable pageable);

    PaymentResponse processPayment(ProcessPaymentRequest request);

    PaymentResponse getPaymentById(String paymentId);

    List<PaymentResponse> getPaymentsByInvoice(String invoiceId);
}
