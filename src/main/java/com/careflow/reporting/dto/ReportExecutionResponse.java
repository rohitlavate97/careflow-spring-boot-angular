package com.careflow.reporting.dto;

import com.careflow.reporting.domain.ReportType;

import java.time.Instant;

/**
 * DTO representing an audited report execution record (§37, §103 Phase 16).
 */
public record ReportExecutionResponse(
        String id,
        ReportType reportType,
        String requestedBy,
        String parameters,
        Long executionTimeMs,
        String status,
        Instant createdAt
) {
}
