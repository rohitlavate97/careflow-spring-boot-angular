package com.careflow.insurance.exception;

import com.careflow.common.exception.BusinessRuleException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when an operation or claim is attempted against an expired or inactive policy (§33).
 */
public class PolicyCoverageExpiredException extends BusinessRuleException {

    public PolicyCoverageExpiredException(String message) {
        super("POLICY_COVERAGE_EXPIRED", message, HttpStatus.BAD_REQUEST);
    }
}
