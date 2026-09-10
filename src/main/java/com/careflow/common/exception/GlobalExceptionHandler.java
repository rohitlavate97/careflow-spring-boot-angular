package com.careflow.common.exception;

import com.careflow.common.dto.ApiErrorResponse;
import com.careflow.common.dto.ValidationError;
import com.careflow.common.filter.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Centralized exception handling advice ensuring uniform API error responses (§42).
 * Prevents information leakage (stack traces, SQL details) while logging correlation IDs.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ApiErrorResponse> handleBusinessRuleException(BusinessRuleException ex,
                                                                        HttpServletRequest request) {
        log.warn("Business rule violation [code={}]: {}", ex.getErrorCode(), ex.getMessage());
        ApiErrorResponse response = buildErrorResponse(
                ex.getStatus(),
                ex.getErrorCode(),
                ex.getMessage(),
                request.getRequestURI(),
                null
        );
        return ResponseEntity.status(ex.getStatus()).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException(MethodArgumentNotValidException ex,
                                                                      HttpServletRequest request) {
        List<ValidationError> validationErrors = new ArrayList<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            validationErrors.add(new ValidationError(
                    fieldError.getField(),
                    fieldError.getRejectedValue(),
                    fieldError.getDefaultMessage()
            ));
        }

        log.warn("Request validation failed on path {}: {} errors", request.getRequestURI(), validationErrors.size());
        ApiErrorResponse response = buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_FAILED",
                "One or more request parameters failed validation.",
                request.getRequestURI(),
                validationErrors
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolationException(ConstraintViolationException ex,
                                                                               HttpServletRequest request) {
        List<ValidationError> validationErrors = new ArrayList<>();
        ex.getConstraintViolations().forEach(violation -> validationErrors.add(new ValidationError(
                violation.getPropertyPath().toString(),
                violation.getInvalidValue(),
                violation.getMessage()
        )));

        log.warn("Constraint violation on path {}: {}", request.getRequestURI(), ex.getMessage());
        ApiErrorResponse response = buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "CONSTRAINT_VIOLATION",
                "Constraint validation check failed.",
                request.getRequestURI(),
                validationErrors
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
                                                                         HttpServletRequest request) {
        log.warn("Malformed JSON request on path {}: {}", request.getRequestURI(), ex.getMessage());
        ApiErrorResponse response = buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "MALFORMED_REQUEST_BODY",
                "The incoming HTTP request body is malformed or unreadable.",
                request.getRequestURI(),
                null
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNoResourceFound(NoResourceFoundException ex,
                                                                 HttpServletRequest request) {
        log.warn("Static or endpoint resource not found: {}", request.getRequestURI());
        ApiErrorResponse response = buildErrorResponse(
                HttpStatus.NOT_FOUND,
                "RESOURCE_NOT_FOUND",
                "The requested endpoint or resource does not exist.",
                request.getRequestURI(),
                null
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDeniedException(AccessDeniedException ex,
                                                                        HttpServletRequest request) {
        log.warn("Access denied on path {}: {}", request.getRequestURI(), ex.getMessage());
        ApiErrorResponse response = buildErrorResponse(
                HttpStatus.FORBIDDEN,
                "ACCESS_DENIED",
                "You do not have permission to access this resource.",
                request.getRequestURI(),
                null
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthenticationException(AuthenticationException ex,
                                                                          HttpServletRequest request) {
        log.warn("Authentication failed on path {}: {}", request.getRequestURI(), ex.getMessage());
        ApiErrorResponse response = buildErrorResponse(
                HttpStatus.UNAUTHORIZED,
                "AUTHENTICATION_REQUIRED",
                "Authentication is required to access this resource.",
                request.getRequestURI(),
                null
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneralException(Exception ex,
                                                                   HttpServletRequest request) {
        String correlationId = resolveCorrelationId();
        log.error("Unhandled internal server error [correlationId={}]: {}", correlationId, ex.getMessage(), ex);
        ApiErrorResponse response = buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_SERVER_ERROR",
                "An unexpected internal error occurred. Please contact support quoting your correlation ID.",
                request.getRequestURI(),
                null
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    private ApiErrorResponse buildErrorResponse(HttpStatus status,
                                                String code,
                                                String message,
                                                String path,
                                                List<ValidationError> validationErrors) {
        return ApiErrorResponse.builder()
                .timestamp(Instant.now())
                .status(status.value())
                .code(code)
                .message(message)
                .path(path)
                .correlationId(resolveCorrelationId())
                .validationErrors(validationErrors)
                .build();
    }

    private String resolveCorrelationId() {
        String correlationId = MDC.get(CorrelationIdFilter.MDC_CORRELATION_ID_KEY);
        return (correlationId != null && !correlationId.isBlank()) ? correlationId : UUID.randomUUID().toString();
    }
}
