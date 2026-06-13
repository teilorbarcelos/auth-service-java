package com.app.infrastructure;

import com.app.infrastructure.auth.JwtService;
import com.app.infrastructure.auth.UserSession;
import com.app.infrastructure.ratelimit.RateLimitFilter;
import com.app.infrastructure.auth.AuthFilter;
import com.app.infrastructure.auth.Authenticated;
import com.app.infrastructure.auth.PermissionFilter;
import com.app.infrastructure.auth.RequiresPermission;
import com.app.modules.audit.AuditService;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.keys.KeyCommands;
import io.quarkus.redis.datasource.value.ValueCommands;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@QuarkusTest
@TestProfile(MockRedisProfile.class)
public class InfrastructureTest {

    @io.quarkus.test.junit.mockito.InjectSpy
    JwtService jwtService;

    @Inject
    RateLimitFilter rateLimitFilter;

    @Inject
    AuthFilter authFilter;

    @Inject
    UserSession userSession;

    @InjectMock
    AuditService auditService;

    @InjectMock
    RedisDataSource redisDataSource;

    @Test
    void testJwtService_FullFlow() {
        Map<String, Object> claims = Map.of("email", "test@test.com", "roleId", 1);
        String token = jwtService.createToken("123", claims, 3600);
        assertNotNull(token);

        Map<String, Object> validated = jwtService.validateToken(token);
        assertNotNull(validated);
        assertEquals("123", validated.get("uid"));
        assertEquals("test@test.com", validated.get("email"));
    }

    @Test
    void testJwtService_InvalidToken() {
        assertNull(jwtService.validateToken(null));
        assertNull(jwtService.validateToken("   "));
        assertNull(jwtService.validateToken("invalid.token.here"));
    }

    @Test
    void testRateLimitFilter_AdminBypass() {
        userSession.setUser(Map.of("uid", "admin", "roleId", "administrator"));
        ContainerRequestContext request = mock(ContainerRequestContext.class);
        
        rateLimitFilter.filter(request);
        
        verify(request, never()).abortWith(any());
        verify(redisDataSource, never()).value(eq(String.class), eq(String.class));
    }

    @Test
    void testRateLimitFilter_Exceeded() {
        userSession.setUser(Map.of("uid", "user", "roleId", "user"));
        ContainerRequestContext request = mock(ContainerRequestContext.class);
        UriInfo uriInfo = mock(UriInfo.class);
        when(uriInfo.getPath()).thenReturn("/test");
        when(request.getUriInfo()).thenReturn(uriInfo);
        when(request.getHeaderString("X-Forwarded-For")).thenReturn("1.2.3.4");

        ValueCommands<String, String> values = mock(ValueCommands.class);
        when(redisDataSource.value(eq(String.class), eq(String.class))).thenReturn(values);

        // Mock current limit reached
        when(values.incr(anyString())).thenReturn(1000L);

        rateLimitFilter.filter(request);

        ArgumentCaptor<Response> responseCaptor = ArgumentCaptor.forClass(Response.class);
        verify(request).abortWith(responseCaptor.capture());
        assertEquals(429, responseCaptor.getValue().getStatus());
    }

    @Test
    void testRateLimitFilter_NormalFlow() {
        userSession.setUser(Map.of("uid", "user", "roleId", "user"));
        ContainerRequestContext request = mock(ContainerRequestContext.class);
        UriInfo uriInfo = mock(UriInfo.class);
        when(uriInfo.getPath()).thenReturn("/test");
        when(request.getUriInfo()).thenReturn(uriInfo);

        ValueCommands<String, String> values = mock(ValueCommands.class);
        KeyCommands<String> keys = mock(KeyCommands.class);
        when(redisDataSource.value(eq(String.class), eq(String.class))).thenReturn(values);
        when(redisDataSource.key(eq(String.class))).thenReturn(keys);
        when(values.incr(anyString())).thenReturn(1L);

        rateLimitFilter.filter(request);

        verify(values).incr(anyString());
        verify(keys).expire(anyString(), anyLong());
        verify(request).setProperty(eq("X-RateLimit-Remaining"), anyString());
    }

    @Test
    void testRateLimitFilter_RedisError() {
        userSession.setUser(Map.of("uid", "user", "roleId", "user"));
        ContainerRequestContext request = mock(ContainerRequestContext.class);
        UriInfo uriInfo = mock(UriInfo.class);
        when(uriInfo.getPath()).thenReturn("/test");
        when(request.getUriInfo()).thenReturn(uriInfo);

        when(redisDataSource.value(eq(String.class), eq(String.class))).thenThrow(new RuntimeException("Redis down"));

        // Should not throw, just continue
        assertDoesNotThrow(() -> rateLimitFilter.filter(request));
    }

    @Test
    void testRateLimitFilter_ResponseHeaders() {
        ContainerRequestContext request = mock(ContainerRequestContext.class);
        ContainerResponseContext response = mock(ContainerResponseContext.class);
        when(request.getProperty("X-RateLimit-Remaining")).thenReturn("59");
        MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        when(response.getHeaders()).thenReturn(headers);

        rateLimitFilter.filter(request, response);

        assertTrue(headers.containsKey("X-RateLimit-Remaining"));
        assertEquals("59", headers.getFirst("X-RateLimit-Remaining"));
    }

    @Test
    void testAuthFilter_NoAuthRequired() throws Exception {
        ContainerRequestContext request = mock(ContainerRequestContext.class);
        UriInfo uriInfo = mock(UriInfo.class);
        when(request.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPath()).thenReturn("/test");
        
        ResourceInfo resourceInfo = mock(ResourceInfo.class);
        authFilter.resourceInfo = resourceInfo;
        
        Method method = Object.class.getMethod("toString");
        when(resourceInfo.getResourceMethod()).thenReturn(method);
        when(resourceInfo.getResourceClass()).thenReturn((Class) Object.class);

        authFilter.filter(request);
        verify(request, never()).abortWith(any());
    }

    @Test
    void testAuthFilter_MissingToken() throws Exception {
        ContainerRequestContext request = mock(ContainerRequestContext.class);
        UriInfo uriInfo = mock(UriInfo.class);
        when(request.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPath()).thenReturn("/test");
        
        ResourceInfo resourceInfo = mock(ResourceInfo.class);
        authFilter.resourceInfo = resourceInfo;
        
        @Authenticated class TestClass {}
        when(resourceInfo.getResourceMethod()).thenReturn(TestClass.class.getMethod("toString"));
        when(resourceInfo.getResourceClass()).thenReturn((Class) TestClass.class);
        when(request.getHeaderString("Authorization")).thenReturn(null);

        authFilter.filter(request);
        verify(request).abortWith(any());
    }

    @Test
    void testAuthFilter_RevokedToken() throws Exception {
        ContainerRequestContext request = mock(ContainerRequestContext.class);
        UriInfo uriInfo = mock(UriInfo.class);
        when(request.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPath()).thenReturn("/test");
        
        ResourceInfo resourceInfo = mock(ResourceInfo.class);
        authFilter.resourceInfo = resourceInfo;
        
        @Authenticated class TestClass {}
        when(resourceInfo.getResourceMethod()).thenReturn(TestClass.class.getMethod("toString"));
        when(resourceInfo.getResourceClass()).thenReturn((Class) TestClass.class);
        when(request.getHeaderString("Authorization")).thenReturn("Bearer valid_token");
        
        doReturn(Map.of("uid", "user123", "email", "test@test.com", "roleId", "user")).when(jwtService).validateToken("valid_token");
        doReturn(-1L).when(jwtService).getSessionVersion(anyString());

        authFilter.filter(request);
        verify(request).abortWith(any());
    }

    @Test
    @RequiresPermission(feature = "test", action = "view")
    void testPermissionFilter_Denied() throws Exception {
        ContainerRequestContext request = mock(ContainerRequestContext.class);
        PermissionFilter permissionFilter = new PermissionFilter("test", "view");
        
        UriInfo uriInfo = mock(UriInfo.class);
        when(request.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPathParameters()).thenReturn(new jakarta.ws.rs.core.MultivaluedHashMap<>());
        
        userSession.setUser(Map.of("uid", "user", "roleId", "user", "permissions", List.of()));

        permissionFilter.filter(request);
        verify(request).abortWith(any());
    }

    @Test
    void testPermissionFilter_MissingFeature() throws Exception {
        ContainerRequestContext request = mock(ContainerRequestContext.class);
        UriInfo uriInfo = mock(UriInfo.class);
        when(request.getUriInfo()).thenReturn(uriInfo);
        
        PermissionFilter permissionFilter = new PermissionFilter("", "view");
        
        permissionFilter.filter(request);
    }
}
