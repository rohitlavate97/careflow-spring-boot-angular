package com.careflow.billing.dto;

import com.careflow.billing.domain.BillingSource;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Request DTO to add a clinical service line item to an invoice (§29).
 */
public record CreateInvoiceItemRequest(
        @NotNull(message = "Billing source is required")
        BillingSource billingSource,

        @NotBlank(message = "Item code is required")
        String itemCode,

        @NotBlank(message = "Description is required")
        String description,

        @NotNull(message = "Unit price is required")
        @DecimalMin(value = "0.00", message = "Unit price cannot be negative")
        BigDecimal unitPrice,

        @Min(value = 1, message = "Quantity must be at least 1")
        int quantity,

        String sourceReferenceId
) {}
