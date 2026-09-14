package com.careflow.common.outbox.service;

import com.careflow.common.outbox.domain.OutboxEvent;
import com.careflow.common.outbox.domain.OutboxStatus;
import com.careflow.common.outbox.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Background relay service polling pending transactional outbox events and dispatching them to Kafka (§53).
 * Decouples business transactions from external broker latency and ensures resilience when Kafka is unavailable.
 */
@Service
public class OutboxRelayService {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelayService.class);

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final boolean kafkaEnabled;

    @Autowired
    public OutboxRelayService(
            OutboxEventRepository outboxEventRepository,
            @Autowired(required = false) KafkaTemplate<String, String> kafkaTemplate,
            @Value("${careflow.kafka.enabled:true}") boolean kafkaEnabled
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.kafkaEnabled = kafkaEnabled;
    }

    /**
     * Polls pending outbox records and publishes them to Kafka with per-event error containment.
     * Scheduled every 2 seconds.
     */
    @Scheduled(fixedDelayString = "${careflow.outbox.relay-interval-ms:2000}")
    @Transactional
    public int relayPendingEvents() {
        if (!kafkaEnabled || kafkaTemplate == null) {
            log.trace("Kafka publishing is disabled. Outbox events will accumulate safely in DB.");
            return 0;
        }

        List<OutboxEvent> pendingEvents = outboxEventRepository.findTop50ByStatusOrderByOccurredAtAsc(OutboxStatus.PENDING);
        if (pendingEvents.isEmpty()) {
            return 0;
        }

        log.debug("Found {} pending outbox event(s) to publish to Kafka", pendingEvents.size());
        int successCount = 0;

        for (OutboxEvent event : pendingEvents) {
            try {
                // Synchronous get with timeout to ensure at-least-once delivery confirmation before marking published
                kafkaTemplate.send(event.getTopic(), event.getPartitionKey(), event.getPayload())
                        .get(3, TimeUnit.SECONDS);

                event.markPublished(Instant.now());
                successCount++;
                log.info("Dispatched outbox event to Kafka: id='{}', topic='{}', partitionKey='{}'",
                        event.getId(), event.getTopic(), event.getPartitionKey());
            } catch (Exception ex) {
                String errorMsg = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
                log.warn("Kafka unavailable or send timed out for outbox event id='{}'. Core workflow unaffected: {}",
                        event.getId(), errorMsg);
                event.recordFailure(errorMsg);
            }
        }

        outboxEventRepository.saveAll(pendingEvents);
        return successCount;
    }
}
