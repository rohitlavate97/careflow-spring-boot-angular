package com.careflow.appointment.exception;

import com.careflow.appointment.domain.AppointmentStatus;
import com.careflow.common.exception.BusinessRuleException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when an illegal state transition is attempted on an appointment (§19, §69).
 */
public class InvalidAppointmentStatusTransitionException extends BusinessRuleException {

    public InvalidAppointmentStatusTransitionException(AppointmentStatus currentStatus, AppointmentStatus targetStatus) {
        super("INVALID_APPOINTMENT_TRANSITION",
                String.format("Cannot transition appointment from status '%s' to '%s'.", currentStatus, targetStatus),
                HttpStatus.BAD_REQUEST);
    }
}
