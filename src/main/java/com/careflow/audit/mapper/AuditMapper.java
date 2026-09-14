package com.careflow.audit.mapper;

import com.careflow.audit.domain.AuditLog;
import com.careflow.audit.dto.AuditLogResponse;
import com.careflow.audit.dto.RecordAuditEventRequest;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting Audit domain entities into API DTOs (§89).
 */
@Component
public class AuditMapper {

    public AuditLogResponse toResponse(AuditLog auditLog) {
        if (auditLog == null) {
            return null;
        }

        return new AuditLogResponse(
                auditLog.getId(),
                auditLog.getActorUserId(),
                auditLog.getAction(),
                auditLog.getResourceType(),
                auditLog.getResourceId(),
                auditLog.getPatientId(),
                auditLog.getTimestamp(),
                auditLog.getPreviousValue(),
                auditLog.getNewValue(),
                auditLog.getIpAddress(),
                auditLog.getCorrelationId(),
                auditLog.getStatus(),
                auditLog.getDetails()
        );
    }

    public AuditLog toEntity(RecordAuditEventRequest request,
                             String resolvedActorUserId,
                             String resolvedIpAddress,
                             String resolvedCorrelationId) {
        if (request == null) {
            return null;
        }

        return AuditLog.create(
                resolvedActorUserId,
                request.action(),
                request.resourceType(),
                request.resourceId(),
                request.patientId(),
                request.previousValue(),
                request.newValue(),
                resolvedIpAddress,
                resolvedCorrelationId,
                request.status(),
                request.details()
        );
    }
}
