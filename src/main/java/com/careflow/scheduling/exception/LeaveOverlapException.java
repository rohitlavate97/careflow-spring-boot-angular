package com.careflow.scheduling.exception;

import com.careflow.common.exception.BusinessRuleException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when a doctor leave request overlaps with an existing leave request (§18, §92).
 */
public class LeaveOverlapException extends BusinessRuleException {

    public LeaveOverlapException(String message) {
        super("LEAVE_OVERLAP", message, HttpStatus.CONFLICT);
    }
}
