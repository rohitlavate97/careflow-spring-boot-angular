package com.careflow.patient.repository;

import com.careflow.patient.domain.Gender;
import com.careflow.patient.domain.Patient;
import com.careflow.patient.domain.PatientStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Spring Data JPA Specification builder for dynamic patient search and multi-attribute filtering (§16, §72).
 */
public class PatientSpecification {

    private PatientSpecification() {
        // Utility class
    }

    public static Specification<Patient> withFilters(
            String query,
            Gender gender,
            PatientStatus status,
            LocalDate dateOfBirth
    ) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (query != null && !query.isBlank()) {
                String searchPattern = "%" + query.trim().toLowerCase() + "%";
                Predicate firstNameMatch = criteriaBuilder.like(criteriaBuilder.lower(root.get("firstName")), searchPattern);
                Predicate lastNameMatch = criteriaBuilder.like(criteriaBuilder.lower(root.get("lastName")), searchPattern);
                Predicate mrnMatch = criteriaBuilder.like(criteriaBuilder.lower(root.get("mrn")), searchPattern);
                Predicate phoneMatch = criteriaBuilder.like(root.get("phone"), "%" + query.trim() + "%");

                predicates.add(criteriaBuilder.or(firstNameMatch, lastNameMatch, mrnMatch, phoneMatch));
            }

            if (gender != null) {
                predicates.add(criteriaBuilder.equal(root.get("gender"), gender));
            }

            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }

            if (dateOfBirth != null) {
                predicates.add(criteriaBuilder.equal(root.get("dateOfBirth"), dateOfBirth));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
