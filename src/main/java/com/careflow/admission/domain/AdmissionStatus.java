package com.careflow.admission.domain;

/**
 * Inpatient admission clinical journey state machine (§28).
 */
public enum AdmissionStatus {
    ADMITTED,
    TRANSFERRED,
    DISCHARGED,
    CANCELLED
}
