package com.careflow.laboratory.repository;

import com.careflow.laboratory.domain.LabTest;
import com.careflow.laboratory.domain.LabTestCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for diagnostic lab test definitions (§27, §96).
 */
@Repository
public interface LabTestRepository extends JpaRepository<LabTest, String> {

    Optional<LabTest> findByCode(String code);

    Page<LabTest> findByActiveTrue(Pageable pageable);

    Page<LabTest> findByCategoryAndActiveTrue(LabTestCategory category, Pageable pageable);

    boolean existsByCode(String code);
}
