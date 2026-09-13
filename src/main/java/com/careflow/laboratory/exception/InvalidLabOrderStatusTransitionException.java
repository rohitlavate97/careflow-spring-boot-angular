package com.careflow.laboratory.exception;

import com.careflow.common.exception.BusinessRuleException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when an illegal lab order status transition is attempted (§27).
 */
public class InvalidLabOrderStatusTransitionException extends BusinessRuleException {

    public InvalidLabOrderStatusTransitionException(String message) {
        super("INVALID_LAB_ORDER_STATUS_TRANSITION", message, HttpStatus.CONFLICT);
    }
}
