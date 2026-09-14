package com.careflow.audit.dto;

/**
 * Summary record of activity count per actor (§36, §103 Phase 15).
 */
public record ActorActivitySummary(
        String actorUserId,
        long eventCount
) {
}
