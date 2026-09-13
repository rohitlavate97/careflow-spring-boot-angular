package com.careflow.scheduling.exception;

import com.careflow.common.exception.BusinessRuleException;
import com.careflow.scheduling.domain.LeaveStatus;
import org.springframework.http.HttpStatus;

/**
 * Thrown when an illegal state transition is attempted on a doctor leave request (§18, §69).
 */
public class InvalidLeaveStatusTransitionException extends BusinessRuleException {

    public InvalidLeaveStatusTransitionException(LeaveStatus currentStatus, LeaveStatus targetStatus) {
        super("INVALID_LEAVE_STATUS_TRANSITION",
                String.format("Cannot transition leave request from status '%s' to '%s'.", currentStatus, targetStatus),
                HttpStatus.BAD_REQUEST);
    }
}
