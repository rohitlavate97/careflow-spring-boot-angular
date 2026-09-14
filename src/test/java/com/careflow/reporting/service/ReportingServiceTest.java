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
import com.careflow.insurance.domain.ClaimStatus;
import com.careflow.insurance.repository.InsuranceClaimRepository;
import com.careflow.laboratory.domain.LabOrderStatus;
import com.careflow.laboratory.repository.LabOrderRepository;
import com.careflow.pharmacy.domain.PharmacyInventoryBatch;
import com.careflow.pharmacy.repository.PharmacyInventoryBatchRepository;
import com.careflow.queue.domain.QueuePriority;
import com.careflow.queue.domain.QueueStatus;
import com.careflow.queue.repository.QueueEntryRepository;
import com.careflow.reporting.domain.ReportExecution;
import com.careflow.reporting.domain.ReportType;
import com.careflow.reporting.dto.AppointmentReportResponse;
import com.careflow.reporting.dto.DashboardSummaryResponse;
import com.careflow.reporting.dto.FinancialReportResponse;
import com.careflow.reporting.dto.InpatientReportResponse;
import com.careflow.reporting.dto.LabReportResponse;
import com.careflow.reporting.dto.PharmacyReportResponse;
import com.careflow.reporting.dto.QueueReportResponse;
import com.careflow.reporting.dto.ReportExecutionResponse;
import com.careflow.reporting.mapper.ReportExecutionMapper;
import com.careflow.reporting.repository.ReportExecutionRepository;
import com.careflow.staff.domain.StaffMember;
import com.careflow.staff.repository.StaffMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportingServiceTest {

    @Mock
    private ReportExecutionRepository reportExecutionRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private QueueEntryRepository queueEntryRepository;

    @Mock
    private AdmissionRepository admissionRepository;

    @Mock
    private BedRepository bedRepository;

    @Mock
    private LabOrderRepository labOrderRepository;

    @Mock
    private PharmacyInventoryBatchRepository pharmacyRepository;

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private InsuranceClaimRepository insuranceClaimRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private StaffMemberRepository staffMemberRepository;

    private ReportExecutionMapper reportExecutionMapper;
    private ReportingServiceImpl reportingService;

    @BeforeEach
    void setUp() {
        reportExecutionMapper = new ReportExecutionMapper();
        reportingService = new ReportingServiceImpl(
                reportExecutionRepository,
                reportExecutionMapper,
                appointmentRepository,
                queueEntryRepository,
                admissionRepository,
                bedRepository,
                labOrderRepository,
                pharmacyRepository,
                invoiceRepository,
                insuranceClaimRepository,
                departmentRepository,
                staffMemberRepository
        );
    }

    @Test
    @DisplayName("getDashboardSummary calculates executive operational indicators correctly")
    void getDashboardSummary_CalculatesAccurately() {
        when(appointmentRepository.countAppointmentsInRange(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(50L);
        when(appointmentRepository.countAppointmentsInRangeByStatus(any(LocalDateTime.class), any(LocalDateTime.class), eq(AppointmentStatus.COMPLETED)))
                .thenReturn(35L);
        when(appointmentRepository.countAppointmentsInRangeByStatus(any(LocalDateTime.class), any(LocalDateTime.class), eq(AppointmentStatus.CANCELLED)))
                .thenReturn(5L);

        when(admissionRepository.countByStatus(AdmissionStatus.ADMITTED)).thenReturn(24L);

        List<Object[]> bedCounts = new ArrayList<>();
        bedCounts.add(new Object[]{BedStatus.AVAILABLE, 20L});
        bedCounts.add(new Object[]{BedStatus.OCCUPIED, 80L});
        when(bedRepository.countBedsByStatus()).thenReturn(bedCounts);

        List<Object[]> queueCounts = new ArrayList<>();
        queueCounts.add(new Object[]{QueueStatus.WAITING, 12L});
        when(queueEntryRepository.countQueueByStatus(any(LocalDate.class), eq(null))).thenReturn(queueCounts);

        when(labOrderRepository.countByStatusIn(any())).thenReturn(8L);
        when(pharmacyRepository.countStockAlerts(any(LocalDate.class))).thenReturn(3L);

        when(invoiceRepository.sumTotalInvoicedInRange(any(Instant.class), any(Instant.class)))
                .thenReturn(new BigDecimal("12500.00"));
        when(invoiceRepository.sumTotalPaidInRange(any(Instant.class), any(Instant.class)))
                .thenReturn(new BigDecimal("9500.00"));

        DashboardSummaryResponse dashboard = reportingService.getDashboardSummary();

        assertThat(dashboard).isNotNull();
        assertThat(dashboard.todayAppointments()).isEqualTo(50L);
        assertThat(dashboard.todayCompletedAppointments()).isEqualTo(35L);
        assertThat(dashboard.appointmentCancellationRate()).isEqualTo(10.0); // 5/50 = 10%
        assertThat(dashboard.activeAdmissions()).isEqualTo(24L);
        assertThat(dashboard.bedOccupancyRate()).isEqualTo(80.0); // 80/100 = 80%
        assertThat(dashboard.activeQueueWaiting()).isEqualTo(12L);
        assertThat(dashboard.pendingLabOrders()).isEqualTo(8L);
        assertThat(dashboard.pharmacyReorderAlerts()).isEqualTo(3L);
        assertThat(dashboard.totalBilledToday()).isEqualByComparingTo("12500.00");
        assertThat(dashboard.totalCollectedToday()).isEqualByComparingTo("9500.00");

        verify(reportExecutionRepository).save(any(ReportExecution.class));
    }

    @Test
    @DisplayName("getAppointmentReport aggregates volumes, cancellation rates, and doctor utilization")
    void getAppointmentReport_Success() {
        LocalDate start = LocalDate.now().minusDays(7);
        LocalDate end = LocalDate.now();

        List<Object[]> statusCounts = List.of(
                new Object[]{AppointmentStatus.COMPLETED, 80L},
                new Object[]{AppointmentStatus.CANCELLED, 15L},
                new Object[]{AppointmentStatus.NO_SHOW, 5L}
        );
        when(appointmentRepository.countAppointmentsByStatusInRange(any(), any(), eq(null), eq(null)))
                .thenReturn(statusCounts);

        LocalDateTime aptTime = start.atTime(10, 0);
        List<Object[]> timeline = List.of(
                new Object[]{aptTime, AppointmentStatus.COMPLETED},
                new Object[]{aptTime, AppointmentStatus.CANCELLED}
        );
        when(appointmentRepository.findAppointmentsTimelineInRange(any(), any(), eq(null), eq(null)))
                .thenReturn(timeline);

        when(appointmentRepository.countAppointmentsByDepartmentInRange(any(), any()))
                .thenReturn(Collections.singletonList(new Object[]{"dept-card-1", 50L}));

        when(departmentRepository.findAll()).thenReturn(List.of(
                new Department("dept-card-1", "CARDIO", "Cardiology", "Cardiology Department", "Building A")
        ));

        when(appointmentRepository.countDoctorUtilizationInRange(any(), any()))
                .thenReturn(Collections.singletonList(new Object[]{"doc-1", 50L, 40L, 5L}));

        StaffMember doctor = new StaffMember(
                "doc-1", "DOC-001", "dept-card-1", "Gregory", "House",
                "house@careflow.local", "+1-555", com.careflow.staff.domain.StaffType.DOCTOR, LocalDate.now()
        );
        when(staffMemberRepository.findAll()).thenReturn(List.of(doctor));

        AppointmentReportResponse report = reportingService.getAppointmentReport(start, end, null, null);

        assertThat(report.totalAppointments()).isEqualTo(100L);
        assertThat(report.completedAppointments()).isEqualTo(80L);
        assertThat(report.cancelledAppointments()).isEqualTo(15L);
        assertThat(report.noShowAppointments()).isEqualTo(5L);
        assertThat(report.cancellationRate()).isEqualTo(15.0);
        assertThat(report.noShowRate()).isEqualTo(5.0);
        assertThat(report.departmentBreakdown()).hasSize(1);
        assertThat(report.departmentBreakdown().getFirst().departmentName()).isEqualTo("Cardiology");
        assertThat(report.doctorUtilization()).hasSize(1);
        assertThat(report.doctorUtilization().getFirst().doctorName()).isEqualTo("Dr. Gregory House");
        assertThat(report.doctorUtilization().getFirst().utilizationRate()).isEqualTo(80.0); // 40/50 = 80%
    }

    @Test
    @DisplayName("getAppointmentReport throws BusinessRuleException on invalid date order")
    void getAppointmentReport_InvalidDateRange_ThrowsException() {
        LocalDate start = LocalDate.now();
        LocalDate end = start.minusDays(1);

        assertThatThrownBy(() -> reportingService.getAppointmentReport(start, end, null, null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("chronologically on or before");
    }

    @Test
    @DisplayName("getQueueReport aggregates queue status, priorities, and average wait times")
    void getQueueReport_Success() {
        LocalDate date = LocalDate.now();

        when(queueEntryRepository.countQueueByStatus(date, "dept-1"))
                .thenReturn(List.of(
                        new Object[]{QueueStatus.WAITING, 10L},
                        new Object[]{QueueStatus.IN_CONSULTATION, 3L},
                        new Object[]{QueueStatus.COMPLETED, 25L}
                ));

        when(queueEntryRepository.countQueueByPriority(date, "dept-1"))
                .thenReturn(List.of(
                        new Object[]{QueuePriority.NORMAL, 20L},
                        new Object[]{QueuePriority.URGENT, 15L},
                        new Object[]{QueuePriority.EMERGENCY, 3L}
                ));

        Instant entry = Instant.now().minusSeconds(1200); // 20 minutes ago
        Instant called = Instant.now();
        when(queueEntryRepository.findQueueWaitTimes(date, "dept-1"))
                .thenReturn(Collections.singletonList(new Object[]{entry, called}));

        QueueReportResponse report = reportingService.getQueueReport(date, "dept-1");

        assertThat(report.totalQueued()).isEqualTo(38L);
        assertThat(report.waitingCount()).isEqualTo(10L);
        assertThat(report.completedCount()).isEqualTo(25L);
        assertThat(report.priorityBreakdown()).containsEntry("URGENT", 15L);
        assertThat(report.averageWaitTimeMinutes()).isEqualTo(20.0);
    }

    @Test
    @DisplayName("getInpatientReport aggregates bed capacity and ward occupancy")
    void getInpatientReport_Success() {
        when(admissionRepository.countAdmissionsByStatus())
                .thenReturn(List.of(
                        new Object[]{AdmissionStatus.ADMITTED, 15L},
                        new Object[]{AdmissionStatus.DISCHARGED, 45L}
                ));

        when(bedRepository.countBedsByStatus())
                .thenReturn(List.of(
                        new Object[]{BedStatus.AVAILABLE, 10L},
                        new Object[]{BedStatus.OCCUPIED, 15L}
                ));

        when(bedRepository.countWardBedOccupancy())
                .thenReturn(Collections.singletonList(new Object[]{"w-1", "ICU", "INTENSIVE_CARE", 10L, 8L}));

        InpatientReportResponse report = reportingService.getInpatientReport();

        assertThat(report.totalAdmissions()).isEqualTo(60L);
        assertThat(report.activeAdmissions()).isEqualTo(15L);
        assertThat(report.dischargedCount()).isEqualTo(45L);
        assertThat(report.totalBeds()).isEqualTo(25L);
        assertThat(report.occupiedBeds()).isEqualTo(15L);
        assertThat(report.overallOccupancyRate()).isEqualTo(60.0); // 15/25 = 60%
        assertThat(report.wardOccupancy()).hasSize(1);
        assertThat(report.wardOccupancy().getFirst().occupancyRate()).isEqualTo(80.0); // 8/10 = 80%
    }

    @Test
    @DisplayName("getLabReport calculates order counts, turnaround time, and top requested tests")
    void getLabReport_Success() {
        LocalDate start = LocalDate.now().minusDays(14);
        LocalDate end = LocalDate.now();

        when(labOrderRepository.countLabOrdersByStatus(any(), any()))
                .thenReturn(List.of(
                        new Object[]{LabOrderStatus.COMPLETED, 40L},
                        new Object[]{LabOrderStatus.PROCESSING, 10L}
                ));

        Instant ordered = Instant.now().minusSeconds(7200); // 2 hours ago
        Instant completed = Instant.now();
        when(labOrderRepository.findCompletedLabOrderTimes(any(), any()))
                .thenReturn(Collections.singletonList(new Object[]{ordered, completed}));

        when(labOrderRepository.findTopRequestedLabTests(any(), any(), any(Pageable.class)))
                .thenReturn(Collections.singletonList(new Object[]{"test-1", "Complete Blood Count", "CBC", 30L}));

        LabReportResponse report = reportingService.getLabReport(start, end);

        assertThat(report.totalOrders()).isEqualTo(50L);
        assertThat(report.ordersByStatus()).containsEntry("COMPLETED", 40L);
        assertThat(report.averageTurnaroundHours()).isEqualTo(2.0);
        assertThat(report.topTests()).hasSize(1);
        assertThat(report.topTests().getFirst().testCode()).isEqualTo("CBC");
    }

    @Test
    @DisplayName("getPharmacyReport calculates inventory quantities, expirations, and stockout alerts")
    void getPharmacyReport_Success() {
        List<Object[]> aggResults = Collections.singletonList(
                new Object[]{120L, 5000L, 5L, 12L, 8L, 2L} // totalBatches, totalStock, expired, nearExpiry, lowStock, outOfStock
        );
        when(pharmacyRepository.getPharmacyInventoryAggregates(any(), any()))
                .thenReturn(aggResults);

        PharmacyInventoryBatch batch = new PharmacyInventoryBatch(
                "batch-1", "med-1", "BATCH-001", LocalDate.now().plusDays(10), 3, 10
        );
        when(pharmacyRepository.findCriticalStockBatches(any(Pageable.class)))
                .thenReturn(List.of(batch));

        PharmacyReportResponse report = reportingService.getPharmacyReport();

        assertThat(report.totalBatches()).isEqualTo(120L);
        assertThat(report.totalStockQuantity()).isEqualTo(5000L);
        assertThat(report.expiredBatchesCount()).isEqualTo(5L);
        assertThat(report.expiringWithin30DaysCount()).isEqualTo(12L);
        assertThat(report.lowStockBatchesCount()).isEqualTo(8L);
        assertThat(report.outOfStockBatchesCount()).isEqualTo(2L);
        assertThat(report.criticalAlerts()).hasSize(1);
        assertThat(report.criticalAlerts().getFirst().batchNumber()).isEqualTo("BATCH-001");
    }

    @Test
    @DisplayName("getFinancialReport computes billing collection rates and claim metrics")
    void getFinancialReport_Success() {
        LocalDate start = LocalDate.now().minusDays(30);
        LocalDate end = LocalDate.now();

        when(invoiceRepository.getInvoiceFinancialTotals(any(), any()))
                .thenReturn(Collections.singletonList(new Object[]{
                        new BigDecimal("100000.00"),
                        new BigDecimal("75000.00"),
                        new BigDecimal("25000.00")
                }));

        when(invoiceRepository.countInvoicesByStatus(any(), any()))
                .thenReturn(List.of(new Object[]{InvoiceStatus.PAID, 70L}, new Object[]{InvoiceStatus.ISSUED, 30L}));

        when(insuranceClaimRepository.getInsuranceClaimAggregates(any(), any()))
                .thenReturn(Collections.singletonList(new Object[]{
                        50L,
                        new BigDecimal("60000.00"),
                        new BigDecimal("48000.00"),
                        40L,
                        10L
                }));

        FinancialReportResponse report = reportingService.getFinancialReport(start, end);

        assertThat(report.totalInvoiced()).isEqualByComparingTo("100000.00");
        assertThat(report.totalPaid()).isEqualByComparingTo("75000.00");
        assertThat(report.totalOutstanding()).isEqualByComparingTo("25000.00");
        assertThat(report.collectionRate()).isEqualTo(75.0); // 75k / 100k = 75%
        assertThat(report.insuranceClaims().totalClaims()).isEqualTo(50L);
        assertThat(report.insuranceClaims().approvalRate()).isEqualTo(80.0); // 40/50 = 80%
    }

    @Test
    @DisplayName("getReportExecutionHistory returns paginated audit records")
    void getReportExecutionHistory_ReturnsHistory() {
        Pageable pageable = PageRequest.of(0, 10);
        ReportExecution execution = new ReportExecution(
                UUID.randomUUID().toString(),
                ReportType.EXECUTIVE_DASHBOARD,
                "admin",
                "today=2026-09-14",
                45L,
                "COMPLETED"
        );
        Page<ReportExecution> page = new PageImpl<>(List.of(execution), pageable, 1);
        when(reportExecutionRepository.findAllByOrderByCreatedAtDesc(pageable)).thenReturn(page);

        PageResponse<ReportExecutionResponse> history = reportingService.getReportExecutionHistory(pageable);

        assertThat(history.content()).hasSize(1);
        assertThat(history.content().getFirst().reportType()).isEqualTo(ReportType.EXECUTIVE_DASHBOARD);
    }
}
