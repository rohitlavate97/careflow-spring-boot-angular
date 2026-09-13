package com.careflow.prescription.domain;

/**
 * Fulfillment status for individual medication line items on a prescription (§24).
 */
public enum PrescriptionItemStatus {
    /**
     * Item awaiting pharmacy dispensing.
     */
    PENDING,

    /**
     * Item has been dispensed to the patient.
     */
    DISPENSED,

    /**
     * Item was cancelled or omitted.
     */
    CANCELLED
}
