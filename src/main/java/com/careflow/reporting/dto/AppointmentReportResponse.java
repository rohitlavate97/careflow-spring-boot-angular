package com.careflow.reporting.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Report payload for appointment operational analytics and doctor utilization (§37, §103 Phase 16).
 */
public record AppointmentReportResponse(
        LocalDate startDate,
        LocalDate endDate,
        long totalAppointments,
        long completedAppointments,
        long cancelledAppointments,
        long noShowAppointments,
        double cancellationRate,
        double noShowRate,
        List<DailyAppointmentMetric> dailyTrend,
        List<DepartmentVolumeMetric> departmentBreakdown,
        List<DoctorUtilizationMetric> doctorUtilization
) {
}
