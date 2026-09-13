package com.careflow.consultation.domain;

/**
 * Operational lifecycle states for a patient consultation encounter (§22, §69, §103 Phase 7).
 */
public enum ConsultationStatus {
    /**
     * Consultation encounter has been initiated by the attending physician.
     */
    STARTED,

    /**
     * Consultation is actively underway; vitals, symptoms, examinations, and diagnoses are being entered.
     */
    IN_PROGRESS,

    /**
     * Consultation has been finalized and clinically concluded. Record is immutable.
     */
    COMPLETED,

    /**
     * Consultation was cancelled or abandoned before clinical completion.
     */
    CANCELLED
}
