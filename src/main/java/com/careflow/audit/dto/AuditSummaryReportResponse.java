package com.careflow.audit.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Compliance and security audit summary report payload (§36, §103 Phase 15).
 */
public record AuditSummaryReportResponse(
        Instant fromTimestamp,
        Instant toTimestamp,
        long totalEvents,
        Map<String, Long> eventsByAction,
        Map<String, Long> eventsByResourceType,
        Map<String, Long> eventsByStatus,
        List<ActorActivitySummary> topActors,
        List<AuditLogResponse> recentAccessViolations
) {
}
