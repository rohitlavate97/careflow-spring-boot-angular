package com.careflow.admission.dto;

import com.careflow.admission.domain.WardType;

import java.time.Instant;

/**
 * Hospital ward details response DTO (§28).
 */
public record WardResponse(
        String id,
        String wardCode,
        String name,
        String departmentId,
        WardType wardType,
        String floor,
        int totalBeds,
        boolean active,
        int availableBedsCount,
        int occupiedBedsCount,
        Instant createdAt
) {
}
