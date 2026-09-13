package com.careflow.admission.exception;

import com.careflow.common.exception.BusinessRuleException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when an illegal admission state transition is attempted (§28).
 */
public class InvalidAdmissionStatusTransitionException extends BusinessRuleException {

    public InvalidAdmissionStatusTransitionException(String message) {
        super("INVALID_ADMISSION_STATUS_TRANSITION", message, HttpStatus.CONFLICT);
    }
}
