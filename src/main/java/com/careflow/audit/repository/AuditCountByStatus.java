package com.careflow.audit.repository;

import com.careflow.audit.domain.AuditStatus;

/**
 * Spring Data JPA projection for audit aggregations by execution status (§36, §103 Phase 15).
 */
public interface AuditCountByStatus {
    AuditStatus getStatus();
    long getCount();
}
