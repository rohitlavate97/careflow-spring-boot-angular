package com.careflow.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

/**
 * Enterprise standardized API error contract (§42).
 * Strictly enforced for all HTTP error responses across all modules.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiErrorResponse {
    private final Instant timestamp;
    private final int status;
    private final String code;
    private final String message;
    private final String path;
    private final String correlationId;
    private final List<ValidationError> validationErrors;

    private ApiErrorResponse(Builder builder) {
        this.timestamp = builder.timestamp;
        this.status = builder.status;
        this.code = builder.code;
        this.message = builder.message;
        this.path = builder.path;
        this.correlationId = builder.correlationId;
        this.validationErrors = builder.validationErrors;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public int getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public String getPath() {
        return path;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public List<ValidationError> getValidationErrors() {
        return validationErrors;
    }

    public static class Builder {
        private Instant timestamp = Instant.now();
        private int status;
        private String code;
        private String message;
        private String path;
        private String correlationId;
        private List<ValidationError> validationErrors;

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder status(int status) {
            this.status = status;
            return this;
        }

        public Builder code(String code) {
            this.code = code;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder path(String path) {
            this.path = path;
            return this;
        }

        public Builder correlationId(String correlationId) {
            this.correlationId = correlationId;
            return this;
        }

        public Builder validationErrors(List<ValidationError> validationErrors) {
            this.validationErrors = validationErrors;
            return this;
        }

        public ApiErrorResponse build() {
            return new ApiErrorResponse(this);
        }
    }
}
