package com.careflow.appointment.exception;

import com.careflow.common.exception.BusinessRuleException;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

/**
 * Thrown when a double-booking conflict occurs concurrently or during reservation (§20, §92).
 */
public class DoubleBookingException extends BusinessRuleException {

    public DoubleBookingException(String doctorId, LocalDateTime appointmentDateTime) {
        super("DOUBLE_BOOKING_CONFLICT",
                String.format("The appointment slot with doctor '%s' at '%s' is already booked by another patient.",
                        doctorId, appointmentDateTime),
                HttpStatus.CONFLICT);
    }
}
