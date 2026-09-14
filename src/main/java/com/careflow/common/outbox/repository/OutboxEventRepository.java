package com.careflow.common.outbox.repository;

import com.careflow.common.outbox.domain.OutboxEvent;
import com.careflow.common.outbox.domain.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for Transactional Outbox persistence and polling (§53).
 */
@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, String> {

    List<OutboxEvent> findTop50ByStatusOrderByOccurredAtAsc(OutboxStatus status);

    long countByStatus(OutboxStatus status);
}
