package com.careflow.scheduling.exception;

import com.careflow.common.exception.BusinessRuleException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when schedule start, end, or break times violate temporal constraints (§18).
 */
public class InvalidScheduleTimeException extends BusinessRuleException {

    public InvalidScheduleTimeException(String message) {
        super("INVALID_SCHEDULE_TIME", message, HttpStatus.BAD_REQUEST);
    }
}
