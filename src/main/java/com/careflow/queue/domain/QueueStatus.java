package com.careflow.queue.domain;

import java.util.Set;

/**
 * Lifecycle states of a patient in the outpatient queue (§21).
 * <pre>
 * REGISTERED -> WAITING -> CALLED -> IN_CONSULTATION -> COMPLETED
 *                  |         |
 *                  |         +-> SKIPPED -> WAITING
 *                  |                  \---> CANCELLED
 *                  +------------> CANCELLED
 * </pre>
 */
public enum QueueStatus {

    /** Patient is registered/checked-in but awaiting placement into active waiting pool. */
    REGISTERED,

    /** Patient is actively waiting in the waiting area for their turn. */
    WAITING,

    /** Patient has been called to a consultation room. */
    CALLED,

    /** Patient is currently inside the consultation room with the clinician. */
    IN_CONSULTATION,

    /** Patient consultation is finished (terminal state). */
    COMPLETED,

    /** Patient was called but did not respond / was absent. Can be re-queued to WAITING. */
    SKIPPED,

    /** Queue entry cancelled (patient left or canceled, terminal state). */
    CANCELLED;

    /**
     * Determines whether transitioning from this status to the target status is valid (§21).
     */
    public boolean canTransitionTo(QueueStatus target) {
        if (this == target) {
            return false;
        }
        return switch (this) {
            case REGISTERED -> target == WAITING || target == CANCELLED;
            case WAITING -> target == CALLED || target == CANCELLED;
            case CALLED -> target == IN_CONSULTATION || target == SKIPPED || target == CANCELLED;
            case IN_CONSULTATION -> target == COMPLETED || target == CANCELLED;
            case SKIPPED -> target == WAITING || target == CANCELLED;
            case COMPLETED, CANCELLED -> false; // Terminal states
        };
    }

    /**
     * Whether the queue entry is actively waiting in the queue to be called.
     */
    public boolean isWaiting() {
        return this == WAITING;
    }

    /**
     * Whether the queue entry is in an active non-terminal state.
     */
    public boolean isActive() {
        return this != COMPLETED && this != CANCELLED;
    }
}
