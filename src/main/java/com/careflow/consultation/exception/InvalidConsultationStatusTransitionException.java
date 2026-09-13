package com.careflow.consultation.exception;

import com.careflow.common.exception.BusinessRuleException;
import com.careflow.consultation.domain.ConsultationStatus;
import org.springframework.http.HttpStatus;

/**
 * Thrown when an illegal state transition is attempted on a consultation encounter (§22, §69).
 */
public class InvalidConsultationStatusTransitionException extends BusinessRuleException {

    public InvalidConsultationStatusTransitionException(String id, ConsultationStatus currentStatus, ConsultationStatus targetStatus) {
        super("INVALID_CONSULTATION_TRANSITION",
                String.format("Cannot transition consultation '%s' from status '%s' to '%s'.", id, currentStatus, targetStatus),
                HttpStatus.BAD_REQUEST);
    }

    public InvalidConsultationStatusTransitionException(String message) {
        super("INVALID_CONSULTATION_TRANSITION", message, HttpStatus.BAD_REQUEST);
    }
}
