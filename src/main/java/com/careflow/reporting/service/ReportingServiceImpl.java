package com.careflow.reporting.service;

import com.careflow.admission.domain.AdmissionStatus;
import com.careflow.admission.domain.BedStatus;
import com.careflow.admission.repository.AdmissionRepository;
import com.careflow.admission.repository.BedRepository;
import com.careflow.appointment.domain.AppointmentStatus;
import com.careflow.appointment.repository.AppointmentRepository;
import com.careflow.billing.domain.InvoiceStatus;
import com.careflow.billing.repository.InvoiceRepository;
import com.careflow.common.dto.PageResponse;
import com.careflow.common.exception.BusinessRuleException;
import com.careflow.department.domain.Department;
import com.careflow.department.repository.DepartmentRepository;
import com.careflow.insurance.repository.InsuranceClaimRepository;
import com.careflow.laboratory.domain.LabOrderStatus;
import com.careflow.laboratory.repository.LabOrderRepository;
import com.careflow.pharmacy.domain.PharmacyInventoryBatch;
import com.careflow.pharmacy.repository.PharmacyInventoryBatchRepository;
import com.careflow.queue.domain.QueueStatus;
import com.careflow.queue.repository.QueueEntryRepository;
import com.careflow.reporting.domain.ReportExecution;
import com.careflow.reporting.domain.ReportType;
import com.careflow.reporting.dto.AppointmentReportResponse;
import com.careflow.reporting.dto.DailyAppointmentMetric;
import com.careflow.reporting.dto.DashboardSummaryResponse;
import com.careflow.reporting.dto.DepartmentVolumeMetric;
import com.careflow.reporting.dto.DoctorUtilizationMetric;
import com.careflow.reporting.dto.FinancialReportResponse;
import com.careflow.reporting.dto.InpatientReportResponse;
import com.careflow.reporting.dto.InsuranceClaimsReportMetric;
import com.careflow.reporting.dto.LabReportResponse;
import com.careflow.reporting.dto.LowStockAlertMetric;
import com.careflow.reporting.dto.PharmacyReportResponse;
import com.careflow.reporting.dto.QueueReportResponse;
import com.careflow.reporting.dto.ReportExecutionResponse;
import com.careflow.reporting.dto.TopLabTestMetric;
import com.careflow.reporting.dto.WardOccupancyMetric;
import com.careflow.reporting.mapper.ReportExecutionMapper;
import com.careflow.reporting.repository.ReportExecutionRepository;
import com.careflow.staff.domain.StaffMember;
import com.careflow.staff.repository.StaffMemberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Service implementation for operational reporting and real-time dashboard analytics (§37, §103 Phase 16).
 * Utilizes direct database aggregation queries to avoid loading large entity sets into memory.
 */
@Service
public class ReportingServiceImpl implements ReportingService {

    private static final Logger log = LoggerFactory.getLogger(ReportingServiceImpl.class);

    private final ReportExecutionRepository reportExecutionRepository;
    private final ReportExecutionMapper reportExecutionMapper;
    private final AppointmentRepository appointmentRepository;
    private final QueueEntryRepository queueEntryRepository;
    private final AdmissionRepository admissionRepository;
    private final BedRepository bedRepository;
    private final LabOrderRepository labOrderRepository;
    private final PharmacyInventoryBatchRepository pharmacyRepository;
    private final InvoiceRepository invoiceRepository;
    private final InsuranceClaimRepository insuranceClaimRepository;
    private final DepartmentRepository departmentRepository;
    private final StaffMemberRepository staffMemberRepository;

    public ReportingServiceImpl(ReportExecutionRepository reportExecutionRepository,
                                ReportExecutionMapper reportExecutionMapper,
                                AppointmentRepository appointmentRepository,
                                QueueEntryRepository queueEntryRepository,
                                AdmissionRepository admissionRepository,
                                BedRepository bedRepository,
                                LabOrderRepository labOrderRepository,
                                PharmacyInventoryBatchRepository pharmacyRepository,
                                InvoiceRepository invoiceRepository,
                                InsuranceClaimRepository insuranceClaimRepository,
                                DepartmentRepository departmentRepository,
                                StaffMemberRepository staffMemberRepository) {
        this.reportExecutionRepository = reportExecutionRepository;
        this.reportExecutionMapper = reportExecutionMapper;
        this.appointmentRepository = appointmentRepository;
        this.queueEntryRepository = queueEntryRepository;
        this.admissionRepository = admissionRepository;
        this.bedRepository = bedRepository;
        this.labOrderRepository = labOrderRepository;
        this.pharmacyRepository = pharmacyRepository;
        this.invoiceRepository = invoiceRepository;
        this.insuranceClaimRepository = insuranceClaimRepository;
        this.departmentRepository = departmentRepository;
        this.staffMemberRepository = staffMemberRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardSummaryResponse getDashboardSummary() {
        long startTime = System.currentTimeMillis();
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);
        Instant startInstant = startOfDay.toInstant(ZoneOffset.UTC);
        Instant endInstant = endOfDay.toInstant(ZoneOffset.UTC);

        // 1. Appointments
        long todayAppointments = appointmentRepository.countAppointmentsInRange(startOfDay, endOfDay);
        long todayCompleted = appointmentRepository.countAppointmentsInRangeByStatus(
                startOfDay, endOfDay, AppointmentStatus.COMPLETED);
        long todayCancelled = appointmentRepository.countAppointmentsInRangeByStatus(
                startOfDay, endOfDay, AppointmentStatus.CANCELLED);
        double cancellationRate = todayAppointments > 0
                ? round(((double) todayCancelled / todayAppointments) * 100.0)
                : 0.0;

        // 2. Inpatients & Beds
        long activeAdmissions = admissionRepository.countByStatus(AdmissionStatus.ADMITTED);
        List<Object[]> bedCounts = bedRepository.countBedsByStatus();
        long totalBeds = 0;
        long occupiedBeds = 0;
        for (Object[] row : bedCounts) {
            BedStatus status = (BedStatus) row[0];
            long count = ((Number) row[1]).longValue();
            totalBeds += count;
            if (status == BedStatus.OCCUPIED) {
                occupiedBeds = count;
            }
        }
        double occupancyRate = totalBeds > 0
                ? round(((double) occupiedBeds / totalBeds) * 100.0)
                : 0.0;

        // 3. Queue
        List<Object[]> queueCounts = queueEntryRepository.countQueueByStatus(today, null);
        long activeQueueWaiting = 0;
        for (Object[] row : queueCounts) {
            QueueStatus status = (QueueStatus) row[0];
            if (status == QueueStatus.WAITING) {
                activeQueueWaiting = ((Number) row[1]).longValue();
                break;
            }
        }

        // 4. Lab & Pharmacy Alerts
        long pendingLabOrders = labOrderRepository.countByStatusIn(
                List.of(LabOrderStatus.ORDERED, LabOrderStatus.SAMPLE_COLLECTED, LabOrderStatus.PROCESSING));
        long pharmacyReorderAlerts = pharmacyRepository.countStockAlerts(today.plusDays(30));

        // 5. Financials
        BigDecimal totalBilledToday = Optional.ofNullable(
                invoiceRepository.sumTotalInvoicedInRange(startInstant, endInstant)).orElse(BigDecimal.ZERO);
        BigDecimal totalCollectedToday = Optional.ofNullable(
                invoiceRepository.sumTotalPaidInRange(startInstant, endInstant)).orElse(BigDecimal.ZERO);

        recordAuditExecution(ReportType.EXECUTIVE_DASHBOARD, "today=" + today, startTime);

        return new DashboardSummaryResponse(
                Instant.now(),
                todayAppointments,
                todayCompleted,
                cancellationRate,
                activeAdmissions,
                occupancyRate,
                activeQueueWaiting,
                pendingLabOrders,
                pharmacyReorderAlerts,
                totalBilledToday,
                totalCollectedToday
        );
    }

    @Override
    @Transactional(readOnly = true)
    public AppointmentReportResponse getAppointmentReport(LocalDate startDate,
                                                          LocalDate endDate,
                                                          String departmentId,
                                                          String doctorId) {
        long startTime = System.currentTimeMillis();
        LocalDate effectiveEnd = endDate != null ? endDate : LocalDate.now();
        LocalDate effectiveStart = startDate != null ? startDate : effectiveEnd.minusDays(7);

        validateDateRange(effectiveStart, effectiveEnd);

        LocalDateTime startDateTime = effectiveStart.atStartOfDay();
        LocalDateTime endDateTime = effectiveEnd.atTime(LocalTime.MAX);

        // Status breakdown
        List<Object[]> statusCounts = appointmentRepository.countAppointmentsByStatusInRange(
                startDateTime, endDateTime, departmentId, doctorId);

        long total = 0;
        long completed = 0;
        long cancelled = 0;
        long noShow = 0;

        for (Object[] row : statusCounts) {
            AppointmentStatus status = (AppointmentStatus) row[0];
            long count = ((Number) row[1]).longValue();
            total += count;
            if (status == AppointmentStatus.COMPLETED) {
                completed = count;
            } else if (status == AppointmentStatus.CANCELLED) {
                cancelled = count;
            } else if (status == AppointmentStatus.NO_SHOW) {
                noShow = count;
            }
        }

        double cancellationRate = total > 0 ? round(((double) cancelled / total) * 100.0) : 0.0;
        double noShowRate = total > 0 ? round(((double) noShow / total) * 100.0) : 0.0;

        // Daily trend
        List<Object[]> timeline = appointmentRepository.findAppointmentsTimelineInRange(
                startDateTime, endDateTime, departmentId, doctorId);

        Map<LocalDate, long[]> dailyMap = new TreeMap<>();
        for (Object[] row : timeline) {
            LocalDateTime dt = (LocalDateTime) row[0];
            AppointmentStatus status = (AppointmentStatus) row[1];
            LocalDate day = dt.toLocalDate();

            long[] stats = dailyMap.computeIfAbsent(day, k -> new long[4]); // total, completed, cancelled, noShow
            stats[0]++;
            if (status == AppointmentStatus.COMPLETED) stats[1]++;
            else if (status == AppointmentStatus.CANCELLED) stats[2]++;
            else if (status == AppointmentStatus.NO_SHOW) stats[3]++;
        }

        List<DailyAppointmentMetric> dailyTrend = dailyMap.entrySet().stream()
                .map(e -> new DailyAppointmentMetric(
                        e.getKey(),
                        e.getValue()[0],
                        e.getValue()[1],
                        e.getValue()[2],
                        e.getValue()[3]
                ))
                .toList();

        // Department breakdown
        List<Object[]> deptCounts = appointmentRepository.countAppointmentsByDepartmentInRange(startDateTime, endDateTime);
        Map<String, String> departmentNames = departmentRepository.findAll().stream()
                .collect(Collectors.toMap(Department::getId, Department::getName, (a, b) -> a));

        List<DepartmentVolumeMetric> departmentBreakdown = deptCounts.stream()
                .map(row -> {
                    String deptId = (String) row[0];
                    long count = ((Number) row[1]).longValue();
                    String name = departmentNames.getOrDefault(deptId, "Department " + deptId);
                    return new DepartmentVolumeMetric(deptId, name, count);
                })
                .toList();

        // Doctor utilization
        List<Object[]> docCounts = appointmentRepository.countDoctorUtilizationInRange(startDateTime, endDateTime);
        Map<String, StaffMember> staffMap = staffMemberRepository.findAll().stream()
                .collect(Collectors.toMap(StaffMember::getId, s -> s, (a, b) -> a));

        List<DoctorUtilizationMetric> doctorUtilization = docCounts.stream()
                .map(row -> {
                    String docId = (String) row[0];
                    long docTotal = ((Number) row[1]).longValue();
                    long docCompleted = ((Number) row[2]).longValue();
                    long docCancelled = ((Number) row[3]).longValue();

                    StaffMember staff = staffMap.get(docId);
                    String doctorName = staff != null ? "Dr. " + staff.getFirstName() + " " + staff.getLastName() : "Doctor " + docId;
                    String deptName = staff != null && departmentNames.containsKey(staff.getDepartmentId())
                            ? departmentNames.get(staff.getDepartmentId()) : "General";

                    double utilization = docTotal > 0 ? round(((double) docCompleted / docTotal) * 100.0) : 0.0;
                    return new DoctorUtilizationMetric(
                            docId,
                            doctorName,
                            deptName,
                            docTotal,
                            docCompleted,
                            docCancelled,
                            utilization
                    );
                })
                .toList();

        recordAuditExecution(ReportType.APPOINTMENT_ANALYTICS,
                "start=" + effectiveStart + ",end=" + effectiveEnd + ",dept=" + departmentId + ",doc=" + doctorId,
                startTime);

        return new AppointmentReportResponse(
                effectiveStart,
                effectiveEnd,
                total,
                completed,
                cancelled,
                noShow,
                cancellationRate,
                noShowRate,
                dailyTrend,
                departmentBreakdown,
                doctorUtilization
        );
    }

    @Override
    @Transactional(readOnly = true)
    public QueueReportResponse getQueueReport(LocalDate queueDate, String departmentId) {
        long startTime = System.currentTimeMillis();
        LocalDate effectiveDate = queueDate != null ? queueDate : LocalDate.now();

        List<Object[]> statusCounts = queueEntryRepository.countQueueByStatus(effectiveDate, departmentId);
        long total = 0;
        long waiting = 0;
        long inConsultation = 0;
        long completed = 0;
        long cancelled = 0;

        for (Object[] row : statusCounts) {
            QueueStatus status = (QueueStatus) row[0];
            long count = ((Number) row[1]).longValue();
            total += count;
            if (status == QueueStatus.WAITING) waiting = count;
            else if (status == QueueStatus.CALLED || status == QueueStatus.IN_CONSULTATION) inConsultation += count;
            else if (status == QueueStatus.COMPLETED) completed = count;
            else if (status == QueueStatus.CANCELLED || status == QueueStatus.SKIPPED) cancelled += count;
        }

        List<Object[]> priorityCounts = queueEntryRepository.countQueueByPriority(effectiveDate, departmentId);
        Map<String, Long> priorityBreakdown = new LinkedHashMap<>();
        for (Object[] row : priorityCounts) {
            priorityBreakdown.put(row[0].toString(), ((Number) row[1]).longValue());
        }

        List<Object[]> waitTimes = queueEntryRepository.findQueueWaitTimes(effectiveDate, departmentId);
        double totalWaitMinutes = 0;
        long waitCount = 0;
        for (Object[] row : waitTimes) {
            Instant entryTime = (Instant) row[0];
            Instant calledTime = (Instant) row[1];
            if (entryTime != null && calledTime != null) {
                totalWaitMinutes += Duration.between(entryTime, calledTime).toSeconds() / 60.0;
                waitCount++;
            }
        }
        double avgWaitTimeMinutes = waitCount > 0 ? round(totalWaitMinutes / waitCount) : 0.0;

        recordAuditExecution(ReportType.QUEUE_ANALYTICS,
                "date=" + effectiveDate + ",dept=" + departmentId,
                startTime);

        return new QueueReportResponse(
                effectiveDate,
                departmentId,
                total,
                waiting,
                inConsultation,
                completed,
                cancelled,
                priorityBreakdown,
                avgWaitTimeMinutes
        );
    }

    @Override
    @Transactional(readOnly = true)
    public InpatientReportResponse getInpatientReport() {
        long startTime = System.currentTimeMillis();

        List<Object[]> admissionCounts = admissionRepository.countAdmissionsByStatus();
        long totalAdmissions = 0;
        long activeAdmissions = 0;
        long discharged = 0;
        long transferred = 0;

        for (Object[] row : admissionCounts) {
            AdmissionStatus status = (AdmissionStatus) row[0];
            long count = ((Number) row[1]).longValue();
            totalAdmissions += count;
            if (status == AdmissionStatus.ADMITTED) activeAdmissions = count;
            else if (status == AdmissionStatus.DISCHARGED) discharged = count;
            else if (status == AdmissionStatus.TRANSFERRED) transferred = count;
        }

        List<Object[]> bedCounts = bedRepository.countBedsByStatus();
        long totalBeds = 0;
        long occupiedBeds = 0;
        long availableBeds = 0;

        for (Object[] row : bedCounts) {
            BedStatus status = (BedStatus) row[0];
            long count = ((Number) row[1]).longValue();
            totalBeds += count;
            if (status == BedStatus.OCCUPIED) occupiedBeds = count;
            else if (status == BedStatus.AVAILABLE) availableBeds = count;
        }

        double overallOccupancyRate = totalBeds > 0 ? round(((double) occupiedBeds / totalBeds) * 100.0) : 0.0;

        List<Object[]> wardCounts = bedRepository.countWardBedOccupancy();
        List<WardOccupancyMetric> wardOccupancy = wardCounts.stream()
                .map(row -> {
                    String wardId = (String) row[0];
                    String wardName = (String) row[1];
                    String wardType = row[2] != null ? row[2].toString() : "GENERAL";
                    long wTotal = ((Number) row[3]).longValue();
                    long wOccupied = ((Number) row[4]).longValue();
                    double wRate = wTotal > 0 ? round(((double) wOccupied / wTotal) * 100.0) : 0.0;
                    return new WardOccupancyMetric(wardId, wardName, wardType, wTotal, wOccupied, wRate);
                })
                .toList();

        recordAuditExecution(ReportType.INPATIENT_ANALYTICS, "type=all", startTime);

        return new InpatientReportResponse(
                totalAdmissions,
                activeAdmissions,
                discharged,
                transferred,
                totalBeds,
                occupiedBeds,
                availableBeds,
                overallOccupancyRate,
                wardOccupancy
        );
    }

    @Override
    @Transactional(readOnly = true)
    public LabReportResponse getLabReport(LocalDate startDate, LocalDate endDate) {
        long startTime = System.currentTimeMillis();
        LocalDate effectiveEnd = endDate != null ? endDate : LocalDate.now();
        LocalDate effectiveStart = startDate != null ? startDate : effectiveEnd.minusDays(30);

        validateDateRange(effectiveStart, effectiveEnd);

        Instant startInstant = effectiveStart.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant endInstant = effectiveEnd.atTime(LocalTime.MAX).toInstant(ZoneOffset.UTC);

        List<Object[]> statusCounts = labOrderRepository.countLabOrdersByStatus(startInstant, endInstant);
        long totalOrders = 0;
        Map<String, Long> ordersByStatus = new LinkedHashMap<>();

        for (Object[] row : statusCounts) {
            String status = row[0].toString();
            long count = ((Number) row[1]).longValue();
            totalOrders += count;
            ordersByStatus.put(status, count);
        }

        List<Object[]> completedTimes = labOrderRepository.findCompletedLabOrderTimes(startInstant, endInstant);
        double totalDurationHours = 0;
        long completedCount = 0;
        for (Object[] row : completedTimes) {
            Instant orderedAt = (Instant) row[0];
            Instant completedAt = (Instant) row[1];
            if (orderedAt != null && completedAt != null) {
                totalDurationHours += Duration.between(orderedAt, completedAt).toSeconds() / 3600.0;
                completedCount++;
            }
        }
        double avgTurnaroundHours = completedCount > 0 ? round(totalDurationHours / completedCount) : 0.0;

        List<Object[]> topTestsRaw = labOrderRepository.findTopRequestedLabTests(
                startInstant, endInstant, PageRequest.of(0, 10));

        List<TopLabTestMetric> topTests = topTestsRaw.stream()
                .map(row -> new TopLabTestMetric(
                        (String) row[0],
                        (String) row[1],
                        (String) row[2],
                        ((Number) row[3]).longValue()
                ))
                .toList();

        recordAuditExecution(ReportType.LABORATORY_ANALYTICS,
                "start=" + effectiveStart + ",end=" + effectiveEnd,
                startTime);

        return new LabReportResponse(
                effectiveStart,
                effectiveEnd,
                totalOrders,
                ordersByStatus,
                avgTurnaroundHours,
                topTests
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PharmacyReportResponse getPharmacyReport() {
        long startTime = System.currentTimeMillis();
        LocalDate today = LocalDate.now();
        LocalDate nearExpiryDate = today.plusDays(30);

        List<Object[]> aggResults = pharmacyRepository.getPharmacyInventoryAggregates(today, nearExpiryDate);
        long totalBatches = 0;
        long totalStock = 0;
        long expiredBatches = 0;
        long expiringWithin30Days = 0;
        long lowStockBatches = 0;
        long outOfStockBatches = 0;

        if (!aggResults.isEmpty() && aggResults.getFirst() != null) {
            Object[] row = aggResults.getFirst();
            totalBatches = ((Number) row[0]).longValue();
            totalStock = ((Number) row[1]).longValue();
            expiredBatches = row[2] != null ? ((Number) row[2]).longValue() : 0;
            expiringWithin30Days = row[3] != null ? ((Number) row[3]).longValue() : 0;
            lowStockBatches = row[4] != null ? ((Number) row[4]).longValue() : 0;
            outOfStockBatches = row[5] != null ? ((Number) row[5]).longValue() : 0;
        }

        List<PharmacyInventoryBatch> criticalBatches = pharmacyRepository.findCriticalStockBatches(
                PageRequest.of(0, 20));

        List<LowStockAlertMetric> criticalAlerts = criticalBatches.stream()
                .map(b -> new LowStockAlertMetric(
                        b.getMedicationId(),
                        b.getBatchNumber(),
                        b.getQuantityAvailable(),
                        b.getReorderThreshold(),
                        b.getExpiryDate()
                ))
                .toList();

        recordAuditExecution(ReportType.PHARMACY_ANALYTICS, "nearExpiryDate=" + nearExpiryDate, startTime);

        return new PharmacyReportResponse(
                totalBatches,
                totalStock,
                expiredBatches,
                expiringWithin30Days,
                lowStockBatches,
                outOfStockBatches,
                criticalAlerts
        );
    }

    @Override
    @Transactional(readOnly = true)
    public FinancialReportResponse getFinancialReport(LocalDate startDate, LocalDate endDate) {
        long startTime = System.currentTimeMillis();
        LocalDate effectiveEnd = endDate != null ? endDate : LocalDate.now();
        LocalDate effectiveStart = startDate != null ? startDate : effectiveEnd.minusDays(30);

        validateDateRange(effectiveStart, effectiveEnd);

        Instant startInstant = effectiveStart.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant endInstant = effectiveEnd.atTime(LocalTime.MAX).toInstant(ZoneOffset.UTC);

        // Invoice financial totals
        List<Object[]> totals = invoiceRepository.getInvoiceFinancialTotals(startInstant, endInstant);
        BigDecimal totalInvoiced = BigDecimal.ZERO;
        BigDecimal totalPaid = BigDecimal.ZERO;
        BigDecimal totalOutstanding = BigDecimal.ZERO;

        if (!totals.isEmpty() && totals.getFirst() != null) {
            Object[] row = totals.getFirst();
            totalInvoiced = (BigDecimal) row[0];
            totalPaid = (BigDecimal) row[1];
            totalOutstanding = (BigDecimal) row[2];
        }

        double collectionRate = totalInvoiced.compareTo(BigDecimal.ZERO) > 0
                ? round(totalPaid.divide(totalInvoiced, 4, RoundingMode.HALF_UP).doubleValue() * 100.0)
                : 0.0;

        List<Object[]> invoiceStatusCounts = invoiceRepository.countInvoicesByStatus(startInstant, endInstant);
        Map<String, Long> invoicesByStatus = new LinkedHashMap<>();
        for (Object[] row : invoiceStatusCounts) {
            invoicesByStatus.put(row[0].toString(), ((Number) row[1]).longValue());
        }

        // Insurance claims aggregates
        List<Object[]> claimAggregates = insuranceClaimRepository.getInsuranceClaimAggregates(startInstant, endInstant);
        long totalClaims = 0;
        BigDecimal totalClaimed = BigDecimal.ZERO;
        BigDecimal totalApproved = BigDecimal.ZERO;
        long approvedCount = 0;
        long rejectedCount = 0;

        if (!claimAggregates.isEmpty() && claimAggregates.getFirst() != null) {
            Object[] row = claimAggregates.getFirst();
            totalClaims = ((Number) row[0]).longValue();
            totalClaimed = (BigDecimal) row[1];
            totalApproved = (BigDecimal) row[2];
            approvedCount = row[3] != null ? ((Number) row[3]).longValue() : 0;
            rejectedCount = row[4] != null ? ((Number) row[4]).longValue() : 0;
        }

        double approvalRate = totalClaims > 0 ? round(((double) approvedCount / totalClaims) * 100.0) : 0.0;

        InsuranceClaimsReportMetric insuranceMetrics = new InsuranceClaimsReportMetric(
                totalClaims,
                totalClaimed,
                totalApproved,
                approvedCount,
                rejectedCount,
                approvalRate
        );

        recordAuditExecution(ReportType.FINANCIAL_ANALYTICS,
                "start=" + effectiveStart + ",end=" + effectiveEnd,
                startTime);

        return new FinancialReportResponse(
                effectiveStart,
                effectiveEnd,
                totalInvoiced,
                totalPaid,
                totalOutstanding,
                collectionRate,
                invoicesByStatus,
                insuranceMetrics
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReportExecutionResponse> getReportExecutionHistory(Pageable pageable) {
        Page<ReportExecution> page = reportExecutionRepository.findAllByOrderByCreatedAtDesc(pageable);
        return PageResponse.from(page, reportExecutionMapper::toResponse);
    }

    private void recordAuditExecution(ReportType type, String params, long startTime) {
        long duration = System.currentTimeMillis() - startTime;
        String requestedBy = resolveCurrentUsername();
        ReportExecution execution = ReportExecution.record(type, requestedBy, params, duration);
        try {
            reportExecutionRepository.save(execution);
            log.info("Report generated: type={}, requestedBy={}, executionTimeMs={}", type, requestedBy, duration);
        } catch (Exception e) {
            log.warn("Failed to audit report execution: {}", e.getMessage());
        }
    }

    private String resolveCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getName().equalsIgnoreCase("anonymousUser")) {
            return auth.getName();
        }
        return "SYSTEM";
    }

    private void validateDateRange(LocalDate start, LocalDate end) {
        if (start != null && end != null && start.isAfter(end)) {
            throw new BusinessRuleException(
                    "INVALID_DATE_RANGE",
                    "The start date must be chronologically on or before the end date.",
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
