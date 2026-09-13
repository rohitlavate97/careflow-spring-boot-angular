package com.careflow.billing.exception;

import com.careflow.common.exception.ResourceNotFoundException;

/**
 * Thrown when a payment transaction is not found in the database.
 */
public class PaymentNotFoundException extends ResourceNotFoundException {

    public PaymentNotFoundException(String id) {
        super("Payment", id);
    }
}
