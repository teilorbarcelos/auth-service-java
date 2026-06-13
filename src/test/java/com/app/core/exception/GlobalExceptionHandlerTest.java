package com.app.core.exception;

import com.app.core.dto.ErrorResponse;
import com.app.modules.audit.AuditService;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
public class GlobalExceptionHandlerTest {

    @Inject
    GlobalExceptionHandler handler;

    @InjectMock
    AuditService auditService;

    @Test
    @SuppressWarnings("unchecked")
    void testToResponse_ValidationException() {
        ValidationException ex = new ValidationException(Map.of("field", "error"));
        Response response = handler.toResponse(ex);

        assertEquals(400, response.getStatus());
        ErrorResponse entity = (ErrorResponse) response.getEntity();
        assertEquals("Validation Failed", entity.getError().getMessage());
        assertEquals("VALIDATION_ERROR", entity.getError().getCode());
        assertEquals("error", ((Map<String, Object>) entity.getError().getDetails()).get("field"));
    }

    @Test
    void testToResponse_BadRequestException() {
        BadRequestException ex = new BadRequestException("Bad request");
        Response response = handler.toResponse(ex);

        assertEquals(400, response.getStatus());
        ErrorResponse entity = (ErrorResponse) response.getEntity();
        assertEquals("Bad request", entity.getError().getMessage());
        assertEquals("BAD_REQUEST", entity.getError().getCode());
    }

    @Test
    void testToResponse_WebApplicationException() {
        WebApplicationException ex = new WebApplicationException("Not found", 404);
        Response response = handler.toResponse(ex);

        assertEquals(404, response.getStatus());
        ErrorResponse entity = (ErrorResponse) response.getEntity();
        assertEquals("HTTP_ERROR", entity.getError().getCode());
    }

    @Test
    void testToResponse_GenericException() {
        RuntimeException ex = new RuntimeException("Unexpected error");
        Response response = handler.toResponse(ex);

        assertEquals(500, response.getStatus());
        ErrorResponse entity = (ErrorResponse) response.getEntity();
        assertEquals("Unexpected error", entity.getError().getMessage());
        assertEquals("INTERNAL_SERVER_ERROR", entity.getError().getCode());

        Mockito.verify(auditService).logError(Mockito.any(Throwable.class), Mockito.isNull());
    }

    @Test
    void testToResponse_GenericException_NullMessage() {
        RuntimeException ex = new RuntimeException((String) null);
        Response response = handler.toResponse(ex);

        assertEquals(500, response.getStatus());
        ErrorResponse entity = (ErrorResponse) response.getEntity();
        assertEquals("Internal Server Error", entity.getError().getMessage());
    }
}
