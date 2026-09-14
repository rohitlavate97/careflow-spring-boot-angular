package com.careflow.reporting.dto;

import java.time.LocalDate;

/**
 * Metric representing daily appointment volume and status aggregates (§37, §103 Phase 16).
 */
public record DailyAppointmentMetric(
        LocalDate date,
        long total,
        long completed,
        long cancelled,
        long noShow
) {
}
