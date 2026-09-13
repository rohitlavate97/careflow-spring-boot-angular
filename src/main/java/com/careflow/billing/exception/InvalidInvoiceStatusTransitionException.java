package com.careflow.billing.exception;

import com.careflow.common.exception.BusinessRuleException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when an illegal state machine transition is attempted on an invoice (§29).
 */
public class InvalidInvoiceStatusTransitionException extends BusinessRuleException {

    public InvalidInvoiceStatusTransitionException(String message) {
        super("INVALID_INVOICE_STATUS_TRANSITION", message, HttpStatus.BAD_REQUEST);
    }
}
