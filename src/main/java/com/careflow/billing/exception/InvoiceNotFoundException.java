package com.careflow.billing.exception;

import com.careflow.common.exception.ResourceNotFoundException;

/**
 * Thrown when an invoice is not found in the database.
 */
public class InvoiceNotFoundException extends ResourceNotFoundException {

    public InvoiceNotFoundException(String id) {
        super("Invoice", id);
    }
}
