package com.careflow.admission.dto;

import java.time.Instant;

/**
 * Bed transfer audit entry response DTO (§28).
 */
public record BedTransferResponse(
        String id,
        String admissionId,
        String fromBedId,
        String fromBedNumber,
        String toBedId,
        String toBedNumber,
        Instant transferredAt,
        String transferReason,
        String transferredById
) {
}
