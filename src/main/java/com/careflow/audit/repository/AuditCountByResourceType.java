package com.careflow.audit.repository;

import com.careflow.audit.domain.AuditResourceType;

/**
 * Spring Data JPA projection for audit aggregations by resource type (§36, §103 Phase 15).
 */
public interface AuditCountByResourceType {
    AuditResourceType getResourceType();
    long getCount();
}
