package com.careflow.audit.dto;

import com.careflow.audit.domain.AuditAction;
import com.careflow.audit.domain.AuditResourceType;
import com.careflow.audit.domain.AuditStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Payload for recording an audit event (§36, §74).
 * If actorUserId, ipAddress, or correlationId are omitted, they will be automatically
 * extracted from the active security and request context.
 */
public record RecordAuditEventRequest(
        String actorUserId,

        @NotNull(message = "Audit action is required")
        AuditAction action,

        @NotNull(message = "Resource type is required")
        AuditResourceType resourceType,

        @Size(max = 100, message = "Resource ID must not exceed 100 characters")
        String resourceId,

        @Size(max = 64, message = "Patient ID must not exceed 64 characters")
        String patientId,

        String previousValue,

        String newValue,

        @Size(max = 45, message = "IP address must not exceed 45 characters")
        String ipAddress,

        @Size(max = 64, message = "Correlation ID must not exceed 64 characters")
        String correlationId,

        AuditStatus status,

        @Size(max = 1000, message = "Details must not exceed 1000 characters")
        String details
) {
    public RecordAuditEventRequest {
        if (status == null) {
            status = AuditStatus.SUCCESS;
        }
    }
}
