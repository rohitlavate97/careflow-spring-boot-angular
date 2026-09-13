package com.careflow.queue.exception;

import com.careflow.common.exception.BusinessRuleException;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;

/**
 * Thrown when attempting to call the next patient from an empty queue (§21).
 */
public class QueueEmptyException extends BusinessRuleException {

    public QueueEmptyException(String departmentId, LocalDate date) {
        super("QUEUE_EMPTY",
                String.format("No waiting patients currently in queue for department '%s' on %s.", departmentId, date),
                HttpStatus.NOT_FOUND);
    }
}
