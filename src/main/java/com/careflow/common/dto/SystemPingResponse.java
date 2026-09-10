package com.careflow.common.dto;

import java.time.Instant;

/**
 * Diagnostic ping response payload.
 * Implemented as an immutable Java 21 record.
 */
public record SystemPingResponse(
        String status,
        String service,
        Instant timestamp,
        String correlationId
) {}
