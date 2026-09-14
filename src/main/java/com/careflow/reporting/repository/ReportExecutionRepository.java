package com.careflow.reporting.repository;

import com.careflow.reporting.domain.ReportExecution;
import com.careflow.reporting.domain.ReportType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for ReportExecution entities (§37, §103 Phase 16).
 */
@Repository
public interface ReportExecutionRepository extends JpaRepository<ReportExecution, String> {

    Page<ReportExecution> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<ReportExecution> findByReportTypeOrderByCreatedAtDesc(ReportType reportType, Pageable pageable);

    Page<ReportExecution> findByRequestedByOrderByCreatedAtDesc(String requestedBy, Pageable pageable);
}
