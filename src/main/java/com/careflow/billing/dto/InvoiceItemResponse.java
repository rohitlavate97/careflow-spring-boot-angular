package com.careflow.billing.dto;

import com.careflow.billing.domain.BillingSource;

import java.math.BigDecimal;

/**
 * Response DTO for an invoice line item (§29).
 */
public record InvoiceItemResponse(
        String id,
        String invoiceId,
        BillingSource billingSource,
        String itemCode,
        String description,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal totalPrice,
        String sourceReferenceId
) {}
