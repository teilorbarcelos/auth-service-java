package com.app.infrastructure.auth;

import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.util.Map;

public class PermissionFilter implements ContainerRequestFilter {

    private static final Logger LOG = Logger.getLogger(PermissionFilter.class);

    private final String feature;
    private final String action;

    public PermissionFilter(String feature, String action) {
        this.feature = feature;
        this.action = action;
    }

    protected UserSession getUserSession() {
        return io.quarkus.arc.Arc.container().instance(UserSession.class).get();
    }

    @Override
    public void filter(ContainerRequestContext requestContext) {
        if (Boolean.TRUE.equals(requestContext.getProperty("auth_aborted"))) {
            return;
        }

        UserSession userSession = getUserSession();
        if (userSession.getUser() == null) {
            LOG.warn("PermissionFilter: No user session found for request");
            requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Map.of("error", "Unauthorized", "message", "User session not found"))
                    .build());
            return;
        }

        if (!userSession.hasPermission(feature, action)) {
            LOG.warnv("PermissionFilter: Access denied for user={0} on {1}:{2}",
                    userSession.getUserId(), feature, action);

            requestContext.abortWith(Response.status(Response.Status.FORBIDDEN)
                    .entity(Map.of("error", "Forbidden", "message", "You do not have permission to access this resource"))
                    .build());
        }
    }
}
