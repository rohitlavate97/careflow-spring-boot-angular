package com.careflow.patient.service;

/**
 * Strategy contract for generating unique Medical Record Numbers (§16).
 */
public interface MrnGenerator {

    /**
     * Generates a unique, non-colliding Medical Record Number.
     *
     * @return Generated MRN
     */
    String generateUniqueMrn();
}
