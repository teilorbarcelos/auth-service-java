package com.app.infrastructure.log;

import com.app.infrastructure.metrics.MetricService;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;
import org.jboss.logging.MDC;

import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * Structured request logging and metrics filter.
 * Equivalent to LogMiddleware.php
 */
@Provider
@Priority(Priorities.USER - 200)
public class RequestLogFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger LOG = Logger.getLogger(RequestLogFilter.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String START_TIME = "request-start-time";
    private static final String REQUEST_ID_KEY = "request-id";
    private static final String USER_ID_KEY = "userId";
    private static final String NORMALIZED_PATH = "normalized-path";

    @Inject
    MetricService metricService;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        requestContext.setProperty(START_TIME, System.nanoTime());
        byte[] bytes = new byte[8];
        RANDOM.nextBytes(bytes);
        String requestId = HexFormat.of().formatHex(bytes);
        requestContext.setProperty(REQUEST_ID_KEY, requestId);

        String normalizedPath = resolveNormalizedPath(requestContext);
        requestContext.setProperty(NORMALIZED_PATH, normalizedPath);

        MDC.put("requestId", requestId);
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        try {
            Long startTime = (Long) requestContext.getProperty(START_TIME);
            String requestId = (String) requestContext.getProperty(REQUEST_ID_KEY);
            String userId = (String) requestContext.getProperty(USER_ID_KEY);

            if (startTime != null && requestId != null) {
                long durationNs = System.nanoTime() - startTime;
                long durationMs = durationNs / 1_000_000;

                String method = requestContext.getMethod();
                String normalizedPath = (String) requestContext.getProperty(NORMALIZED_PATH);
                if (normalizedPath == null) {
                    normalizedPath = requestContext.getUriInfo().getPath();
                }
                int status = responseContext.getStatus();

                String clientIp = getClientIp(requestContext);

                if (userId != null) {
                    MDC.put(USER_ID_KEY, userId);
                }

                LOG.infov("Request processed | request_id={0} method={1} url={2} status={3} duration_ms={4} ip={5}",
                        requestId, method, normalizedPath, status, durationMs, clientIp);

                metricService.incrementCounter("http_requests_total",
                        "method", method,
                        "status", String.valueOf(status),
                        "path", normalizedPath);

                metricService.recordTimer("http_request_duration_ms", durationMs,
                        "method", method,
                        "path", normalizedPath);

                responseContext.getHeaders().putSingle("X-Request-ID", requestId);
            }
        } finally {
            MDC.remove("requestId");
            MDC.remove(USER_ID_KEY);
        }
    }

    private static String normalizePath(String path) {
        return path.replaceAll("/[0-9a-fA-F-]{36}", "/{id}")
                   .replaceAll("/\\d+", "/{id}");
    }

    private String resolveNormalizedPath(ContainerRequestContext requestContext) {
        return normalizePath(requestContext.getUriInfo().getPath());
    }

    private String getClientIp(ContainerRequestContext ctx) {
        String forwarded = ctx.getHeaderString("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return "unknown";
    }
}
