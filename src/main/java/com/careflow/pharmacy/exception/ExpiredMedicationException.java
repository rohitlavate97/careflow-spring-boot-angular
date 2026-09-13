package com.careflow.pharmacy.exception;

import com.careflow.common.exception.BusinessRuleException;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;

/**
 * Thrown when an inventory batch has expired and cannot legally be dispensed (§25).
 */
public class ExpiredMedicationException extends BusinessRuleException {

    public ExpiredMedicationException(String batchNumber, LocalDate expiryDate) {
        super("MEDICATION_EXPIRED",
                String.format("Inventory batch '%s' expired on '%s' and cannot be dispensed.", batchNumber, expiryDate),
                HttpStatus.BAD_REQUEST);
    }
}
