package com.careflow.audit.repository;

/**
 * Spring Data JPA projection for the most active actors in the audit log (§36, §103 Phase 15).
 */
public interface TopActorSummary {
    String getActorUserId();
    long getCount();
}
