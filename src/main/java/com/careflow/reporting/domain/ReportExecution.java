package com.careflow.reporting.domain;

import com.careflow.common.domain.BaseAuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.util.Objects;
import java.util.UUID;

/**
 * Audit record of an executed operational or analytical report query (§37, §74, §103 Phase 16).
 * Maintains accountability of who extracted hospital clinical, operational, and financial aggregates.
 */
@Entity
@Table(
        name = "report_executions",
        indexes = {
                @Index(name = "idx_report_exec_type_time", columnList = "report_type, created_at"),
                @Index(name = "idx_report_exec_user_time", columnList = "requested_by, created_at")
        }
)
public class ReportExecution extends BaseAuditEntity {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(name = "report_type", length = 50, nullable = false)
    private ReportType reportType;

    @Column(name = "requested_by", length = 100, nullable = false)
    private String requestedBy;

    @Column(name = "parameters", length = 1000)
    private String parameters;

    @Column(name = "execution_time_ms", nullable = false)
    private Long executionTimeMs;

    @Column(name = "status", length = 20, nullable = false)
    private String status = "COMPLETED";

    protected ReportExecution() {
        // JPA requirement
    }

    public ReportExecution(String id,
                           ReportType reportType,
                           String requestedBy,
                           String parameters,
                           Long executionTimeMs,
                           String status) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.reportType = Objects.requireNonNull(reportType, "reportType must not be null");
        this.requestedBy = Objects.requireNonNull(requestedBy, "requestedBy must not be null");
        this.parameters = parameters;
        this.executionTimeMs = executionTimeMs != null ? executionTimeMs : 0L;
        this.status = status != null ? status : "COMPLETED";
    }

    public static ReportExecution record(ReportType reportType,
                                         String requestedBy,
                                         String parameters,
                                         long executionTimeMs) {
        return new ReportExecution(
                UUID.randomUUID().toString(),
                reportType,
                requestedBy,
                parameters,
                executionTimeMs,
                "COMPLETED"
        );
    }

    public String getId() {
        return id;
    }

    public ReportType getReportType() {
        return reportType;
    }

    public String getRequestedBy() {
        return requestedBy;
    }

    public String getParameters() {
        return parameters;
    }

    public Long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
