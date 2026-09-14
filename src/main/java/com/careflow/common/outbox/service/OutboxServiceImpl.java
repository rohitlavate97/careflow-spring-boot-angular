package com.careflow.common.outbox.service;

import com.careflow.common.event.DomainEvent;
import com.careflow.common.outbox.domain.OutboxEvent;
import com.careflow.common.outbox.repository.OutboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of OutboxService writing domain events to the outbox table within active transactions (§53).
 */
@Service
public class OutboxServiceImpl implements OutboxService {

    private static final Logger log = LoggerFactory.getLogger(OutboxServiceImpl.class);

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public OutboxServiceImpl(OutboxEventRepository outboxEventRepository, ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void saveEvent(DomainEvent event, String topic, String partitionKey) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            OutboxEvent outboxEvent = new OutboxEvent(
                    event.getEventId(),
                    event.getAggregateType(),
                    event.getAggregateId(),
                    event.getEventType(),
                    payload,
                    topic,
                    partitionKey,
                    event.getOccurredAt()
            );

            outboxEventRepository.save(outboxEvent);
            log.debug("Outbox event saved: id='{}', type='{}', topic='{}'",
                    event.getEventId(), event.getEventType(), topic);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize domain event id='{}', type='{}'",
                    event.getEventId(), event.getEventType(), e);
            throw new IllegalStateException("Failed to serialize outbox event payload", e);
        }
    }
}
