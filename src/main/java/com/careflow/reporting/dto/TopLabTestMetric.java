package com.careflow.reporting.dto;

/**
 * Metric representing laboratory diagnostic test request volumes (§37, §103 Phase 16).
 */
public record TopLabTestMetric(
        String testId,
        String testName,
        String testCode,
        long requestCount
) {
}
