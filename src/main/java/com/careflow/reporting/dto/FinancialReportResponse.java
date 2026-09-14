package com.careflow.reporting.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * Report payload for hospital revenue cycle, collections, and payer claims (§37, §103 Phase 16).
 */
public record FinancialReportResponse(
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal totalInvoiced,
        BigDecimal totalPaid,
        BigDecimal totalOutstanding,
        double collectionRate,
        Map<String, Long> invoicesByStatus,
        InsuranceClaimsReportMetric insuranceClaims
) {
}
