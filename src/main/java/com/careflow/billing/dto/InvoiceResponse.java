package com.careflow.billing.dto;

import com.careflow.billing.domain.InvoiceStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Detailed response DTO for an invoice including line items and payments (§29, §30, §31).
 */
public record InvoiceResponse(
        String id,
        String invoiceNumber,
        String patientId,
        String encounterId,
        String admissionId,
        InvoiceStatus status,
        String currency,
        BigDecimal subtotal,
        BigDecimal discountAmount,
        BigDecimal taxAmount,
        BigDecimal totalAmount,
        BigDecimal paidAmount,
        BigDecimal balanceDue,
        String notes,
        LocalDate dueDate,
        Instant issuedAt,
        Instant paidAt,
        Instant createdAt,
        Instant updatedAt,
        List<InvoiceItemResponse> items,
        List<PaymentResponse> payments
) {}
