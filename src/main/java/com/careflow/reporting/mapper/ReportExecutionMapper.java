package com.careflow.reporting.mapper;

import com.careflow.reporting.domain.ReportExecution;
import com.careflow.reporting.dto.ReportExecutionResponse;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting ReportExecution entities to DTO responses (§89).
 */
@Component
public class ReportExecutionMapper {

    public ReportExecutionResponse toResponse(ReportExecution execution) {
        if (execution == null) {
            return null;
        }

        return new ReportExecutionResponse(
                execution.getId(),
                execution.getReportType(),
                execution.getRequestedBy(),
                execution.getParameters(),
                execution.getExecutionTimeMs(),
                execution.getStatus(),
                execution.getCreatedAt()
        );
    }
}
