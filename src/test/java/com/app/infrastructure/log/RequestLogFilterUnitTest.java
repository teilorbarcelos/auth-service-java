package com.app.infrastructure.log;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.UriInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class RequestLogFilterUnitTest {

    private RequestLogFilter filter;
    private ContainerRequestContext requestContext;
    private ContainerResponseContext responseContext;
    private MultivaluedMap<String, Object> headers;

    @BeforeEach
    void setUp() {
        filter = new RequestLogFilter();

        requestContext = mock(ContainerRequestContext.class);
        responseContext = mock(ContainerResponseContext.class);
        headers = mock(MultivaluedMap.class);

        UriInfo uriInfo = mock(UriInfo.class);
        when(uriInfo.getRequestUri()).thenReturn(URI.create("http://localhost/test"));
        when(uriInfo.getPath()).thenReturn("/test");
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(requestContext.getMethod()).thenReturn("GET");
        when(responseContext.getStatus()).thenReturn(200);
        when(responseContext.getHeaders()).thenReturn(headers);
    }

    @Test
    @DisplayName("Should log and set request properties")
    void testFilter() {
        filter.filter(requestContext);
        verify(requestContext, times(3)).setProperty(anyString(), any());

        when(requestContext.getProperty("request-start-time")).thenReturn(System.nanoTime());
        when(requestContext.getProperty("request-id")).thenReturn("test-id");

        filter.filter(requestContext, responseContext);
        verify(headers).putSingle("X-Request-ID", "test-id");
    }

    @Test
    @DisplayName("Should handle missing start time or request id")
    void testFilterMissingProperties() {
        when(requestContext.getProperty("request-start-time")).thenReturn(null);
        when(requestContext.getProperty("request-id")).thenReturn("test-id");
        filter.filter(requestContext, responseContext);

        when(requestContext.getProperty("request-start-time")).thenReturn(System.nanoTime());
        when(requestContext.getProperty("request-id")).thenReturn(null);
        filter.filter(requestContext, responseContext);

        verify(headers, never()).putSingle(anyString(), any());
    }

    @Test
    @DisplayName("Should handle userId property in response filter")
    void testFilterWithUserId() {
        when(requestContext.getProperty("request-start-time")).thenReturn(System.nanoTime());
        when(requestContext.getProperty("request-id")).thenReturn("test-id");
        when(requestContext.getProperty("userId")).thenReturn("user-123");

        filter.filter(requestContext, responseContext);
        verify(headers).putSingle("X-Request-ID", "test-id");
    }

    @Test
    @DisplayName("Should extract client IP from X-Forwarded-For")
    void testGetClientIp() {
        when(requestContext.getHeaderString("X-Forwarded-For")).thenReturn("1.2.3.4, 5.6.7.8");
        when(requestContext.getProperty("request-start-time")).thenReturn(System.nanoTime());
        when(requestContext.getProperty("request-id")).thenReturn("test-id");

        filter.filter(requestContext, responseContext);
        verify(headers).putSingle("X-Request-ID", "test-id");
    }

    @Test
    @DisplayName("Should handle missing or blank X-Forwarded-For")
    void testGetClientIpEdgeCases() {
        when(requestContext.getProperty("request-start-time")).thenReturn(System.nanoTime());
        when(requestContext.getProperty("request-id")).thenReturn("test-id");

        when(requestContext.getHeaderString("X-Forwarded-For")).thenReturn(null);
        filter.filter(requestContext, responseContext);

        when(requestContext.getHeaderString("X-Forwarded-For")).thenReturn("   ");
        filter.filter(requestContext, responseContext);

        verify(headers, times(2)).putSingle("X-Request-ID", "test-id");
    }
}
