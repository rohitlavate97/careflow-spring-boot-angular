package com.careflow.billing.exception;

import com.careflow.common.exception.BusinessRuleException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when a payment amount exceeds the invoice's remaining balance due (§31, §57 Lab 5).
 */
public class OverpaymentException extends BusinessRuleException {

    public OverpaymentException(String message) {
        super("OVERPAYMENT_NOT_ALLOWED", message, HttpStatus.BAD_REQUEST);
    }
}
