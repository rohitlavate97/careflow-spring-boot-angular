package com.careflow.appointment.domain;

import java.util.EnumSet;
import java.util.Set;

/**
 * Lifecycle states of an appointment with guarded state transitions (§19, §69).
 */
public enum AppointmentStatus {
    REQUESTED,
    CONFIRMED,
    CHECKED_IN,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED,
    NO_SHOW;

    private static final Set<AppointmentStatus> TERMINAL_STATES = EnumSet.of(COMPLETED, CANCELLED, NO_SHOW);

    /**
     * Determines whether an appointment in this status holds an active doctor slot reservation (§20).
     */
    public boolean isActive() {
        return this != CANCELLED && this != NO_SHOW;
    }

    /**
     * Verifies if a transition to the target state is allowed by hospital business rules (§19).
     */
    public boolean canTransitionTo(AppointmentStatus target) {
        if (this == target) {
            return true;
        }
        if (TERMINAL_STATES.contains(this)) {
            return false;
        }
        return switch (this) {
            case REQUESTED -> target == CONFIRMED || target == CANCELLED;
            case CONFIRMED -> target == CHECKED_IN || target == CANCELLED || target == NO_SHOW;
            case CHECKED_IN -> target == IN_PROGRESS || target == CANCELLED;
            case IN_PROGRESS -> target == COMPLETED;
            default -> false;
        };
    }
}
