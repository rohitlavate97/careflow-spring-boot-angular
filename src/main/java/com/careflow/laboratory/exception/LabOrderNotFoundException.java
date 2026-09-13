package com.careflow.laboratory.exception;

import com.careflow.common.exception.BusinessRuleException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when a requested lab order cannot be located.
 */
public class LabOrderNotFoundException extends BusinessRuleException {

    public LabOrderNotFoundException(String orderId) {
        super("LAB_ORDER_NOT_FOUND", "Lab order not found with identifier: " + orderId, HttpStatus.NOT_FOUND);
    }
}
