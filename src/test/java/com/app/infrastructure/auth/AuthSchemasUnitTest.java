package com.app.infrastructure.auth;

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
        req.email = "admin@email.com";
        req.password = "123";
        assertEquals("admin@email.com", req.email);
        assertEquals("123", req.password);
    }

    @Test
    void testAuthResponse() {
        AuthSchemas.AuthResponse res = new AuthSchemas.AuthResponse();
        res.token = "abc";
        res.expiresIn = 3600L;
        res.user = new AuthSchemas.UserInfo();
        res.user.id = "1";
        res.user.name = "Admin";
        res.user.email = "admin@email.com";
        res.user.role = "role1";

        assertEquals("abc", res.token);
        assertEquals(3600L, res.expiresIn);
        assertEquals("Admin", res.user.name);
    }

    @Test
    void testMessageResponse() {
        AuthSchemas.MessageResponse res = new AuthSchemas.MessageResponse();
        res.message = "success";
        assertEquals("success", res.message);
    }
    
    @Test
    void testUserInfo() {
        AuthSchemas.UserInfo user = new AuthSchemas.UserInfo();
        user.id = "1";
        assertEquals("1", user.id);
    }
}
