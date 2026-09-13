package com.careflow.staff.domain;

import java.util.EnumSet;
import java.util.Set;

/**
 * Lifecycle status of a hospital staff member (§17, §69).
 */
public enum StaffStatus {
    ACTIVE,
    ON_LEAVE,
    SUSPENDED,
    TERMINATED,
    RESIGNED;

    /**
     * Set of terminal states from which no further transitions are allowed.
     */
    private static final Set<StaffStatus> TERMINAL_STATES = EnumSet.of(TERMINATED, RESIGNED);

    /**
     * Determines whether transitioning from this status to the target status is allowed (§69).
     *
     * @param target the desired next status
     * @return true if the transition is valid, false otherwise
     */
    public boolean canTransitionTo(StaffStatus target) {
        if (this == target) {
            return true;
        }
        if (TERMINAL_STATES.contains(this)) {
            return false;
        }
        return switch (this) {
            case ACTIVE -> target == ON_LEAVE || target == SUSPENDED || target == TERMINATED || target == RESIGNED;
            case ON_LEAVE -> target == ACTIVE || target == TERMINATED || target == RESIGNED;
            case SUSPENDED -> target == ACTIVE || target == TERMINATED || target == RESIGNED;
            default -> false;
        };
    }
}
