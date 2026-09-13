package com.careflow.queue.domain;

/**
 * Priority levels for patient triage in the queue (§21).
 * <pre>
 * EMERGENCY (1) -> Immediate attention / bypass normal wait
 * URGENT    (2) -> Accelerated consultation priority
 * NORMAL    (3) -> Standard outpatient first-come-first-served order
 * </pre>
 */
public enum QueuePriority {

    EMERGENCY(1),
    URGENT(2),
    NORMAL(3);

    private final int rank;

    QueuePriority(int rank) {
        this.rank = rank;
    }

    public int getRank() {
        return rank;
    }
}
