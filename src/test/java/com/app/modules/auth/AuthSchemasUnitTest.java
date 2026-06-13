package com.app.modules.auth;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AuthSchemasUnitTest {

    @Test
    void testAuthSchemas() {
        assertNotNull(new AuthSchemas());
    }

    @Test
    void testLoginRequest() {
        AuthSchemas.LoginRequest req = new AuthSchemas.LoginRequest();
        req.email = "test@test.com";
        req.password = "123";
        assertEquals("test@test.com", req.email);
    }

    @Test
    void testAuthResponse() {
        AuthSchemas.AuthResponse res = new AuthSchemas.AuthResponse();
        res.token = "token";
        res.expiresIn = 3600L;
        res.user = new AuthSchemas.UserInfo();
        res.user.name = "User";
        
        assertEquals("token", res.token);
        assertEquals("User", res.user.name);
    }

    @Test
    void testRefreshTokenRequest() {
        AuthSchemas.RefreshTokenRequest req = new AuthSchemas.RefreshTokenRequest();
        req.refreshToken = "abc";
        assertEquals("abc", req.refreshToken);
    }

    @Test
    void testPasswordRequest() {
        AuthSchemas.PasswordRequest req = new AuthSchemas.PasswordRequest();
        req.email = "pass@test.com";
        assertEquals("pass@test.com", req.email);
    }
    
    @Test
    void testUserInfo() {
        AuthSchemas.UserInfo user = new AuthSchemas.UserInfo();
        user.id = "1";
        assertEquals("1", user.id);
    }
}
