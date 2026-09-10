package com.careflow.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Base exception for domain and business rule violations across all CareFlow modules.
 */
public class BusinessRuleException extends RuntimeException {

    private final String errorCode;
    private final HttpStatus status;

    public BusinessRuleException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.status = HttpStatus.BAD_REQUEST;
    }

    public BusinessRuleException(String errorCode, String message, HttpStatus status) {
        super(message);
        this.errorCode = errorCode;
        this.status = status;
    }

    public BusinessRuleException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.status = HttpStatus.BAD_REQUEST;
    }

    public BusinessRuleException(String errorCode, String message, HttpStatus status, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.status = status;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
