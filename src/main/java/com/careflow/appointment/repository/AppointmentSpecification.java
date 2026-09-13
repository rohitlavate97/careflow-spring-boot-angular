package com.careflow.appointment.repository;

import com.careflow.appointment.domain.Appointment;
import com.careflow.appointment.domain.AppointmentStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Spring Data JPA Specification builder for dynamic appointment searching and reporting (§19, §72).
 */
public final class AppointmentSpecification {

    private AppointmentSpecification() {
        // Utility class
    }

    public static Specification<Appointment> withFilters(
            String patientId,
            String doctorId,
            String departmentId,
            AppointmentStatus status,
            LocalDateTime fromDateTime,
            LocalDateTime toDateTime
    ) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (patientId != null && !patientId.isBlank()) {
                predicates.add(criteriaBuilder.equal(root.get("patientId"), patientId.trim()));
            }

            if (doctorId != null && !doctorId.isBlank()) {
                predicates.add(criteriaBuilder.equal(root.get("doctorId"), doctorId.trim()));
            }

            if (departmentId != null && !departmentId.isBlank()) {
                predicates.add(criteriaBuilder.equal(root.get("departmentId"), departmentId.trim()));
            }

            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }

            if (fromDateTime != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("appointmentDateTime"), fromDateTime));
            }

            if (toDateTime != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("appointmentDateTime"), toDateTime));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
