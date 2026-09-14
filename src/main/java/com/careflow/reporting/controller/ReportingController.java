package com.careflow.reporting.controller;

import com.careflow.common.dto.PageResponse;
import com.careflow.reporting.dto.AppointmentReportResponse;
import com.careflow.reporting.dto.DashboardSummaryResponse;
import com.careflow.reporting.dto.FinancialReportResponse;
import com.careflow.reporting.dto.InpatientReportResponse;
import com.careflow.reporting.dto.LabReportResponse;
import com.careflow.reporting.dto.PharmacyReportResponse;
import com.careflow.reporting.dto.QueueReportResponse;
import com.careflow.reporting.dto.ReportExecutionResponse;
import com.careflow.reporting.service.ReportingService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * REST controller for hospital operational dashboards, clinical throughput analytics,
 * and financial aggregation reports (§37, §91, §103 Phase 16).
 */
@RestController
@RequestMapping("/api/v1/reporting")
@Validated
public class ReportingController {

    private final ReportingService reportingService;

    public ReportingController(ReportingService reportingService) {
        this.reportingService = reportingService;
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN', 'BILLING_OFFICER')")
    public ResponseEntity<DashboardSummaryResponse> getDashboardSummary() {
        return ResponseEntity.ok(reportingService.getDashboardSummary());
    }

    @GetMapping("/appointments")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'RECEPTIONIST')")
    public ResponseEntity<AppointmentReportResponse> getAppointmentReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String departmentId,
            @RequestParam(required = false) String doctorId) {

        return ResponseEntity.ok(reportingService.getAppointmentReport(startDate, endDate, departmentId, doctorId));
    }

    @GetMapping("/queue")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'RECEPTIONIST', 'NURSE')")
    public ResponseEntity<QueueReportResponse> getQueueReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate queueDate,
            @RequestParam(required = false) String departmentId) {

        return ResponseEntity.ok(reportingService.getQueueReport(queueDate, departmentId));
    }

    @GetMapping("/admissions")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'NURSE')")
    public ResponseEntity<InpatientReportResponse> getInpatientReport() {
        return ResponseEntity.ok(reportingService.getInpatientReport());
    }

    @GetMapping("/laboratory")
    @PreAuthorize("hasAnyRole('ADMIN', 'LAB_TECHNICIAN', 'DOCTOR')")
    public ResponseEntity<LabReportResponse> getLabReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        return ResponseEntity.ok(reportingService.getLabReport(startDate, endDate));
    }

    @GetMapping("/pharmacy")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST')")
    public ResponseEntity<PharmacyReportResponse> getPharmacyReport() {
        return ResponseEntity.ok(reportingService.getPharmacyReport());
    }

    @GetMapping("/financial")
    @PreAuthorize("hasAnyRole('ADMIN', 'BILLING_OFFICER')")
    public ResponseEntity<FinancialReportResponse> getFinancialReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        return ResponseEntity.ok(reportingService.getFinancialReport(startDate, endDate));
    }

    @GetMapping("/history")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<ReportExecutionResponse>> getReportExecutionHistory(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        return ResponseEntity.ok(reportingService.getReportExecutionHistory(pageable));
    }
}
