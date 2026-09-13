package com.careflow.laboratory.exception;

import com.careflow.common.exception.BusinessRuleException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when a diagnostic test definition is not found in the catalog.
 */
public class LabTestNotFoundException extends BusinessRuleException {

    public LabTestNotFoundException(String testId) {
        super("LAB_TEST_NOT_FOUND", "Lab test catalog definition not found: " + testId, HttpStatus.NOT_FOUND);
    }
}
