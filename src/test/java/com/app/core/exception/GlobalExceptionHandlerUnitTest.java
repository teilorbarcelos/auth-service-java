package com.app.core.exception;

import com.app.core.dto.ErrorResponse;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class GlobalExceptionHandlerUnitTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setup() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    void testValidationException() {
        ValidationException ve = new ValidationException(Map.of("email", "required"));
        Response response = exceptionHandler.toResponse(ve);

        assertEquals(400, response.getStatus());
        ErrorResponse err = (ErrorResponse) response.getEntity();
        assertEquals("VALIDATION_ERROR", err.getError().getCode());
    }

    @Test
    void testBadRequestException() {
        BadRequestException bre = new BadRequestException("Invalid input");
        Response response = exceptionHandler.toResponse(bre);

        assertEquals(400, response.getStatus());
        ErrorResponse err = (ErrorResponse) response.getEntity();
        assertEquals("BAD_REQUEST", err.getError().getCode());
    }

    @Test
    void testWebApplicationException_401() {
        WebApplicationException wae = new WebApplicationException("Unauthorized", 401);
        Response response = exceptionHandler.toResponse(wae);

        assertEquals(401, response.getStatus());
        Map<String, String> entity = (Map<String, String>) response.getEntity();
        assertEquals("UnauthorizedError", entity.get("error"));
    }

    @Test
    void testWebApplicationException_Other() {
        WebApplicationException wae = new WebApplicationException("Not Found", 404);
        Response response = exceptionHandler.toResponse(wae);

        assertEquals(404, response.getStatus());
        ErrorResponse err = (ErrorResponse) response.getEntity();
        assertEquals("HTTP_ERROR", err.getError().getCode());
    }

    @Test
    void testOptimisticLockException() {
        jakarta.persistence.OptimisticLockException ole = new jakarta.persistence.OptimisticLockException("Conflict");
        Response response = exceptionHandler.toResponse(ole);

        assertEquals(409, response.getStatus());
        ErrorResponse err = (ErrorResponse) response.getEntity();
        assertEquals("CONFLICT", err.getError().getCode());
    }

    @Test
    void testGenericException() {
        RuntimeException re = new RuntimeException("Unexpected error");
        Response response = exceptionHandler.toResponse(re);

        assertEquals(500, response.getStatus());
        ErrorResponse err = (ErrorResponse) response.getEntity();
        assertEquals("INTERNAL_SERVER_ERROR", err.getError().getCode());
    }
}
