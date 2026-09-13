package com.careflow.staff.repository;

import com.careflow.staff.domain.StaffMember;
import com.careflow.staff.domain.StaffStatus;
import com.careflow.staff.domain.StaffType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Spring Data JPA Specification builder for dynamic staff filtering and directory searches (§17, §72).
 */
public final class StaffMemberSpecification {

    private StaffMemberSpecification() {
        // Utility class
    }

    public static Specification<StaffMember> withFilters(
            String query,
            String departmentId,
            StaffType staffType,
            StaffStatus status
    ) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (query != null && !query.isBlank()) {
                String searchPattern = "%" + query.trim().toLowerCase() + "%";
                Predicate firstNameMatch = criteriaBuilder.like(criteriaBuilder.lower(root.get("firstName")), searchPattern);
                Predicate lastNameMatch = criteriaBuilder.like(criteriaBuilder.lower(root.get("lastName")), searchPattern);
                Predicate staffCodeMatch = criteriaBuilder.like(criteriaBuilder.lower(root.get("staffCode")), searchPattern);
                Predicate emailMatch = criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), searchPattern);

                predicates.add(criteriaBuilder.or(firstNameMatch, lastNameMatch, staffCodeMatch, emailMatch));
            }

            if (departmentId != null && !departmentId.isBlank()) {
                predicates.add(criteriaBuilder.equal(root.get("departmentId"), departmentId.trim()));
            }

            if (staffType != null) {
                predicates.add(criteriaBuilder.equal(root.get("staffType"), staffType));
            }

            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
