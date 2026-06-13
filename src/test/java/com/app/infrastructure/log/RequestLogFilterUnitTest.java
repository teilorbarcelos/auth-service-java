package com.app.infrastructure.log;

import com.app.infrastructure.metrics.MetricService;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.UriInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.net.URI;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class RequestLogFilterUnitTest {

    private RequestLogFilter filter;
    private MetricService metricService;
    private ContainerRequestContext requestContext;
    private ContainerResponseContext responseContext;

    @BeforeEach
    void setUp() {
        metricService = mock(MetricService.class);
        filter = new RequestLogFilter();
        filter.metricService = metricService;

        requestContext = mock(ContainerRequestContext.class);
        responseContext = mock(ContainerResponseContext.class);
        
        UriInfo uriInfo = mock(UriInfo.class);
        when(uriInfo.getRequestUri()).thenReturn(URI.create("http://localhost/test"));
        when(uriInfo.getPath()).thenReturn("/test");
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(requestContext.getMethod()).thenReturn("GET");
        when(responseContext.getStatus()).thenReturn(200);
        when(responseContext.getHeaders()).thenReturn(new MultivaluedHashMap<>());
    }

    @Test
    @DisplayName("Should log and record metrics for a request")
    void testFilter() {
        // 1. Request filter
        filter.filter(requestContext);
        verify(requestContext, times(3)).setProperty(anyString(), any());

        // 2. Response filter
        when(requestContext.getProperty("request-start-time")).thenReturn(System.nanoTime());
        when(requestContext.getProperty("request-id")).thenReturn("test-id");
        
        filter.filter(requestContext, responseContext);

        verify(metricService).incrementCounter(
                eq("http_requests_total"), 
                eq("method"), eq("GET"), 
                eq("status"), eq("200"), 
                eq("path"), eq("/test")
        );
        
        verify(metricService).recordTimer(
                eq("http_request_duration_ms"), 
                anyLong(), 
                eq("method"), eq("GET"), 
                eq("path"), eq("/test")
        );
    }

    @Test
    @DisplayName("Should handle missing start time or request id")
    void testFilterMissingProperties() {
        // Case 1: startTime is null
        when(requestContext.getProperty("request-start-time")).thenReturn(null);
        when(requestContext.getProperty("request-id")).thenReturn("test-id");
        filter.filter(requestContext, responseContext);
        
        // Case 2: requestId is null
        when(requestContext.getProperty("request-start-time")).thenReturn(System.nanoTime());
        when(requestContext.getProperty("request-id")).thenReturn(null);
        filter.filter(requestContext, responseContext);
        
        verifyNoInteractions(metricService);
    }

    @Test
    @DisplayName("Should extract client IP from X-Forwarded-For")
    void testGetClientIp() {
        // Case 1: Header present
        when(requestContext.getHeaderString("X-Forwarded-For")).thenReturn("1.2.3.4, 5.6.7.8");
        when(requestContext.getProperty("request-start-time")).thenReturn(System.nanoTime());
        when(requestContext.getProperty("request-id")).thenReturn("test-id");

        filter.filter(requestContext, responseContext);
        verify(metricService, atLeastOnce()).incrementCounter(eq("http_requests_total"), any(String[].class));
    }

    @Test
    @DisplayName("Should handle missing or blank X-Forwarded-For")
    void testGetClientIpEdgeCases() {
        when(requestContext.getProperty("request-start-time")).thenReturn(System.nanoTime());
        when(requestContext.getProperty("request-id")).thenReturn("test-id");

        // Case 1: Header is null
        when(requestContext.getHeaderString("X-Forwarded-For")).thenReturn(null);
        filter.filter(requestContext, responseContext);

        // Case 2: Header is blank
        when(requestContext.getHeaderString("X-Forwarded-For")).thenReturn("   ");
        filter.filter(requestContext, responseContext);

        verify(metricService, times(2)).incrementCounter(eq("http_requests_total"), any(String[].class));
    }
}
