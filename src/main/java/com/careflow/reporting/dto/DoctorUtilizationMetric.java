package com.careflow.reporting.dto;

/**
 * Metric representing physician schedule utilization and encounter volume (§37, §103 Phase 16).
 */
public record DoctorUtilizationMetric(
        String doctorId,
        String doctorName,
        String departmentName,
        long totalAppointments,
        long completedAppointments,
        long cancelledAppointments,
        double utilizationRate
) {
}
