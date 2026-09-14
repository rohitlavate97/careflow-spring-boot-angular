package com.careflow.audit.service;

import com.careflow.audit.domain.AuditAction;
import com.careflow.audit.domain.AuditResourceType;
import com.careflow.audit.domain.AuditStatus;
import com.careflow.audit.dto.AuditLogResponse;
import com.careflow.audit.dto.RecordAuditEventRequest;
import com.careflow.audit.repository.AuditLogRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Concurrency test verifying high-throughput append-only audit logging (§57, §92, §103 Phase 15).
 * Simulates concurrent clinical and administrative operations attempting to write audit logs simultaneously.
 */
@SpringBootTest
@ActiveProfiles("test")
class AuditConcurrencyTest {

    @Autowired
    private AuditService auditService;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Test
    @DisplayName("Multiple concurrent threads can write audit logs simultaneously without deadlocks or collisions")
    void concurrentAuditLogWrites_HighThroughput_Success() throws InterruptedException {
        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());
        List<String> createdLogIds = Collections.synchronizedList(new ArrayList<>());

        String correlationBatchId = "batch-" + UUID.randomUUID();

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    startGate.await();

                    RecordAuditEventRequest request = new RecordAuditEventRequest(
                            "concurrent.user." + index + "@careflow.local",
                            AuditAction.CLINICAL_RECORD_VIEWED,
                            AuditResourceType.CLINICAL_RECORD,
                            "cr-conc-" + index,
                            "pat-conc-batch",
                            null,
                            null,
                            "10.0.1." + (index + 1),
                            correlationBatchId,
                            AuditStatus.SUCCESS,
                            "Concurrent audit trail test execution " + index
                    );

                    AuditLogResponse response = auditService.recordEvent(request);
                    createdLogIds.add(response.id());
                    successCount.incrementAndGet();
                } catch (Throwable t) {
                    errors.add(t);
                } finally {
                    endGate.countDown();
                }
            });
        }

        startGate.countDown();
        boolean completed = endGate.await(15, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).isTrue();
        assertThat(errors).isEmpty();
        assertThat(successCount.get()).isEqualTo(threadCount);
        assertThat(createdLogIds).hasSize(threadCount);

        // Verify distinct primary keys were generated and all are present
        long distinctIds = createdLogIds.stream().distinct().count();
        assertThat(distinctIds).isEqualTo(threadCount);

        for (String id : createdLogIds) {
            assertThat(auditLogRepository.existsById(id)).isTrue();
        }
    }
}
