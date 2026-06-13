package com.app.modules.auth;

import com.app.infrastructure.auth.UserSession;
import com.app.modules.auth.dto.AuthResponseDTO;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class AuthResourceUnitTest {

    private AuthResource authResource;
    private AuthService authService;
    private UserSession userSession;

    @BeforeEach
    void setup() {
        authService = mock(AuthService.class);
        userSession = mock(UserSession.class);
        authResource = new AuthResource();
        authResource.authService = authService;
        authResource.userSession = userSession;
    }

    @Test
    void testLogin() {
        AuthResponseDTO dto = new AuthResponseDTO();
        when(authService.login("e", "p")).thenReturn(dto);
        
        Response resp = authResource.login(Map.of("email", "e", "password", "p"));
        assertEquals(200, resp.getStatus());
        assertEquals(dto, resp.getEntity());
    }

    @Test
    void testMe() {
        when(userSession.getUserId()).thenReturn("u1");
        AuthResponseDTO dto = new AuthResponseDTO();
        when(authService.getMe("u1")).thenReturn(dto);
        
        Response resp = authResource.me();
        assertEquals(200, resp.getStatus());
        assertEquals(dto, resp.getEntity());
    }

    @Test
    void testRefresh() {
        AuthResponseDTO dto = new AuthResponseDTO();
        when(authService.refreshToken("r1")).thenReturn(dto);
        
        Response resp = authResource.refresh(Map.of("refreshToken", "r1"));
        assertEquals(200, resp.getStatus());
        assertEquals(dto, resp.getEntity());
    }

    @Test
    void testPasswordFlows() {
        // Request
        Response r1 = authResource.requestPasswordReset(Map.of("email", "e"));
        assertEquals(200, r1.getStatus());
        verify(authService).requestPasswordReset("e");
        
        // Validate
        Response r2 = authResource.validateResetToken(Map.of("email", "e", "token", "t"));
        assertEquals(200, r2.getStatus());
        verify(authService).validateResetToken("e", "t");
        
        // Change
        Response r3 = authResource.resetPassword(Map.of("email", "e", "token", "t", "password", "p"));
        assertEquals(200, r3.getStatus());
        verify(authService).resetPassword("e", "t", "p");
    }
}
