package com.careflow.billing.dto;

import com.careflow.billing.domain.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Request DTO to process a payment with idempotency key support (§31, §32, §57 Lab 5).
 */
public record ProcessPaymentRequest(
        @NotBlank(message = "Invoice ID is required")
        String invoiceId,

        @NotNull(message = "Payment amount is required")
        @DecimalMin(value = "0.01", message = "Payment amount must be greater than zero")
        BigDecimal amount,

        @NotNull(message = "Payment method is required")
        PaymentMethod paymentMethod,

        String idempotencyKey,

        String transactionReference,

        String notes
) {}
