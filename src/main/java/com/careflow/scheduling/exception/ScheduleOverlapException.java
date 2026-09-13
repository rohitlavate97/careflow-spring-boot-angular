package com.careflow.scheduling.exception;

import com.careflow.common.exception.BusinessRuleException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when a doctor schedule template overlaps with an existing schedule for the same day (§18, §92).
 */
public class ScheduleOverlapException extends BusinessRuleException {

    public ScheduleOverlapException(String message) {
        super("SCHEDULE_OVERLAP", message, HttpStatus.CONFLICT);
    }
}
