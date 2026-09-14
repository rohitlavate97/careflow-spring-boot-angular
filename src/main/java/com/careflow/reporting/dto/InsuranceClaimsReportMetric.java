package com.careflow.reporting.dto;

import java.math.BigDecimal;

/**
 * Metric summarizing insurance claim volume, financial exposure, and approval rates (§37, §103 Phase 16).
 */
public record InsuranceClaimsReportMetric(
        long totalClaims,
        BigDecimal totalClaimedAmount,
        BigDecimal totalApprovedAmount,
        long approvedCount,
        long rejectedCount,
        double approvalRate
) {
}
