package com.careflow.audit.repository;

import com.careflow.audit.domain.AuditAction;

/**
 * Spring Data JPA projection for audit aggregations by action (§36, §103 Phase 15).
 */
public interface AuditCountByAction {
    AuditAction getAction();
    long getCount();
}
