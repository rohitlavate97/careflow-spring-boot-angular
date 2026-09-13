package com.careflow.staff.exception;

import com.careflow.common.exception.BusinessRuleException;
import com.careflow.staff.domain.StaffStatus;
import org.springframework.http.HttpStatus;

/**
 * Thrown when an illegal state transition is attempted on a staff member (§17, §69).
 */
public class InvalidStaffStatusTransitionException extends BusinessRuleException {

    public InvalidStaffStatusTransitionException(StaffStatus currentStatus, StaffStatus targetStatus) {
        super("INVALID_STAFF_STATUS_TRANSITION",
                String.format("Cannot transition staff member from status '%s' to '%s'.", currentStatus, targetStatus),
                HttpStatus.BAD_REQUEST);
    }
}
