package com.app.infrastructure.ratelimit;

import com.app.infrastructure.auth.UserSession;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.keys.KeyCommands;
import io.quarkus.redis.datasource.value.ValueCommands;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.util.Map;

/**
 * Rate limiting filter using Redis.
 * Equivalent to RateLimitMiddleware.php
 */
@Provider
@Priority(Priorities.AUTHENTICATION - 100)
public class RateLimitFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger LOG = Logger.getLogger(RateLimitFilter.class);

    @ConfigProperty(name = "app.rate-limit.max", defaultValue = "60")
    int limit;

    @ConfigProperty(name = "app.rate-limit.window", defaultValue = "60")
    int window;

    @Inject
    RedisDataSource redisDataSource;

    @Inject
    UserSession userSession;

    private static final String RATE_LIMIT_REMAINING = "X-RateLimit-Remaining";

    @Override
    public void filter(ContainerRequestContext requestContext) {
        if (userSession.isAdmin()) {
            return;
        }

        String ip = getClientIp(requestContext);
        String route = requestContext.getUriInfo().getPath();
        String key = "rate_limit:" + ip + ":" + route;

        try {
            ValueCommands<String, String> values = redisDataSource.value(String.class, String.class);
            KeyCommands<String> keys = redisDataSource.key(String.class);

            long count = values.incr(key);
            if (count == 1) {
                keys.expire(key, window);
            }

            if (count > limit) {
                requestContext.abortWith(Response.status(429)
                        .header("Content-Type", "application/json")
                        .header("X-RateLimit-Limit", String.valueOf(limit))
                        .header(RATE_LIMIT_REMAINING, "0")
                        .entity(Map.of(
                                "error", "Too Many Requests",
                                "message", "Rate limit exceeded. Try again in some seconds.",
                                "limit", limit,
                                "window", window + "s"))
                        .build());
                return;
            }

            requestContext.setProperty(RATE_LIMIT_REMAINING, String.valueOf(Math.max(0, limit - count)));
        } catch (Exception e) {
            LOG.errorv("Rate limit check failed for key={0}: {1}", key, e.getMessage());
        }
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        String remaining = (String) requestContext.getProperty(RATE_LIMIT_REMAINING);
        if (remaining != null) {
            responseContext.getHeaders().putSingle("X-RateLimit-Limit", String.valueOf(limit));
            responseContext.getHeaders().putSingle(RATE_LIMIT_REMAINING, remaining);
            responseContext.getHeaders().putSingle("X-RateLimit-Reset", String.valueOf(window));
        }
    }

    private String getClientIp(ContainerRequestContext ctx) {
        String forwarded = ctx.getHeaderString("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return "unknown";
    }
}
