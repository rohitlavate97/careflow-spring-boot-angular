package com.careflow.common.exception;

import com.careflow.common.dto.ApiErrorResponse;
import com.careflow.common.filter.CorrelationIdFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/test-endpoint");
        MDC.put(CorrelationIdFilter.MDC_CORRELATION_ID_KEY, "test-correlation-uuid");
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    @DisplayName("handleBusinessRuleException should return standardized 400 response with domain error code")
    void handleBusinessRuleException_shouldReturnCustomStatusAndCode() {
        BusinessRuleException ex = new BusinessRuleException("INVALID_APPOINTMENT_STATE", "Cannot cancel an already completed appointment.");

        ResponseEntity<ApiErrorResponse> response = handler.handleBusinessRuleException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("INVALID_APPOINTMENT_STATE");
        assertThat(response.getBody().getMessage()).isEqualTo("Cannot cancel an already completed appointment.");
        assertThat(response.getBody().getPath()).isEqualTo("/api/v1/test-endpoint");
        assertThat(response.getBody().getCorrelationId()).isEqualTo("test-correlation-uuid");
    }

    @Test
    @DisplayName("handleResourceNotFoundException should return standardized 404 response")
    void handleResourceNotFoundException_shouldReturn404() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Patient", "PAT-10023");

        ResponseEntity<ApiErrorResponse> response = handler.handleBusinessRuleException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("RESOURCE_NOT_FOUND");
        assertThat(response.getBody().getMessage()).contains("Patient with identifier 'PAT-10023' was not found.");
        assertThat(response.getBody().getStatus()).isEqualTo(404);
    }

    @Test
    @DisplayName("handleGeneralException should return 500 without leaking stack trace or internal details")
    void handleGeneralException_shouldMaskDetailsAndReturn500WithCorrelationId() {
        Exception internalEx = new NullPointerException("Database connection dropped unexpectedly at line 42");

        ResponseEntity<ApiErrorResponse> response = handler.handleGeneralException(internalEx, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("INTERNAL_SERVER_ERROR");
        assertThat(response.getBody().getMessage()).doesNotContain("NullPointerException");
        assertThat(response.getBody().getMessage()).doesNotContain("line 42");
        assertThat(response.getBody().getMessage()).contains("An unexpected internal error occurred");
        assertThat(response.getBody().getCorrelationId()).isEqualTo("test-correlation-uuid");
    }
}
