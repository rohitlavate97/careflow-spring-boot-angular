package com.careflow.common.event;

import java.time.Instant;

/**
 * Fundamental contract for all CareFlow asynchronous domain events (§51, §53).
 * Events represent immutable historical facts published after database transactions commit.
 */
public interface DomainEvent {

    String getEventId();

    String getEventType();

    String getAggregateType();

    String getAggregateId();

    Instant getOccurredAt();

    default int getVersion() {
        return 1;
    }
}
