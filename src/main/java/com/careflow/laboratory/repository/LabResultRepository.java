package com.careflow.laboratory.repository;

import com.careflow.laboratory.domain.LabResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for diagnostic lab results (§27).
 */
@Repository
public interface LabResultRepository extends JpaRepository<LabResult, String> {

    List<LabResult> findByOrderItemId(String orderItemId);
}
