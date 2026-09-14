package com.careflow.audit.dto;

import com.careflow.audit.domain.AuditAction;
import com.careflow.audit.domain.AuditResourceType;
import com.careflow.audit.domain.AuditStatus;

import java.time.Instant;

/**
 * Filter criteria for paginated audit log queries (§36, §103 Phase 15).
 */
public record AuditSearchCriteria(
        String actorUserId,
        AuditAction action,
        AuditResourceType resourceType,
        String resourceId,
        String patientId,
        Instant fromTimestamp,
        Instant toTimestamp,
        String correlationId,
        AuditStatus status
) {
}
