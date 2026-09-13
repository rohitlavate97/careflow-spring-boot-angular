package com.careflow.laboratory.exception;

import com.careflow.common.exception.BusinessRuleException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when a laboratory specimen is not found.
 */
public class LabSampleNotFoundException extends BusinessRuleException {

    public LabSampleNotFoundException(String identifier) {
        super("LAB_SAMPLE_NOT_FOUND", "Lab specimen not found: " + identifier, HttpStatus.NOT_FOUND);
    }
}
