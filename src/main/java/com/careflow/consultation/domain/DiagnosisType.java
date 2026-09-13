package com.careflow.consultation.domain;

/**
 * Categorization of clinical diagnosis entered during a consultation encounter (§22).
 */
public enum DiagnosisType {
    /**
     * The primary medical condition established after clinical evaluation.
     */
    PRIMARY,

    /**
     * Co-existing or secondary medical conditions.
     */
    SECONDARY,

    /**
     * Working or tentative diagnosis awaiting further diagnostic confirmation.
     */
    PROVISIONAL,

    /**
     * Potential alternate condition under diagnostic consideration.
     */
    DIFFERENTIAL
}
