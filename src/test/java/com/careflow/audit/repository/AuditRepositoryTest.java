package com.careflow.audit.repository;

import com.careflow.audit.domain.AuditAction;
import com.careflow.audit.domain.AuditLog;
import com.careflow.audit.domain.AuditResourceType;
import com.careflow.audit.domain.AuditStatus;
import com.careflow.audit.dto.AuditSearchCriteria;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
class AuditRepositoryTest {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private EntityManager entityManager;

    private Instant baseTime;
    private AuditLog log1;
    private AuditLog log2;
    private AuditLog log3;

    @BeforeEach
    void setUp() {
        baseTime = Instant.now().minus(2, ChronoUnit.HOURS);

        log1 = new AuditLog(
                UUID.randomUUID().toString(),
                "doctor.jones@careflow.local",
                AuditAction.PATIENT_VIEWED,
                AuditResourceType.PATIENT,
                "pat-001",
                "pat-001",
                baseTime.plus(10, ChronoUnit.MINUTES),
                null,
                null,
                "192.168.1.10",
                "corr-rep-1",
                AuditStatus.SUCCESS,
                "Routine checkup view"
        );
        auditLogRepository.save(log1);

        log2 = new AuditLog(
                UUID.randomUUID().toString(),
                "doctor.jones@careflow.local",
                AuditAction.CLINICAL_RECORD_UPDATED,
                AuditResourceType.CLINICAL_RECORD,
                "cr-001",
                "pat-001",
                baseTime.plus(20, ChronoUnit.MINUTES),
                "{\"stage\": 1}",
                "{\"stage\": 2}",
                "192.168.1.10",
                "corr-rep-2",
                AuditStatus.SUCCESS,
                "Updated treatment stage"
        );
        auditLogRepository.save(log2);

        log3 = new AuditLog(
                UUID.randomUUID().toString(),
                "unknown.user",
                AuditAction.ACCESS_DENIED,
                AuditResourceType.INSURANCE_CLAIM,
                "claim-001",
                "pat-002",
                baseTime.plus(30, ChronoUnit.MINUTES),
                null,
                null,
                "10.0.0.5",
                "corr-rep-3",
                AuditStatus.ACCESS_DENIED,
                "Forbidden attempt to access claims"
        );
        auditLogRepository.save(log3);

        entityManager.flush();
    }

    @Test
    @DisplayName("AuditLog enforces immutability and throws exception on deletion attempt")
    void immutability_DeleteAttempt_ThrowsException() {
        assertThatThrownBy(() -> {
            auditLogRepository.delete(log1);
            entityManager.flush();
        }).isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("immutable");
    }

    @Test
    @DisplayName("AuditLog enforces immutability and throws exception on direct preUpdate call")
    void immutability_PreUpdate_ThrowsException() {
        assertThatThrownBy(() -> log1.onPreUpdate())
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("immutable");
    }

    @Test
    @DisplayName("findByPatientIdOrderByTimestampDesc returns all logs for patient ordered by time")
    void findByPatientId_ReturnsOrderedLogs() {
        Page<AuditLog> results = auditLogRepository.findByPatientIdOrderByTimestampDesc(
                "pat-001", PageRequest.of(0, 10));

        assertThat(results.getContent()).hasSize(2);
        assertThat(results.getContent().get(0).getId()).isEqualTo(log2.getId());
        assertThat(results.getContent().get(1).getId()).isEqualTo(log1.getId());
    }

    @Test
    @DisplayName("findByResourceTypeAndResourceId returns matching logs")
    void findByResourceTypeAndResourceId_ReturnsMatchingLogs() {
        Page<AuditLog> results = auditLogRepository.findByResourceTypeAndResourceIdOrderByTimestampDesc(
                AuditResourceType.CLINICAL_RECORD, "cr-001", PageRequest.of(0, 10));

        assertThat(results.getContent()).hasSize(1);
        assertThat(results.getContent().getFirst().getAction()).isEqualTo(AuditAction.CLINICAL_RECORD_UPDATED);
    }

    @Test
    @DisplayName("AuditLogSpecification filters accurately by multi-attribute criteria")
    void specificationSearch_FiltersAccurately() {
        AuditSearchCriteria criteria = new AuditSearchCriteria(
                "doctor.jones@careflow.local",
                AuditAction.CLINICAL_RECORD_UPDATED,
                AuditResourceType.CLINICAL_RECORD,
                "cr-001",
                "pat-001",
                baseTime,
                Instant.now(),
                "corr-rep-2",
                AuditStatus.SUCCESS
        );

        Page<AuditLog> page = auditLogRepository.findAll(AuditLogSpecification.build(criteria), PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().getFirst().getId()).isEqualTo(log2.getId());
    }

    @Test
    @DisplayName("countGroupedByAction aggregates audit event counts correctly")
    void countGroupedByAction_AggregatesAccurately() {
        List<AuditCountByAction> counts = auditLogRepository.countGroupedByAction(
                baseTime.minus(1, ChronoUnit.HOURS), Instant.now());

        assertThat(counts).isNotEmpty();
        long patientViewCount = counts.stream()
                .filter(c -> c.getAction() == AuditAction.PATIENT_VIEWED)
                .mapToLong(AuditCountByAction::getCount)
                .sum();
        assertThat(patientViewCount).isEqualTo(1);
    }

    @Test
    @DisplayName("countGroupedByStatus aggregates outcomes correctly")
    void countGroupedByStatus_AggregatesAccurately() {
        List<AuditCountByStatus> counts = auditLogRepository.countGroupedByStatus(
                baseTime.minus(1, ChronoUnit.HOURS), Instant.now());

        long successCount = counts.stream()
                .filter(c -> c.getStatus() == AuditStatus.SUCCESS)
                .mapToLong(AuditCountByStatus::getCount)
                .sum();
        long accessDeniedCount = counts.stream()
                .filter(c -> c.getStatus() == AuditStatus.ACCESS_DENIED)
                .mapToLong(AuditCountByStatus::getCount)
                .sum();

        assertThat(successCount).isEqualTo(2);
        assertThat(accessDeniedCount).isEqualTo(1);
    }

    @Test
    @DisplayName("findTopActors ranks the most active users")
    void findTopActors_RanksCorrectly() {
        List<TopActorSummary> topActors = auditLogRepository.findTopActors(
                baseTime.minus(1, ChronoUnit.HOURS), Instant.now(), PageRequest.of(0, 5));

        assertThat(topActors).isNotEmpty();
        assertThat(topActors.getFirst().getActorUserId()).isEqualTo("doctor.jones@careflow.local");
        assertThat(topActors.getFirst().getCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("findRecentByStatus retrieves access violation incidents")
    void findRecentByStatus_RetrievesViolations() {
        List<AuditLog> violations = auditLogRepository.findRecentByStatus(
                baseTime.minus(1, ChronoUnit.HOURS), Instant.now(), AuditStatus.ACCESS_DENIED, PageRequest.of(0, 10));

        assertThat(violations).hasSize(1);
        assertThat(violations.getFirst().getActorUserId()).isEqualTo("unknown.user");
        assertThat(violations.getFirst().getStatus()).isEqualTo(AuditStatus.ACCESS_DENIED);
    }
}
