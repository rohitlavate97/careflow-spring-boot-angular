package com.careflow.prescription.domain;

/**
 * Operational lifecycle states for a prescription (§24, §69, §103 Phase 8).
 */
public enum PrescriptionStatus {
    /**
     * Prescription is in draft mode and can be freely modified by the prescribing physician.
     */
    DRAFT,

    /**
     * Prescription has been signed and issued to the pharmacy department for dispensing.
     */
    PENDING_DISPENSE,

    /**
     * A subset of prescribed items has been dispensed; remaining items are pending.
     */
    PARTIALLY_DISPENSED,

    /**
     * All prescribed items have been fully dispensed and verified by the pharmacist.
     */
    DISPENSED,

    /**
     * Prescription was cancelled before complete fulfillment.
     */
    CANCELLED
}
