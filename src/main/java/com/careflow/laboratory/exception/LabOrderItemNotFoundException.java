package com.careflow.laboratory.exception;

import com.careflow.common.exception.BusinessRuleException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when a laboratory requisition item is not found.
 */
public class LabOrderItemNotFoundException extends BusinessRuleException {

    public LabOrderItemNotFoundException(String itemId) {
        super("LAB_ORDER_ITEM_NOT_FOUND", "Lab order item not found: " + itemId, HttpStatus.NOT_FOUND);
    }
}
