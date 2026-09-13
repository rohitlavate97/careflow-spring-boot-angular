package com.careflow.billing.dto;

import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request DTO to issue a finalized draft invoice (§29, §30).
 */
public record IssueInvoiceRequest(
        LocalDate dueDate,

        @DecimalMin(value = "0.00", message = "Discount cannot be negative")
        BigDecimal discountAmount,

        @DecimalMin(value = "0.00", message = "Tax cannot be negative")
        BigDecimal taxAmount,

        String notes
) {}
