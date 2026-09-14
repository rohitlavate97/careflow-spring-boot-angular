package com.careflow.audit.repository;

import com.careflow.audit.domain.AuditLog;
import com.careflow.audit.dto.AuditSearchCriteria;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Dynamic JPA Specification builder for filtering audit logs (§36, §103 Phase 15).
 */
public class AuditLogSpecification {

    private AuditLogSpecification() {
        // Utility class
    }

    public static Specification<AuditLog> build(AuditSearchCriteria criteria) {
        return (root, query, cb) -> {
            if (criteria == null) {
                return cb.conjunction();
            }

            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(criteria.actorUserId())) {
                predicates.add(cb.equal(root.get("actorUserId"), criteria.actorUserId().trim()));
            }

            if (criteria.action() != null) {
                predicates.add(cb.equal(root.get("action"), criteria.action()));
            }

            if (criteria.resourceType() != null) {
                predicates.add(cb.equal(root.get("resourceType"), criteria.resourceType()));
            }

            if (StringUtils.hasText(criteria.resourceId())) {
                predicates.add(cb.equal(root.get("resourceId"), criteria.resourceId().trim()));
            }

            if (StringUtils.hasText(criteria.patientId())) {
                predicates.add(cb.equal(root.get("patientId"), criteria.patientId().trim()));
            }

            if (criteria.fromTimestamp() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("timestamp"), criteria.fromTimestamp()));
            }

            if (criteria.toTimestamp() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("timestamp"), criteria.toTimestamp()));
            }

            if (StringUtils.hasText(criteria.correlationId())) {
                predicates.add(cb.equal(root.get("correlationId"), criteria.correlationId().trim()));
            }

            if (criteria.status() != null) {
                predicates.add(cb.equal(root.get("status"), criteria.status()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
