package com.careflow.scheduling.domain;

/**
 * Lifecycle status of a doctor leave request (§18, §69).
 */
public enum LeaveStatus {
    PENDING,
    APPROVED,
    REJECTED,
    CANCELLED;

    public boolean canTransitionTo(LeaveStatus target) {
        if (this == target) {
            return true;
        }
        return switch (this) {
            case PENDING -> target == APPROVED || target == REJECTED || target == CANCELLED;
            case APPROVED -> target == CANCELLED;
            default -> false; // REJECTED and CANCELLED cannot transition
        };
    }
}
