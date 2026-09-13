package com.careflow.admission.dto;

import com.careflow.admission.domain.BedStatus;

import java.math.BigDecimal;

/**
 * Hospital physical bed response DTO (§28).
 */
public record BedResponse(
        String id,
        String bedNumber,
        String roomId,
        String roomNumber,
        String wardId,
        String wardName,
        BedStatus status,
        BigDecimal dailyRate,
        boolean active
) {
}
