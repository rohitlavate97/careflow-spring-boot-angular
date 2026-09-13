package com.careflow.insurance.exception;

import com.careflow.common.exception.BusinessRuleException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when an illegal lifecycle state transition is attempted on an insurance claim (§33).
 */
public class InvalidClaimStatusTransitionException extends BusinessRuleException {

    public InvalidClaimStatusTransitionException(String message) {
        super("INVALID_CLAIM_STATUS_TRANSITION", message, HttpStatus.BAD_REQUEST);
    }
}
