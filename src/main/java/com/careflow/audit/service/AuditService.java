package com.careflow.audit.service;

import com.careflow.audit.domain.AuditAction;
import com.careflow.audit.domain.AuditResourceType;
import com.careflow.audit.domain.AuditStatus;
import com.careflow.audit.dto.AuditLogResponse;
import com.careflow.audit.dto.AuditSearchCriteria;
import com.careflow.audit.dto.AuditSummaryReportResponse;
import com.careflow.audit.dto.RecordAuditEventRequest;
import com.careflow.common.dto.PageResponse;
import org.springframework.data.domain.Pageable;

import java.time.Instant;

/**
 * Service interface for recording, searching, and generating compliance reports from immutable audit logs (§36, §74, §103 Phase 15).
 */
public interface AuditService {

    /**
     * Records an audit event using the provided request payload.
     * Automatically enriches missing actor, IP, or correlation metadata from the ambient context.
     * Runs with REQUIRES_NEW propagation to ensure persistence independently of outer transaction outcomes.
     */
    AuditLogResponse recordEvent(RecordAuditEventRequest request);

    /**
     * Programmatic convenience method for recording sensitive healthcare access and mutations.
     */
    AuditLogResponse recordSensitiveAccess(String actorUserId,
                                           AuditAction action,
                                           AuditResourceType resourceType,
                                           String resourceId,
                                           String patientId,
                                           String previousValue,
                                           String newValue,
                                           AuditStatus status,
                                           String details);

    /**
     * Retrieves an individual audit log by its unique identifier.
     */
    AuditLogResponse getAuditLogById(String id);

    /**
     * Searches audit logs dynamically using multi-attribute criteria and pagination.
     */
    PageResponse<AuditLogResponse> searchAuditLogs(AuditSearchCriteria criteria, Pageable pageable);

    /**
     * Retrieves the audit trail of all access and mutations associated with a given patient (HIPAA accounting of disclosures).
     */
    PageResponse<AuditLogResponse> getPatientAuditTrail(String patientId, Pageable pageable);

    /**
     * Retrieves the complete audit history of a specific hospital entity.
     */
    PageResponse<AuditLogResponse> getResourceAuditTrail(AuditResourceType resourceType, String resourceId, Pageable pageable);

    /**
     * Retrieves all actions performed by a specific user or system actor.
     */
    PageResponse<AuditLogResponse> getActorAuditTrail(String actorUserId, Pageable pageable);

    /**
     * Generates an aggregated compliance and security summary report over a specified time interval.
     */
    AuditSummaryReportResponse generateAuditReport(Instant from, Instant to);
}
