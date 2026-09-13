package com.careflow.laboratory.domain;

/**
 * Diagnostic lab order lifecycle state machine (§27).
 */
public enum LabOrderStatus {
    ORDERED,
    SAMPLE_COLLECTED,
    PROCESSING,
    COMPLETED,
    CANCELLED
}
