package com.careflow.admission.exception;

import com.careflow.common.exception.BusinessRuleException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when an allocation or admission attempt targets a bed that is not AVAILABLE (§28, §57 Lab 4).
 */
public class BedNotAvailableException extends BusinessRuleException {

    public BedNotAvailableException(String bedId, String currentStatus) {
        super("BED_NOT_AVAILABLE",
                String.format("Bed '%s' is not available for allocation. Current status: %s", bedId, currentStatus),
                HttpStatus.CONFLICT);
    }
}
