package com.careflow.pharmacy.exception;

import com.careflow.common.exception.BusinessRuleException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when an inventory batch has insufficient stock for dispensing (§25, §26).
 */
public class InsufficientInventoryException extends BusinessRuleException {

    public InsufficientInventoryException(String batchId, int requested, int available) {
        super("INSUFFICIENT_INVENTORY",
                String.format("Cannot dispense %d units from batch '%s'; only %d units available.", requested, batchId, available),
                HttpStatus.CONFLICT);
    }

    public InsufficientInventoryException(String message) {
        super("INSUFFICIENT_INVENTORY", message, HttpStatus.CONFLICT);
    }
}
