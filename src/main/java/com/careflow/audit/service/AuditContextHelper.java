package com.careflow.audit.service;

import com.careflow.common.filter.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

/**
 * Context helper for transparently extracting actor, client IP, and correlation identifiers (§36, §74).
 */
@Component
public class AuditContextHelper {

    private static final String DEFAULT_ACTOR = "SYSTEM";
    private static final String DEFAULT_IP = "127.0.0.1";

    public String resolveActorUserId(String explicitActor) {
        if (StringUtils.hasText(explicitActor)) {
            return explicitActor.trim();
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            String name = auth.getName();
            if (StringUtils.hasText(name)) {
                return name.trim();
            }
        }

        return DEFAULT_ACTOR;
    }

    public String resolveIpAddress(String explicitIp) {
        if (StringUtils.hasText(explicitIp)) {
            return explicitIp.trim();
        }

        try {
            if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
                HttpServletRequest request = attributes.getRequest();
                String xForwardedFor = request.getHeader("X-Forwarded-For");
                if (StringUtils.hasText(xForwardedFor)) {
                    String[] ips = xForwardedFor.split(",");
                    if (ips.length > 0 && StringUtils.hasText(ips[0])) {
                        return ips[0].trim();
                    }
                }
                String remoteAddr = request.getRemoteAddr();
                if (StringUtils.hasText(remoteAddr)) {
                    return remoteAddr.trim();
                }
            }
        } catch (Exception ignored) {
            // Ambient web request context might not be available in asynchronous or test threads
        }

        return DEFAULT_IP;
    }

    public String resolveCorrelationId(String explicitCorrelationId) {
        if (StringUtils.hasText(explicitCorrelationId)) {
            return explicitCorrelationId.trim();
        }

        String mdcCorrelationId = MDC.get(CorrelationIdFilter.MDC_CORRELATION_ID_KEY);
        if (StringUtils.hasText(mdcCorrelationId)) {
            return mdcCorrelationId.trim();
        }

        return UUID.randomUUID().toString();
    }
}
