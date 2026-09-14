package com.careflow.reporting.dto;

/**
 * Metric representing clinical department appointment volume (§37, §103 Phase 16).
 */
public record DepartmentVolumeMetric(
        String departmentId,
        String departmentName,
        long appointmentCount
) {
}
