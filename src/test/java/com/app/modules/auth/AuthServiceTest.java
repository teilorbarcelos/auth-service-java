package com.app.modules.auth;

import com.app.core.exception.BadRequestException;
import com.app.infrastructure.auth.JwtService;
import com.app.modules.user.UserRepository;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.InjectMock;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@QuarkusTest
public class AuthServiceTest {

    @Inject
    AuthService authService;

    @InjectMock
    UserRepository userRepository;

    @InjectMock
    JwtService jwtService;

    @Test
    @DisplayName("Should throw exception for missing credentials")
    public void testLoginMissingCredentials() {
        assertThrows(BadRequestException.class, () -> authService.login("", ""));
        assertThrows(BadRequestException.class, () -> authService.login("email@test.com", ""));
    }

    @Test
    @DisplayName("Should throw exception for non-existent user")
    public void testLoginUserNotFound() {
        when(userRepository.findByEmail(anyString())).thenReturn(null);
        assertThrows(WebApplicationException.class, () -> authService.login("wrong@test.com", "password"));
    }

    @Test
    @DisplayName("Should throw exception for invalid refresh token")
    public void testRefreshTokenInvalid() {
        assertThrows(BadRequestException.class, () -> authService.refreshToken(""));

        when(jwtService.validateToken(anyString())).thenReturn(null);
        assertThrows(WebApplicationException.class, () -> authService.refreshToken("invalid-token"));
    }

    @Test
    @DisplayName("Should throw exception when refresh token claims are missing uid")
    public void testRefreshTokenMissingUid() {
        when(jwtService.validateToken(anyString())).thenReturn(Map.of("other", "claim"));
        assertThrows(WebApplicationException.class, () -> authService.refreshToken("token-without-uid"));
    }
}
