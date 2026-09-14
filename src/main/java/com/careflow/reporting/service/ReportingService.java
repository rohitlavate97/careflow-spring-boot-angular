package com.careflow.reporting.service;

import com.careflow.common.dto.PageResponse;
import com.careflow.reporting.dto.AppointmentReportResponse;
import com.careflow.reporting.dto.DashboardSummaryResponse;
import com.careflow.reporting.dto.FinancialReportResponse;
import com.careflow.reporting.dto.InpatientReportResponse;
import com.careflow.reporting.dto.LabReportResponse;
import com.careflow.reporting.dto.PharmacyReportResponse;
import com.careflow.reporting.dto.QueueReportResponse;
import com.careflow.reporting.dto.ReportExecutionResponse;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

/**
 * Service interface for operational reporting, dashboard aggregations, and KPI queries (§37, §103 Phase 16).
 * Employs direct database aggregations to prevent memory bloat.
 */
public interface ReportingService {

    /**
     * Computes the high-level executive operational dashboard snapshot for the current day.
     */
    DashboardSummaryResponse getDashboardSummary();

    /**
     * Generates appointment volume trends, cancellation/no-show rates, department distribution,
     * and doctor schedule utilization metrics.
     */
    AppointmentReportResponse getAppointmentReport(LocalDate startDate,
                                                  LocalDate endDate,
                                                  String departmentId,
                                                  String doctorId);

    /**
     * Generates outpatient queue throughput, wait time metrics, and priority distribution.
     */
    QueueReportResponse getQueueReport(LocalDate queueDate, String departmentId);

    /**
     * Generates inpatient admission statistics, bed capacity, and ward occupancy percentages.
     */
    InpatientReportResponse getInpatientReport();

    /**
     * Generates diagnostic laboratory order volumes, order status breakdown,
     * average turnaround time (TAT), and top requested tests.
     */
    LabReportResponse getLabReport(LocalDate startDate, LocalDate endDate);

    /**
     * Generates pharmacy inventory valuation, stockout alerts, expired batches, and reorder metrics.
     */
    PharmacyReportResponse getPharmacyReport();

    /**
     * Generates revenue cycle metrics, invoiced vs collected totals, outstanding balances,
     * invoice status breakdown, and insurance claims adjudication rates.
     */
    FinancialReportResponse getFinancialReport(LocalDate startDate, LocalDate endDate);

    /**
     * Retrieves paginated audit history of executed operational reports.
     */
    PageResponse<ReportExecutionResponse> getReportExecutionHistory(Pageable pageable);
}
