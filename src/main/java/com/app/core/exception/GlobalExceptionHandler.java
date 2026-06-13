package com.app.core.exception;

import com.app.core.dto.ErrorResponse;
import jakarta.persistence.OptimisticLockException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

/**
 * Global exception handler for all REST endpoints.
 * Equivalent to JsonErrorMiddleware.php — catches all exceptions and returns
 * structured JSON.
 */
@Provider
public class GlobalExceptionHandler implements ExceptionMapper<Throwable> {

    private static final Logger LOG = Logger.getLogger(GlobalExceptionHandler.class);

    @jakarta.inject.Inject
    com.app.modules.audit.AuditService auditService;

    @jakarta.inject.Inject
    com.app.infrastructure.metrics.MetricService metricService;

    @Override
    public Response toResponse(Throwable exception) {
        metricService.incrementCounter("jvm_exceptions", "type", exception.getClass().getSimpleName());

        boolean shouldLog = !(exception instanceof WebApplicationException wae && wae.getResponse().getStatus() == 404);

        if (shouldLog) {
            try {
                auditService.logError(exception, null);
            } catch (Exception e) {
                LOG.error("Failed to log error to audit service", e);
            }
        }

        if (exception instanceof ValidationException ve) {
            LOG.warnv("Validation error: {0}", ve.getErrors());
            return Response.status(400)
                    .entity(new ErrorResponse(ve.getMessage(), "VALIDATION_ERROR", ve.getErrors()))
                    .build();
        }

        if (exception instanceof BadRequestException bre) {
            LOG.warnv("Bad request: {0}", bre.getMessage());
            return Response.status(400)
                    .entity(new ErrorResponse(bre.getMessage(), "BAD_REQUEST"))
                    .build();
        }

        if (exception instanceof WebApplicationException wae) {
            int status = wae.getResponse().getStatus();
            LOG.warnv("HTTP error {0}: {1}", status, wae.getMessage());
            if (status == 401) {
                return Response.status(status)
                        .entity(java.util.Map.of("error", "UnauthorizedError"))
                        .build();
            }
            return Response.status(status)
                    .entity(new ErrorResponse(wae.getMessage(), "HTTP_ERROR"))
                    .build();
        }

        if (exception instanceof OptimisticLockException) {
            LOG.warnv("Optimistic lock conflict: {0}", exception.getMessage());
            return Response.status(409)
                    .entity(new ErrorResponse("Resource was modified by another request. Please retry.", "CONFLICT"))
                    .build();
        }

        LOG.errorv(exception, "Unhandled exception: {0}", exception.getMessage());
        exception.printStackTrace();

        int statusCode = 500;
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            message = "Internal Server Error";
        }

        return Response.status(statusCode)
                .entity(new ErrorResponse(message, "INTERNAL_SERVER_ERROR"))
                .build();
    }
}
