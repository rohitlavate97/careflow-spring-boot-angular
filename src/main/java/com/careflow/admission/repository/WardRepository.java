package com.careflow.admission.repository;

import com.careflow.admission.domain.Ward;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for hospital inpatient wards (§28).
 */
@Repository
public interface WardRepository extends JpaRepository<Ward, String> {

    Optional<Ward> findByWardCode(String wardCode);

    Page<Ward> findByActiveTrue(Pageable pageable);

    Page<Ward> findByDepartmentIdAndActiveTrue(String departmentId, Pageable pageable);

    boolean existsByWardCode(String wardCode);
}
