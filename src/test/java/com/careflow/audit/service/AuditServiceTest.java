package com.careflow.audit.service;

import com.careflow.audit.domain.AuditAction;
import com.careflow.audit.domain.AuditLog;
import com.careflow.audit.domain.AuditResourceType;
import com.careflow.audit.domain.AuditStatus;
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
import com.careflow.audit.repository.TopActorSummary;
import com.careflow.common.dto.PageResponse;
import com.careflow.common.exception.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private AuditContextHelper auditContextHelper;

    private AuditMapper auditMapper;
    private AuditServiceImpl auditService;

    @BeforeEach
    void setUp() {
        auditMapper = new AuditMapper();
        auditService = new AuditServiceImpl(auditLogRepository, auditMapper, auditContextHelper);
    }

    @Test
    @DisplayName("recordEvent successfully saves audit log with explicit context")
    void recordEvent_WithExplicitContext_Success() {
        RecordAuditEventRequest request = new RecordAuditEventRequest(
                "dr.smith@careflow.local",
                AuditAction.PATIENT_VIEWED,
                AuditResourceType.PATIENT,
                "pat-100",
                "pat-100",
                null,
                null,
                "192.168.1.50",
                "corr-12345",
                AuditStatus.SUCCESS,
                "Viewed patient medical demographics"
        );

        when(auditContextHelper.resolveActorUserId("dr.smith@careflow.local")).thenReturn("dr.smith@careflow.local");
        when(auditContextHelper.resolveIpAddress("192.168.1.50")).thenReturn("192.168.1.50");
        when(auditContextHelper.resolveCorrelationId("corr-12345")).thenReturn("corr-12345");

        AuditLog savedEntity = new AuditLog(
                UUID.randomUUID().toString(),
                "dr.smith@careflow.local",
                AuditAction.PATIENT_VIEWED,
                AuditResourceType.PATIENT,
                "pat-100",
                "pat-100",
                Instant.now(),
                null,
                null,
                "192.168.1.50",
                "corr-12345",
                AuditStatus.SUCCESS,
                "Viewed patient medical demographics"
        );

        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(savedEntity);

        AuditLogResponse response = auditService.recordEvent(request);

        assertThat(response).isNotNull();
        assertThat(response.actorUserId()).isEqualTo("dr.smith@careflow.local");
        assertThat(response.action()).isEqualTo(AuditAction.PATIENT_VIEWED);
        assertThat(response.resourceType()).isEqualTo(AuditResourceType.PATIENT);
        assertThat(response.resourceId()).isEqualTo("pat-100");
        assertThat(response.patientId()).isEqualTo("pat-100");
        assertThat(response.status()).isEqualTo(AuditStatus.SUCCESS);
        assertThat(response.correlationId()).isEqualTo("corr-12345");

        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("recordEvent auto-resolves missing actor, ip, and correlationId from ambient context")
    void recordEvent_WithMissingContext_AutoResolves() {
        RecordAuditEventRequest request = new RecordAuditEventRequest(
                null,
                AuditAction.CLINICAL_RECORD_UPDATED,
                AuditResourceType.CLINICAL_RECORD,
                "cr-200",
                "pat-100",
                "{\"bloodPressure\": \"120/80\"}",
                "{\"bloodPressure\": \"135/85\"}",
                null,
                null,
                AuditStatus.SUCCESS,
                "Updated vitals after triage"
        );

        when(auditContextHelper.resolveActorUserId(null)).thenReturn("nurse.joy@careflow.local");
        when(auditContextHelper.resolveIpAddress(null)).thenReturn("10.0.0.12");
        when(auditContextHelper.resolveCorrelationId(null)).thenReturn("gen-uuid-999");

        AuditLog savedEntity = new AuditLog(
                UUID.randomUUID().toString(),
                "nurse.joy@careflow.local",
                AuditAction.CLINICAL_RECORD_UPDATED,
                AuditResourceType.CLINICAL_RECORD,
                "cr-200",
                "pat-100",
                Instant.now(),
                "{\"bloodPressure\": \"120/80\"}",
                "{\"bloodPressure\": \"135/85\"}",
                "10.0.0.12",
                "gen-uuid-999",
                AuditStatus.SUCCESS,
                "Updated vitals after triage"
        );

        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(savedEntity);

        AuditLogResponse response = auditService.recordEvent(request);

        assertThat(response.actorUserId()).isEqualTo("nurse.joy@careflow.local");
        assertThat(response.ipAddress()).isEqualTo("10.0.0.12");
        assertThat(response.correlationId()).isEqualTo("gen-uuid-999");
        assertThat(response.previousValue()).isEqualTo("{\"bloodPressure\": \"120/80\"}");
        assertThat(response.newValue()).isEqualTo("{\"bloodPressure\": \"135/85\"}");
    }

    @Test
    @DisplayName("recordSensitiveAccess records event via programmatic convenience helper")
    void recordSensitiveAccess_Success() {
        when(auditContextHelper.resolveActorUserId("admin@careflow.local")).thenReturn("admin@careflow.local");
        when(auditContextHelper.resolveIpAddress(null)).thenReturn("127.0.0.1");
        when(auditContextHelper.resolveCorrelationId(null)).thenReturn("corr-audit");

        AuditLog saved = new AuditLog(
                UUID.randomUUID().toString(),
                "admin@careflow.local",
                AuditAction.CLAIM_APPROVED,
                AuditResourceType.INSURANCE_CLAIM,
                "claim-300",
                "pat-100",
                Instant.now(),
                null,
                null,
                "127.0.0.1",
                "corr-audit",
                AuditStatus.SUCCESS,
                "Pre-authorization claim approved"
        );

        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(saved);

        AuditLogResponse response = auditService.recordSensitiveAccess(
                "admin@careflow.local",
                AuditAction.CLAIM_APPROVED,
                AuditResourceType.INSURANCE_CLAIM,
                "claim-300",
                "pat-100",
                null,
                null,
                AuditStatus.SUCCESS,
                "Pre-authorization claim approved"
        );

        assertThat(response.action()).isEqualTo(AuditAction.CLAIM_APPROVED);
        assertThat(response.resourceId()).isEqualTo("claim-300");
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("getAuditLogById returns log when found")
    void getAuditLogById_Found() {
        String id = UUID.randomUUID().toString();
        AuditLog log = new AuditLog(
                id,
                "admin@careflow.local",
                AuditAction.USER_LOGIN_SUCCESS,
                AuditResourceType.USER,
                "usr-1",
                null,
                Instant.now(),
                null,
                null,
                "127.0.0.1",
                "corr-1",
                AuditStatus.SUCCESS,
                "User logged in"
        );

        when(auditLogRepository.findById(id)).thenReturn(Optional.of(log));

        AuditLogResponse response = auditService.getAuditLogById(id);

        assertThat(response.id()).isEqualTo(id);
        assertThat(response.action()).isEqualTo(AuditAction.USER_LOGIN_SUCCESS);
    }

    @Test
    @DisplayName("getAuditLogById throws AuditLogNotFoundException when not found")
    void getAuditLogById_NotFound_ThrowsException() {
        when(auditLogRepository.findById("non-existent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> auditService.getAuditLogById("non-existent"))
                .isInstanceOf(AuditLogNotFoundException.class)
                .hasMessageContaining("non-existent");
    }

    @Test
    @DisplayName("searchAuditLogs passes criteria specification and returns page response")
    void searchAuditLogs_ReturnsPageResponse() {
        AuditSearchCriteria criteria = new AuditSearchCriteria(
                "dr.smith",
                AuditAction.PATIENT_VIEWED,
                AuditResourceType.PATIENT,
                null,
                null,
                null,
                null,
                null,
                AuditStatus.SUCCESS
        );

        Pageable pageable = PageRequest.of(0, 10);
        AuditLog log = new AuditLog(
                UUID.randomUUID().toString(),
                "dr.smith",
                AuditAction.PATIENT_VIEWED,
                AuditResourceType.PATIENT,
                "pat-1",
                "pat-1",
                Instant.now(),
                null,
                null,
                "127.0.0.1",
                "c-1",
                AuditStatus.SUCCESS,
                null
        );

        Page<AuditLog> page = new PageImpl<>(List.of(log), pageable, 1);
        when(auditLogRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        PageResponse<AuditLogResponse> response = auditService.searchAuditLogs(criteria, pageable);

        assertThat(response.content()).hasSize(1);
        assertThat(response.totalElements()).isEqualTo(1);
        assertThat(response.content().getFirst().actorUserId()).isEqualTo("dr.smith");
    }

    @Test
    @DisplayName("getPatientAuditTrail returns patient HIPAA access logs")
    void getPatientAuditTrail_ReturnsLogs() {
        String patientId = "pat-hipaa-1";
        Pageable pageable = PageRequest.of(0, 20);

        AuditLog log = new AuditLog(
                UUID.randomUUID().toString(),
                "doc1",
                AuditAction.PATIENT_VIEWED,
                AuditResourceType.PATIENT,
                patientId,
                patientId,
                Instant.now(),
                null,
                null,
                "127.0.0.1",
                "c-2",
                AuditStatus.SUCCESS,
                "Reviewed chart"
        );

        when(auditLogRepository.findByPatientIdOrderByTimestampDesc(patientId, pageable))
                .thenReturn(new PageImpl<>(List.of(log), pageable, 1));

        PageResponse<AuditLogResponse> response = auditService.getPatientAuditTrail(patientId, pageable);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().getFirst().patientId()).isEqualTo(patientId);
    }

    @Test
    @DisplayName("getResourceAuditTrail returns resource mutation history")
    void getResourceAuditTrail_ReturnsLogs() {
        Pageable pageable = PageRequest.of(0, 10);
        AuditLog log = new AuditLog(
                UUID.randomUUID().toString(),
                "billing1",
                AuditAction.INVOICE_UPDATED,
                AuditResourceType.INVOICE,
                "inv-10",
                "pat-10",
                Instant.now(),
                "{\"status\": \"DRAFT\"}",
                "{\"status\": \"ISSUED\"}",
                "127.0.0.1",
                "c-3",
                AuditStatus.SUCCESS,
                "Issued invoice"
        );

        when(auditLogRepository.findByResourceTypeAndResourceIdOrderByTimestampDesc(
                AuditResourceType.INVOICE, "inv-10", pageable))
                .thenReturn(new PageImpl<>(List.of(log), pageable, 1));

        PageResponse<AuditLogResponse> response = auditService.getResourceAuditTrail(
                AuditResourceType.INVOICE, "inv-10", pageable);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().getFirst().resourceType()).isEqualTo(AuditResourceType.INVOICE);
    }

    @Test
    @DisplayName("generateAuditReport aggregates actions, resources, statuses, top actors, and violations")
    void generateAuditReport_Success() {
        Instant to = Instant.now();
        Instant from = to.minus(7, ChronoUnit.DAYS);

        when(auditLogRepository.countByTimestampBetween(from, to)).thenReturn(150L);

        AuditCountByAction countAction = mock(AuditCountByAction.class);
        when(countAction.getAction()).thenReturn(AuditAction.PATIENT_VIEWED);
        when(countAction.getCount()).thenReturn(100L);
        when(auditLogRepository.countGroupedByAction(from, to)).thenReturn(List.of(countAction));

        AuditCountByResourceType countResource = mock(AuditCountByResourceType.class);
        when(countResource.getResourceType()).thenReturn(AuditResourceType.PATIENT);
        when(countResource.getCount()).thenReturn(100L);
        when(auditLogRepository.countGroupedByResourceType(from, to)).thenReturn(List.of(countResource));

        AuditCountByStatus countStatus = mock(AuditCountByStatus.class);
        when(countStatus.getStatus()).thenReturn(AuditStatus.SUCCESS);
        when(countStatus.getCount()).thenReturn(145L);
        when(auditLogRepository.countGroupedByStatus(from, to)).thenReturn(List.of(countStatus));

        TopActorSummary topActor = mock(TopActorSummary.class);
        when(topActor.getActorUserId()).thenReturn("dr.smith");
        when(topActor.getCount()).thenReturn(65L);
        when(auditLogRepository.findTopActors(eq(from), eq(to), any(Pageable.class)))
                .thenReturn(List.of(topActor));

        AuditLog violationLog = new AuditLog(
                UUID.randomUUID().toString(),
                "malicious.user",
                AuditAction.ACCESS_DENIED,
                AuditResourceType.CLINICAL_RECORD,
                "cr-secret",
                "pat-vip",
                Instant.now(),
                null,
                null,
                "198.51.100.1",
                "corr-v",
                AuditStatus.ACCESS_DENIED,
                "Unauthorized access to VIP medical record"
        );
        when(auditLogRepository.findRecentByStatus(eq(from), eq(to), eq(AuditStatus.ACCESS_DENIED), any(Pageable.class)))
                .thenReturn(List.of(violationLog));

        AuditSummaryReportResponse report = auditService.generateAuditReport(from, to);

        assertThat(report.totalEvents()).isEqualTo(150L);
        assertThat(report.eventsByAction()).containsEntry("PATIENT_VIEWED", 100L);
        assertThat(report.eventsByResourceType()).containsEntry("PATIENT", 100L);
        assertThat(report.eventsByStatus()).containsEntry("SUCCESS", 145L);
        assertThat(report.topActors()).hasSize(1);
        assertThat(report.topActors().getFirst().actorUserId()).isEqualTo("dr.smith");
        assertThat(report.topActors().getFirst().eventCount()).isEqualTo(65L);
        assertThat(report.recentAccessViolations()).hasSize(1);
        assertThat(report.recentAccessViolations().getFirst().actorUserId()).isEqualTo("malicious.user");
    }

    @Test
    @DisplayName("generateAuditReport throws BusinessRuleException when from is chronologically after to")
    void generateAuditReport_InvalidDateRange_ThrowsException() {
        Instant from = Instant.now();
        Instant to = from.minus(1, ChronoUnit.DAYS);

        assertThatThrownBy(() -> auditService.generateAuditReport(from, to))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("chronologically earlier");
    }
}
