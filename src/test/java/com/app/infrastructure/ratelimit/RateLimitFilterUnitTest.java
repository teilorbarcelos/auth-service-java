package com.app.infrastructure.ratelimit;

import com.app.infrastructure.auth.UserSession;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.keys.KeyCommands;
import io.quarkus.redis.datasource.value.ValueCommands;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class RateLimitFilterUnitTest {

    private RateLimitFilter rateLimitFilter;
    private RedisDataSource redisDataSource;
    private UserSession userSession;
    private ValueCommands<String, String> valueCommands;
    private KeyCommands<String> keyCommands;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setup() {
        redisDataSource = mock(RedisDataSource.class);
        userSession = mock(UserSession.class);
        valueCommands = mock(ValueCommands.class);
        keyCommands = mock(KeyCommands.class);

        when(redisDataSource.value(eq(String.class), eq(String.class))).thenReturn(valueCommands);
        when(redisDataSource.key(eq(String.class))).thenReturn(keyCommands);

        rateLimitFilter = new RateLimitFilter();
        rateLimitFilter.redisDataSource = redisDataSource;
        rateLimitFilter.userSession = userSession;
        rateLimitFilter.limit = 60;
        rateLimitFilter.window = 60;
    }

    @Test
    void testBypassForAdmin() {
        when(userSession.isAdmin()).thenReturn(true);
        ContainerRequestContext request = mock(ContainerRequestContext.class);

        rateLimitFilter.filter(request);

        verify(redisDataSource, never()).value(any(Class.class), any(Class.class));
    }

    @Test
    void testFirstRequest() {
        ContainerRequestContext request = mock(ContainerRequestContext.class);
        UriInfo uriInfo = mock(UriInfo.class);
        when(uriInfo.getPath()).thenReturn("/test");
        when(request.getUriInfo()).thenReturn(uriInfo);
        when(request.getHeaderString("X-Forwarded-For")).thenReturn("1.2.3.4");
        when(valueCommands.incr(anyString())).thenReturn(1L);

        rateLimitFilter.filter(request);

        verify(valueCommands).incr(contains("1.2.3.4"));
        verify(keyCommands).expire(anyString(), eq(60L));
        verify(request).setProperty(eq("X-RateLimit-Remaining"), eq("59"));
    }

    @Test
    void testWithinLimit() {
        ContainerRequestContext request = mock(ContainerRequestContext.class);
        UriInfo uriInfo = mock(UriInfo.class);
        when(uriInfo.getPath()).thenReturn("/test");
        when(request.getUriInfo()).thenReturn(uriInfo);
        when(request.getHeaderString("X-Forwarded-For")).thenReturn(null);
        when(valueCommands.incr(anyString())).thenReturn(30L);

        rateLimitFilter.filter(request);

        verify(request).setProperty(eq("X-RateLimit-Remaining"), eq("30"));
    }

    @Test
    void testLimitExceeded() {
        ContainerRequestContext request = mock(ContainerRequestContext.class);
        UriInfo uriInfo = mock(UriInfo.class);
        when(uriInfo.getPath()).thenReturn("/test");
        when(request.getUriInfo()).thenReturn(uriInfo);
        when(valueCommands.incr(anyString())).thenReturn(61L);

        rateLimitFilter.filter(request);

        ArgumentCaptor<Response> captor = ArgumentCaptor.forClass(Response.class);
        verify(request).abortWith(captor.capture());
        assertEquals(429, captor.getValue().getStatus());
        verify(request, never()).setProperty(anyString(), any());
    }

    @Test
    void testResponseFilter_WithRemaining() {
        ContainerRequestContext request = mock(ContainerRequestContext.class);
        ContainerResponseContext response = mock(ContainerResponseContext.class);
        when(response.getHeaders()).thenReturn(new MultivaluedHashMap<>());
        when(request.getProperty("X-RateLimit-Remaining")).thenReturn("30");

        rateLimitFilter.filter(request, response);
    }

    @Test
    void testBlankForwardedFor() {
        ContainerRequestContext request = mock(ContainerRequestContext.class);
        UriInfo uriInfo = mock(UriInfo.class);
        when(uriInfo.getPath()).thenReturn("/test");
        when(request.getUriInfo()).thenReturn(uriInfo);
        when(request.getHeaderString("X-Forwarded-For")).thenReturn("  ");
        when(valueCommands.incr(anyString())).thenReturn(1L);

        rateLimitFilter.filter(request);

        verify(valueCommands).incr(contains("unknown"));
    }

    @Test
    void testResponseFilter_NoRemaining() {
        ContainerRequestContext request = mock(ContainerRequestContext.class);
        ContainerResponseContext response = mock(ContainerResponseContext.class);
        when(request.getProperty("X-RateLimit-Remaining")).thenReturn(null);

        rateLimitFilter.filter(request, response);

        verify(response, never()).getHeaders();
    }
}
