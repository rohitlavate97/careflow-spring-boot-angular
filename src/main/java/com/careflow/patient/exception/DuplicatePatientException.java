package com.careflow.patient.exception;

import com.careflow.common.exception.BusinessRuleException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when an operation violates patient uniqueness constraints (MRN, etc.).
 */
public class DuplicatePatientException extends BusinessRuleException {

    public DuplicatePatientException(String message) {
        super("DUPLICATE_PATIENT", message, HttpStatus.CONFLICT);
    }
}
