package com.careflow.queue.exception;

import com.careflow.common.exception.BusinessRuleException;
import com.careflow.queue.domain.QueueStatus;
import org.springframework.http.HttpStatus;

/**
 * Thrown when an illegal state transition is attempted on a queue entry (§21, §69).
 */
public class InvalidQueueStatusTransitionException extends BusinessRuleException {

    public InvalidQueueStatusTransitionException(String id, QueueStatus currentStatus, QueueStatus targetStatus) {
        super("INVALID_QUEUE_TRANSITION",
                String.format("Cannot transition queue entry '%s' from status '%s' to '%s'.", id, currentStatus, targetStatus),
                HttpStatus.BAD_REQUEST);
    }

    public InvalidQueueStatusTransitionException(QueueStatus currentStatus, QueueStatus targetStatus) {
        super("INVALID_QUEUE_TRANSITION",
                String.format("Cannot transition queue entry from status '%s' to '%s'.", currentStatus, targetStatus),
                HttpStatus.BAD_REQUEST);
    }
}
