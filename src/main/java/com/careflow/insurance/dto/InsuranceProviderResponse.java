package com.careflow.insurance.dto;

import java.time.Instant;

/**
 * Response DTO for an insurance payer entity (§33).
 */
public record InsuranceProviderResponse(
        String id,
        String providerCode,
        String name,
        String payerId,
        String contactEmail,
        String contactPhone,
        String address,
        boolean active,
        Instant createdAt
) {}
