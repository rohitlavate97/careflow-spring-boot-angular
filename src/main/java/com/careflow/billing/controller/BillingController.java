package com.careflow.billing.controller;

import com.careflow.billing.domain.InvoiceStatus;
import com.careflow.billing.dto.CreateInvoiceItemRequest;
import com.careflow.billing.dto.CreateInvoiceRequest;
import com.careflow.billing.dto.InvoiceResponse;
import com.careflow.billing.dto.InvoiceSummaryResponse;
import com.careflow.billing.dto.IssueInvoiceRequest;
import com.careflow.billing.dto.PaymentResponse;
import com.careflow.billing.dto.ProcessPaymentRequest;
import com.careflow.billing.service.BillingService;
import com.careflow.common.dto.PageResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * REST controller for hospital billing, invoice aggregation, and idempotent payments (§29, §30, §31, §32, §57 Lab 5).
 */
@RestController
@RequestMapping("/api/v1/billing")
@Validated
public class BillingController {

    private final BillingService billingService;

    public BillingController(BillingService billingService) {
        this.billingService = billingService;
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Invoices Aggregate Endpoints
    // -----------------------------------------------------------------------------------------------------------------

    @PostMapping("/invoices")
    @PreAuthorize("hasAnyRole('BILLING_OFFICER', 'ADMIN')")
    public ResponseEntity<InvoiceResponse> createInvoice(@Valid @RequestBody CreateInvoiceRequest request) {
        InvoiceResponse response = billingService.createInvoice(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PostMapping("/invoices/{id}/items")
    @PreAuthorize("hasAnyRole('BILLING_OFFICER', 'ADMIN')")
    public ResponseEntity<InvoiceResponse> addItemToInvoice(
            @PathVariable String id,
            @Valid @RequestBody CreateInvoiceItemRequest request) {
        return ResponseEntity.ok(billingService.addItemToInvoice(id, request));
    }

    @DeleteMapping("/invoices/{id}/items/{itemId}")
    @PreAuthorize("hasAnyRole('BILLING_OFFICER', 'ADMIN')")
    public ResponseEntity<InvoiceResponse> removeItemFromInvoice(
            @PathVariable String id,
            @PathVariable String itemId) {
        return ResponseEntity.ok(billingService.removeItemFromInvoice(id, itemId));
    }

    @PostMapping("/invoices/{id}/issue")
    @PreAuthorize("hasAnyRole('BILLING_OFFICER', 'ADMIN')")
    public ResponseEntity<InvoiceResponse> issueInvoice(
            @PathVariable String id,
            @Valid @RequestBody(required = false) IssueInvoiceRequest request) {
        return ResponseEntity.ok(billingService.issueInvoice(id, request));
    }

    @PostMapping("/invoices/{id}/cancel")
    @PreAuthorize("hasAnyRole('BILLING_OFFICER', 'ADMIN')")
    public ResponseEntity<InvoiceResponse> cancelInvoice(
            @PathVariable String id,
            @RequestParam(required = false, defaultValue = "Cancelled by billing officer") String reason) {
        return ResponseEntity.ok(billingService.cancelInvoice(id, reason));
    }

    @GetMapping("/invoices/{id}")
    @PreAuthorize("hasAnyRole('BILLING_OFFICER', 'ADMIN', 'PATIENT', 'DOCTOR')")
    public ResponseEntity<InvoiceResponse> getInvoiceById(@PathVariable String id) {
        return ResponseEntity.ok(billingService.getInvoiceById(id));
    }

    @GetMapping("/invoices/number/{invoiceNumber}")
    @PreAuthorize("hasAnyRole('BILLING_OFFICER', 'ADMIN')")
    public ResponseEntity<InvoiceResponse> getInvoiceByNumber(@PathVariable String invoiceNumber) {
        return ResponseEntity.ok(billingService.getInvoiceByNumber(invoiceNumber));
    }

    @GetMapping("/invoices/patient/{patientId}")
    @PreAuthorize("hasAnyRole('BILLING_OFFICER', 'ADMIN', 'PATIENT', 'DOCTOR')")
    public ResponseEntity<PageResponse<InvoiceSummaryResponse>> getInvoicesByPatient(
            @PathVariable String patientId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(billingService.getInvoicesByPatient(patientId, pageable)));
    }

    @GetMapping("/invoices")
    @PreAuthorize("hasAnyRole('BILLING_OFFICER', 'ADMIN')")
    public ResponseEntity<PageResponse<InvoiceSummaryResponse>> getInvoices(
            @RequestParam(required = false) InvoiceStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        if (status != null) {
            return ResponseEntity.ok(PageResponse.from(billingService.getInvoicesByStatus(status, pageable)));
        }
        return ResponseEntity.ok(PageResponse.from(billingService.getAllInvoices(pageable)));
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Payments Endpoints (Idempotent Settlement)
    // -----------------------------------------------------------------------------------------------------------------

    @PostMapping("/payments")
    @PreAuthorize("hasAnyRole('BILLING_OFFICER', 'ADMIN', 'PATIENT')")
    public ResponseEntity<PaymentResponse> processPayment(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKeyHeader,
            @Valid @RequestBody ProcessPaymentRequest request) {

        String effectiveKey = (idempotencyKeyHeader != null && !idempotencyKeyHeader.isBlank())
                ? idempotencyKeyHeader.trim()
                : request.idempotencyKey();

        ProcessPaymentRequest effectiveRequest = new ProcessPaymentRequest(
                request.invoiceId(),
                request.amount(),
                request.paymentMethod(),
                effectiveKey,
                request.transactionReference(),
                request.notes()
        );

        PaymentResponse response = billingService.processPayment(effectiveRequest);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/payments/{id}")
    @PreAuthorize("hasAnyRole('BILLING_OFFICER', 'ADMIN', 'PATIENT')")
    public ResponseEntity<PaymentResponse> getPaymentById(@PathVariable String id) {
        return ResponseEntity.ok(billingService.getPaymentById(id));
    }

    @GetMapping("/payments/invoice/{invoiceId}")
    @PreAuthorize("hasAnyRole('BILLING_OFFICER', 'ADMIN', 'PATIENT')")
    public ResponseEntity<List<PaymentResponse>> getPaymentsByInvoice(@PathVariable String invoiceId) {
        return ResponseEntity.ok(billingService.getPaymentsByInvoice(invoiceId));
    }
}
