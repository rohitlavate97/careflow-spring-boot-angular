package com.careflow.consultation.exception;

import com.careflow.common.exception.BusinessRuleException;
import com.careflow.consultation.domain.ConsultationStatus;
import org.springframework.http.HttpStatus;

/**
 * Thrown when an update is attempted on a consultation that has been finalized or cancelled (§22, §23).
 */
public class ConsultationLockedException extends BusinessRuleException {

    public ConsultationLockedException(String id, ConsultationStatus status) {
        super("CONSULTATION_LOCKED",
                String.format("Consultation encounter '%s' is locked in status '%s' and cannot be modified.", id, status),
                HttpStatus.CONFLICT);
    }
}
