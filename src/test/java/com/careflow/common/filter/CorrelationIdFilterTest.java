package com.careflow.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CorrelationIdFilterTest {

    private CorrelationIdFilter filter;

    @BeforeEach
    void setUp() {
        filter = new CorrelationIdFilter();
        MDC.clear();
    }

    @Test
    @DisplayName("doFilter should generate UUID correlation ID when X-Correlation-ID header is missing")
    void doFilter_shouldGenerateCorrelationId_whenHeaderMissing() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = (req, res) -> {
            String mdcValue = MDC.get(CorrelationIdFilter.MDC_CORRELATION_ID_KEY);
            assertThat(mdcValue).isNotNull().isNotBlank();
        };

        filter.doFilter(request, response, chain);

        String headerInResponse = response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER);
        assertThat(headerInResponse).isNotNull().isNotBlank();
        // MDC must be cleared after request execution
        assertThat(MDC.get(CorrelationIdFilter.MDC_CORRELATION_ID_KEY)).isNull();
    }

    @Test
    @DisplayName("doFilter should preserve client correlation ID when X-Correlation-ID header is supplied")
    void doFilter_shouldPreserveClientCorrelationId_whenHeaderPresent() throws ServletException, IOException {
        String clientCorrelationId = "custom-client-trace-12345";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, clientCorrelationId);
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = (req, res) -> {
            String mdcValue = MDC.get(CorrelationIdFilter.MDC_CORRELATION_ID_KEY);
            assertThat(mdcValue).isEqualTo(clientCorrelationId);
        };

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER)).isEqualTo(clientCorrelationId);
        assertThat(MDC.get(CorrelationIdFilter.MDC_CORRELATION_ID_KEY)).isNull();
    }

    @Test
    @DisplayName("doFilter should ensure MDC cleanup even when downstream filter throws an exception")
    void doFilter_shouldCleanUpMdc_whenExceptionOccurs() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = (req, res) -> {
            throw new RuntimeException("Downstream filter simulation failure");
        };

        assertThatThrownBy(() -> filter.doFilter(request, response, chain))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Downstream filter simulation failure");

        // MDC must still be completely cleaned up
        assertThat(MDC.get(CorrelationIdFilter.MDC_CORRELATION_ID_KEY)).isNull();
    }
}
