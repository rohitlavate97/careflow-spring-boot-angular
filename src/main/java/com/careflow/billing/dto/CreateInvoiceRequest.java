package com.careflow.billing.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

/**
 * Request DTO to create a draft invoice aggregate (§29).
 */
public record CreateInvoiceRequest(
        @NotBlank(message = "Patient ID is required")
        String patientId,

        String encounterId,

        String admissionId,

        String currency,

        String notes,

        List<@Valid CreateInvoiceItemRequest> items
) {}
