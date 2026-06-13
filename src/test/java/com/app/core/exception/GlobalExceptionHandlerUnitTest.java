package com.app.core.exception;

import com.app.core.dto.ErrorResponse;
import com.app.modules.audit.AuditService;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class GlobalExceptionHandlerUnitTest {

    private GlobalExceptionHandler exceptionHandler;
    private AuditService auditService;
    private com.app.infrastructure.metrics.MetricService metricService;

    @BeforeEach
    void setup() {
        auditService = mock(AuditService.class);
        metricService = mock(com.app.infrastructure.metrics.MetricService.class);
        exceptionHandler = new GlobalExceptionHandler();
        exceptionHandler.auditService = auditService;
        exceptionHandler.metricService = metricService;
    }

    @Test
    void testToResponse_ValidationException() {
        ValidationException ve = new ValidationException(Map.of("field1", "error1"));
        Response response = exceptionHandler.toResponse(ve);

        assertEquals(400, response.getStatus());
        ErrorResponse entity = (ErrorResponse) response.getEntity();
        assertEquals("VALIDATION_ERROR", entity.getError().getCode());
        assertNotNull(entity.getError().getDetails());
    }

    @Test
    void testToResponse_BadRequestException() {
        BadRequestException bre = new BadRequestException("Custom bad request");
        Response response = exceptionHandler.toResponse(bre);

        assertEquals(400, response.getStatus());
        ErrorResponse entity = (ErrorResponse) response.getEntity();
        assertEquals("BAD_REQUEST", entity.getError().getCode());
    }

    @Test
    void testToResponse_WebApplicationException() {
        WebApplicationException wae = new WebApplicationException("Forbidden", 403);
        Response response = exceptionHandler.toResponse(wae);

        assertEquals(403, response.getStatus());
        ErrorResponse entity = (ErrorResponse) response.getEntity();
        assertEquals("HTTP_ERROR", entity.getError().getCode());
    }

    @Test
    void testToResponse_GenericException() {
        RuntimeException ex = new RuntimeException("Something went wrong");
        Response response = exceptionHandler.toResponse(ex);

        assertEquals(500, response.getStatus());
        ErrorResponse entity = (ErrorResponse) response.getEntity();
        assertEquals("INTERNAL_SERVER_ERROR", entity.getError().getCode());
        assertEquals("Something went wrong", entity.getError().getMessage());
        verify(auditService).logError(eq(ex), any());
    }

    @Test
    void testToResponse_GenericException_NullMessage() {
        RuntimeException ex = new RuntimeException();
        Response response = exceptionHandler.toResponse(ex);

        assertEquals(500, response.getStatus());
        ErrorResponse entity = (ErrorResponse) response.getEntity();
        assertEquals("Internal Server Error", entity.getError().getMessage());
    }
    
    @Test
    void testToResponse_GenericException_BlankMessage() {
        RuntimeException ex = new RuntimeException("  ");
        Response response = exceptionHandler.toResponse(ex);

        assertEquals(500, response.getStatus());
        ErrorResponse entity = (ErrorResponse) response.getEntity();
        assertEquals("Internal Server Error", entity.getError().getMessage());
    }

    @Test
    void testToResponse_WebApplicationException_404() {
        WebApplicationException wae = new WebApplicationException("Not Found", 404);
        Response response = exceptionHandler.toResponse(wae);

        assertEquals(404, response.getStatus());
        verify(auditService, never()).logError(any(), any());
    }

    @Test
    void testToResponse_OptimisticLockException() {
        jakarta.persistence.OptimisticLockException ole = new jakarta.persistence.OptimisticLockException("Concurrent modification");
        Response response = exceptionHandler.toResponse(ole);

        assertEquals(409, response.getStatus());
        ErrorResponse entity = (ErrorResponse) response.getEntity();
        assertEquals("CONFLICT", entity.getError().getCode());
    }

    @Test
    void testToResponse_AuditServiceThrowsException() {
        RuntimeException ex = new RuntimeException("Original error");
        doThrow(new RuntimeException("Database error during logging")).when(auditService).logError(any(), any());

        Response response = exceptionHandler.toResponse(ex);

        assertEquals(500, response.getStatus());
        ErrorResponse entity = (ErrorResponse) response.getEntity();
        assertEquals("Original error", entity.getError().getMessage());
        verify(auditService).logError(eq(ex), any());
    }
}
