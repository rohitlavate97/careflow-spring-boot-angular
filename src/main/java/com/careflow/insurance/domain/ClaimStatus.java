package com.careflow.insurance.domain;

/**
 * Lifecycle status of an insurance reimbursement claim (§33).
 */
public enum ClaimStatus {
    DRAFT,
    SUBMITTED,
    UNDER_REVIEW,
    APPROVED,
    PARTIALLY_APPROVED,
    REJECTED,
    SETTLED
}
