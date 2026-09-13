package com.careflow.laboratory.domain;

/**
 * Physician review and sign-off status for completed laboratory requisitions (§27).
 */
public enum LabReviewStatus {
    PENDING_REVIEW,
    REVIEWED,
    AMENDED
}
