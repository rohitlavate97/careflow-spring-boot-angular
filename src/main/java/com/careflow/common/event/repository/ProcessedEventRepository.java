package com.careflow.common.event.repository;

import com.careflow.common.event.domain.ProcessedEvent;
import com.careflow.common.event.domain.ProcessedEventId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for consumer event deduplication (§51, §76 Scenario 7).
 */
@Repository
public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, ProcessedEventId> {

    boolean existsByEventIdAndConsumerGroup(String eventId, String consumerGroup);
}
