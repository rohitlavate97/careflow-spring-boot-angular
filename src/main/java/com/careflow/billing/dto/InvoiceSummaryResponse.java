package com.careflow.billing.dto;

import com.careflow.billing.domain.InvoiceStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Lightweight summary response DTO for invoice listing and dashboards (§29).
 */
public record InvoiceSummaryResponse(
        String id,
        String invoiceNumber,
        String patientId,
        InvoiceStatus status,
        String currency,
        BigDecimal subtotal,
        BigDecimal discountAmount,
        BigDecimal taxAmount,
        BigDecimal totalAmount,
        BigDecimal paidAmount,
        BigDecimal balanceDue,
        LocalDate dueDate,
        Instant issuedAt,
        Instant createdAt
) {}
