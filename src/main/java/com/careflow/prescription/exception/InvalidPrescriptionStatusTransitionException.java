package com.careflow.prescription.exception;

import com.careflow.common.exception.BusinessRuleException;
import com.careflow.prescription.domain.PrescriptionStatus;
import org.springframework.http.HttpStatus;

/**
 * Thrown when an illegal lifecycle transition is attempted on a prescription aggregate (§24, §69).
 */
public class InvalidPrescriptionStatusTransitionException extends BusinessRuleException {

    public InvalidPrescriptionStatusTransitionException(String id, PrescriptionStatus currentStatus, PrescriptionStatus targetStatus) {
        super("INVALID_PRESCRIPTION_TRANSITION",
                String.format("Cannot transition prescription '%s' from status '%s' to '%s'.", id, currentStatus, targetStatus),
                HttpStatus.BAD_REQUEST);
    }

    public InvalidPrescriptionStatusTransitionException(String message) {
        super("INVALID_PRESCRIPTION_TRANSITION", message, HttpStatus.BAD_REQUEST);
    }
}
