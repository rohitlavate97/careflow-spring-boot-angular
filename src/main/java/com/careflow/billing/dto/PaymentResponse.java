package com.careflow.billing.dto;

import com.careflow.billing.domain.PaymentMethod;
import com.careflow.billing.domain.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Response DTO for a payment transaction (§31, §32).
 */
public record PaymentResponse(
        String id,
        String paymentNumber,
        String invoiceId,
        String idempotencyKey,
        PaymentMethod paymentMethod,
        BigDecimal amount,
        PaymentStatus status,
        String transactionReference,
        String notes,
        Instant processedAt
) {}
