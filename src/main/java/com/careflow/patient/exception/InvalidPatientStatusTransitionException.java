package com.careflow.patient.exception;

import com.careflow.common.exception.BusinessRuleException;
import com.careflow.patient.domain.PatientStatus;
import org.springframework.http.HttpStatus;

/**
 * Thrown when an invalid status transition is attempted on a Patient entity (§16, §69).
 */
public class InvalidPatientStatusTransitionException extends BusinessRuleException {

    public InvalidPatientStatusTransitionException(PatientStatus from, PatientStatus to) {
        super("INVALID_STATUS_TRANSITION",
              String.format("Cannot transition patient status from '%s' to '%s'.", from, to),
              HttpStatus.UNPROCESSABLE_ENTITY);
    }

    public InvalidPatientStatusTransitionException(String message) {
        super("INVALID_STATUS_TRANSITION", message, HttpStatus.UNPROCESSABLE_ENTITY);
    }
}
