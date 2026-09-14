package com.careflow.audit.controller;

import com.careflow.audit.domain.AuditAction;
import com.careflow.audit.domain.AuditResourceType;
import com.careflow.audit.domain.AuditStatus;
import com.careflow.audit.dto.AuditLogResponse;
import com.careflow.audit.dto.AuditSearchCriteria;
import com.careflow.audit.dto.AuditSummaryReportResponse;
import com.careflow.audit.dto.RecordAuditEventRequest;
import com.careflow.audit.service.AuditService;
import com.careflow.common.dto.PageResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.Instant;

/**
 * REST controller for immutable healthcare audit trails, search, and compliance reporting (§36, §74, §91, §103 Phase 15).
 * Querying audit logs is restricted to administrators and compliance officers.
 * In accordance with HIPAA §164.312(b), audit records are immutable; no update or delete endpoints exist.
 */
@RestController
@RequestMapping("/api/v1/audit")
@Validated
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @PostMapping("/events")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AuditLogResponse> recordEvent(@Valid @RequestBody RecordAuditEventRequest request) {
        AuditLogResponse response = auditService.recordEvent(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .replacePath("/api/v1/audit/logs/{id}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/logs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<AuditLogResponse>> searchAuditLogs(
            @RequestParam(required = false) String actorUserId,
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) AuditResourceType resourceType,
            @RequestParam(required = false) String resourceId,
            @RequestParam(required = false) String patientId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) String correlationId,
            @RequestParam(required = false) AuditStatus status,
            @PageableDefault(size = 20, sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable) {

        AuditSearchCriteria criteria = new AuditSearchCriteria(
                actorUserId,
                action,
                resourceType,
                resourceId,
                patientId,
                from,
                to,
                correlationId,
                status
        );

        return ResponseEntity.ok(auditService.searchAuditLogs(criteria, pageable));
    }

    @GetMapping("/logs/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AuditLogResponse> getAuditLogById(@PathVariable("id") String id) {
        return ResponseEntity.ok(auditService.getAuditLogById(id));
    }

    @GetMapping("/patients/{patientId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<AuditLogResponse>> getPatientAuditTrail(
            @PathVariable("patientId") String patientId,
            @PageableDefault(size = 20, sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable) {

        return ResponseEntity.ok(auditService.getPatientAuditTrail(patientId, pageable));
    }

    @GetMapping("/resources/{resourceType}/{resourceId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<AuditLogResponse>> getResourceAuditTrail(
            @PathVariable("resourceType") AuditResourceType resourceType,
            @PathVariable("resourceId") String resourceId,
            @PageableDefault(size = 20, sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable) {

        return ResponseEntity.ok(auditService.getResourceAuditTrail(resourceType, resourceId, pageable));
    }

    @GetMapping("/actors/{actorUserId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<AuditLogResponse>> getActorAuditTrail(
            @PathVariable("actorUserId") String actorUserId,
            @PageableDefault(size = 20, sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable) {

        return ResponseEntity.ok(auditService.getActorAuditTrail(actorUserId, pageable));
    }

    @GetMapping("/reports/summary")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AuditSummaryReportResponse> getAuditSummaryReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {

        return ResponseEntity.ok(auditService.generateAuditReport(from, to));
    }
}
