package com.app.core.exception;

import com.app.core.dto.ErrorResponse;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class GlobalExceptionHandlerTest {

    @Inject
    GlobalExceptionHandler exceptionHandler;

    @Test
    void testValidationException() {
        ValidationException ve = new ValidationException(Map.of("field", "error"));
        Response response = exceptionHandler.toResponse(ve);
        assertEquals(400, response.getStatus());
    }

    @Test
    void testBadRequestException() {
        BadRequestException bre = new BadRequestException("bad request");
        Response response = exceptionHandler.toResponse(bre);
        assertEquals(400, response.getStatus());
    }

    @Test
    void testWebAppException() {
        WebApplicationException wae = new WebApplicationException("unauthorized", 401);
        Response response = exceptionHandler.toResponse(wae);
        assertEquals(401, response.getStatus());
        assertTrue(response.getEntity() instanceof Map);
        assertEquals("UnauthorizedError", ((Map<String, String>) response.getEntity()).get("error"));
    }

    @Test
    void testGenericException() {
        RuntimeException re = new RuntimeException("server error");
        Response response = exceptionHandler.toResponse(re);
        assertEquals(500, response.getStatus());
        assertTrue(response.getEntity() instanceof ErrorResponse);
        ErrorResponse err = (ErrorResponse) response.getEntity();
        assertEquals("INTERNAL_SERVER_ERROR", err.getError().getCode());
    }
}
