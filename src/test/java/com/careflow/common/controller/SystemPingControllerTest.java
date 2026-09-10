package com.careflow.common.controller;

import com.careflow.common.config.SecurityConfig;
import com.careflow.common.filter.CorrelationIdFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SystemPingController.class)
@Import({SecurityConfig.class, CorrelationIdFilter.class})
class SystemPingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /api/v1/system/ping returns 200 OK and propagates correlation ID")
    void ping_shouldReturn200AndCorrelationId() throws Exception {
        String testCorrelationId = "test-ping-trace-999";

        mockMvc.perform(get("/api/v1/system/ping")
                        .header(CorrelationIdFilter.CORRELATION_ID_HEADER, testCorrelationId))
                .andExpect(status().isOk())
                .andExpect(header().string(CorrelationIdFilter.CORRELATION_ID_HEADER, testCorrelationId))
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("CareFlow Enterprise Platform"))
                .andExpect(jsonPath("$.correlationId").value(testCorrelationId))
                .andExpect(jsonPath("$.timestamp").exists());
    }
}
