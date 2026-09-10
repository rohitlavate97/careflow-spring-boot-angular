package com.careflow.common.controller;

import com.careflow.common.dto.SystemPingResponse;
import com.careflow.common.filter.CorrelationIdFilter;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/**
 * Diagnostic and smoke-test controller for platform initialization verification.
 */
@RestController
@RequestMapping("/api/v1/system")
public class SystemPingController {

    @GetMapping("/ping")
    public ResponseEntity<SystemPingResponse> ping() {
        String correlationId = MDC.get(CorrelationIdFilter.MDC_CORRELATION_ID_KEY);
        SystemPingResponse response = new SystemPingResponse(
                "UP",
                "CareFlow Enterprise Platform",
                Instant.now(),
                correlationId
        );
        return ResponseEntity.ok(response);
    }
}
