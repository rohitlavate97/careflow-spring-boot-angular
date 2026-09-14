package com.careflow.audit.dto;

import com.careflow.audit.domain.AuditAction;
import com.careflow.audit.domain.AuditResourceType;
import com.careflow.audit.domain.AuditStatus;

import java.time.Instant;

/**
 * Standard immutable audit log response record (§36, §74, §103 Phase 15).
 */
public record AuditLogResponse(
        String id,
        String actorUserId,
        AuditAction action,
        AuditResourceType resourceType,
        String resourceId,
        String patientId,
        Instant timestamp,
        String previousValue,
        String newValue,
        String ipAddress,
        String correlationId,
        AuditStatus status,
        String details
) {
}
