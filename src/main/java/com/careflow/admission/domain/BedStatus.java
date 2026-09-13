package com.careflow.admission.domain;

/**
 * Physical hospital bed availability and operational states (§28).
 */
public enum BedStatus {
    AVAILABLE,
    OCCUPIED,
    RESERVED,
    MAINTENANCE
}
