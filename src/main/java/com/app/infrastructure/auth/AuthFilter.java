package com.app.infrastructure.auth;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * JAX-RS filter that validates Bearer JWT tokens on @Authenticated endpoints.
 * Equivalent to AuthMiddleware.php
 */
@Provider
@Priority(Priorities.AUTHENTICATION)
public class AuthFilter implements ContainerRequestFilter {

    @Inject
    JwtService jwtService;

    private static final org.jboss.logging.Logger LOG = org.jboss.logging.Logger.getLogger(AuthFilter.class);

    @Inject
    UserSession userSession;

    @Inject
    public ResourceInfo resourceInfo;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        if (requestContext.getUriInfo() != null) {
            LOG.debugf("Filter triggered for: %s", requestContext.getUriInfo().getPath());
        }

        if (resourceInfo == null || resourceInfo.getResourceMethod() == null) {
            return;
        }

        Method method = resourceInfo.getResourceMethod();
        Class<?> clazz = resourceInfo.getResourceClass();

        String path = "";
        if (requestContext.getUriInfo() != null && requestContext.getUriInfo().getPath() != null) {
            path = requestContext.getUriInfo().getPath();
        }

        if (path.endsWith("/v1/auth/login") || path.endsWith("/v1/health") || path.endsWith("/v1/health/live")
                || path.endsWith("/v1/health/ready")) {
            return;
        }

        boolean hasAuthenticated = method.isAnnotationPresent(Authenticated.class)
                || clazz.isAnnotationPresent(Authenticated.class);
        boolean hasPermission = method.isAnnotationPresent(RequiresPermission.class);
        boolean requiresAuth = hasAuthenticated || hasPermission;

        if (!requiresAuth) {
            return;
        }

        String authHeader = requestContext.getHeaderString("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            LOG.warn("AuthFilter: Missing or invalid Authorization header");
            requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Map.of("error", "UnauthorizedError")).build());
            return;
        }

        String token = authHeader.substring(7);
        Map<String, Object> claims = jwtService.validateToken(token);

        if (claims == null) {
            LOG.warn("AuthFilter: JWT validation failed (signature or expiration)");
            requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Map.of("error", "UnauthorizedError")).build());
            return;
        }

        String userId = null;
        if (claims.get("uid") != null) {
            userId = claims.get("uid").toString();
        } else if (claims.get("sub") != null) {
            userId = claims.get("sub").toString();
        }

        long sv = -1;
        if (claims.get("sv") instanceof Number n) {
            sv = n.longValue();
        }

        boolean sessionValid = userId != null && sv >= 0 && jwtService.getSessionVersion(userId) == sv;

        if (userId == null || !sessionValid) {
            LOG.warnv("AuthFilter: Auth failed. userId={0}, sessionValid={1}", userId, sessionValid);
            requestContext.setProperty("auth_aborted", true);
            requestContext.abortWith(
                    Response.status(401).entity(Map.of("error", "UnauthorizedError")).build());
            return;
        }

        LOG.infov("AuthFilter: Auth successful for user={0}", userId);
        userSession.setUser(claims);
        requestContext.setProperty("userId", userId);
    }
}
