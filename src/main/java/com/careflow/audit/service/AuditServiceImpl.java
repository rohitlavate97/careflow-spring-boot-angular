package com.careflow.audit.service;

import com.careflow.audit.domain.AuditAction;
import com.careflow.audit.domain.AuditLog;
import com.careflow.audit.domain.AuditResourceType;
import com.careflow.audit.domain.AuditStatus;
import com.careflow.audit.dto.ActorActivitySummary;
import com.careflow.audit.dto.AuditLogResponse;
import com.careflow.audit.dto.AuditSearchCriteria;
import com.careflow.audit.dto.AuditSummaryReportResponse;
import com.careflow.audit.dto.RecordAuditEventRequest;
import com.careflow.audit.exception.AuditLogNotFoundException;
import com.careflow.audit.mapper.AuditMapper;
import com.careflow.audit.repository.AuditCountByAction;
import com.careflow.audit.repository.AuditCountByResourceType;
import com.careflow.audit.repository.AuditCountByStatus;
import com.careflow.audit.repository.AuditLogRepository;
import com.careflow.audit.repository.AuditLogSpecification;
import com.careflow.audit.repository.TopActorSummary;
import com.careflow.common.dto.PageResponse;
import com.careflow.common.exception.BusinessRuleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service implementation managing immutable audit logs (§36, §74, §103 Phase 15).
 * Uses Propagation.REQUIRES_NEW for audit recording to guarantee persistence
 * regardless of whether the calling transaction commits or encounters a rollback.
 */
@Service
public class AuditServiceImpl implements AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditServiceImpl.class);

    private final AuditLogRepository auditLogRepository;
    private final AuditMapper auditMapper;
    private final AuditContextHelper auditContextHelper;

    public AuditServiceImpl(AuditLogRepository auditLogRepository,
                            AuditMapper auditMapper,
                            AuditContextHelper auditContextHelper) {
        this.auditLogRepository = auditLogRepository;
        this.auditMapper = auditMapper;
        this.auditContextHelper = auditContextHelper;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditLogResponse recordEvent(RecordAuditEventRequest request) {
        String resolvedActor = auditContextHelper.resolveActorUserId(request.actorUserId());
        String resolvedIp = auditContextHelper.resolveIpAddress(request.ipAddress());
        String resolvedCorrelationId = auditContextHelper.resolveCorrelationId(request.correlationId());

        AuditLog auditLog = auditMapper.toEntity(request, resolvedActor, resolvedIp, resolvedCorrelationId);
        AuditLog saved = auditLogRepository.save(auditLog);

        log.info("Audit log recorded: id={}, actor={}, action={}, resource={}:{}, patient={}, status={}, correlationId={}",
                saved.getId(),
                saved.getActorUserId(),
                saved.getAction(),
                saved.getResourceType(),
                saved.getResourceId(),
                saved.getPatientId(),
                saved.getStatus(),
                saved.getCorrelationId());

        return auditMapper.toResponse(saved);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditLogResponse recordSensitiveAccess(String actorUserId,
                                                  AuditAction action,
                                                  AuditResourceType resourceType,
                                                  String resourceId,
                                                  String patientId,
                                                  String previousValue,
                                                  String newValue,
                                                  AuditStatus status,
                                                  String details) {
        RecordAuditEventRequest request = new RecordAuditEventRequest(
                actorUserId,
                action,
                resourceType,
                resourceId,
                patientId,
                previousValue,
                newValue,
                null,
                null,
                status != null ? status : AuditStatus.SUCCESS,
                details
        );
        return recordEvent(request);
    }

    @Override
    @Transactional(readOnly = true)
    public AuditLogResponse getAuditLogById(String id) {
        return auditLogRepository.findById(id)
                .map(auditMapper::toResponse)
                .orElseThrow(() -> new AuditLogNotFoundException(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> searchAuditLogs(AuditSearchCriteria criteria, Pageable pageable) {
        Page<AuditLog> page = auditLogRepository.findAll(AuditLogSpecification.build(criteria), pageable);
        return PageResponse.from(page, auditMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> getPatientAuditTrail(String patientId, Pageable pageable) {
        Page<AuditLog> page = auditLogRepository.findByPatientIdOrderByTimestampDesc(patientId, pageable);
        return PageResponse.from(page, auditMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> getResourceAuditTrail(AuditResourceType resourceType, String resourceId, Pageable pageable) {
        Page<AuditLog> page = auditLogRepository.findByResourceTypeAndResourceIdOrderByTimestampDesc(resourceType, resourceId, pageable);
        return PageResponse.from(page, auditMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> getActorAuditTrail(String actorUserId, Pageable pageable) {
        Page<AuditLog> page = auditLogRepository.findByActorUserIdOrderByTimestampDesc(actorUserId, pageable);
        return PageResponse.from(page, auditMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public AuditSummaryReportResponse generateAuditReport(Instant from, Instant to) {
        Instant effectiveTo = to != null ? to : Instant.now();
        Instant effectiveFrom = from != null ? from : effectiveTo.minus(7, ChronoUnit.DAYS);

        if (effectiveFrom.isAfter(effectiveTo)) {
            throw new BusinessRuleException(
                    "INVALID_DATE_RANGE",
                    "The 'from' timestamp must be chronologically earlier than the 'to' timestamp.",
                    HttpStatus.BAD_REQUEST
            );
        }

        long totalEvents = auditLogRepository.countByTimestampBetween(effectiveFrom, effectiveTo);

        Map<String, Long> eventsByAction = auditLogRepository.countGroupedByAction(effectiveFrom, effectiveTo)
                .stream()
                .collect(Collectors.toMap(
                        p -> p.getAction().name(),
                        AuditCountByAction::getCount,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));

        Map<String, Long> eventsByResourceType = auditLogRepository.countGroupedByResourceType(effectiveFrom, effectiveTo)
                .stream()
                .collect(Collectors.toMap(
                        p -> p.getResourceType().name(),
                        AuditCountByResourceType::getCount,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));

        Map<String, Long> eventsByStatus = auditLogRepository.countGroupedByStatus(effectiveFrom, effectiveTo)
                .stream()
                .collect(Collectors.toMap(
                        p -> p.getStatus().name(),
                        AuditCountByStatus::getCount,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));

        List<ActorActivitySummary> topActors = auditLogRepository.findTopActors(
                        effectiveFrom, effectiveTo, PageRequest.of(0, 10))
                .stream()
                .map(p -> new ActorActivitySummary(p.getActorUserId(), p.getCount()))
                .toList();

        List<AuditLogResponse> recentViolations = auditLogRepository.findRecentByStatus(
                        effectiveFrom, effectiveTo, AuditStatus.ACCESS_DENIED, PageRequest.of(0, 20))
                .stream()
                .map(auditMapper::toResponse)
                .toList();

        return new AuditSummaryReportResponse(
                effectiveFrom,
                effectiveTo,
                totalEvents,
                eventsByAction,
                eventsByResourceType,
                eventsByStatus,
                topActors,
                recentViolations
        );
    }
}
