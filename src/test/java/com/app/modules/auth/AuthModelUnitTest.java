package com.app.modules.auth;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class AuthModelUnitTest {

    @Test
    void testAuthModel() {
        AuthModel auth = new AuthModel();

        auth.setId("auth-1");
        auth.setPassword("hash");
        auth.setRequestPasswordToken("token-123");
        auth.setRequestPasswordExpiration(LocalDateTime.of(2026, 6, 13, 12, 0));
        auth.setRetries(5);
        auth.setFirstAccess(false);
        auth.setSessionVersion(2);
        auth.setActive(false);
        auth.setCreatedAt(LocalDateTime.of(2026, 1, 1, 0, 0));
        auth.setUpdatedAt(LocalDateTime.of(2026, 6, 1, 0, 0));

        assertEquals("auth-1", auth.getId());
        assertEquals("hash", auth.getPassword());
        assertEquals("token-123", auth.getRequestPasswordToken());
        assertEquals(LocalDateTime.of(2026, 6, 13, 12, 0), auth.getRequestPasswordExpiration());
        assertEquals(5, auth.getRetries());
        assertFalse(auth.getFirstAccess());
        assertEquals(2, auth.getSessionVersion());
        assertFalse(auth.getActive());
        assertEquals(LocalDateTime.of(2026, 1, 1, 0, 0), auth.getCreatedAt());
        assertEquals(LocalDateTime.of(2026, 6, 1, 0, 0), auth.getUpdatedAt());
    }

    @Test
    void testAuthModelDefaults() {
        AuthModel auth = new AuthModel();

        assertNull(auth.getId());
        assertEquals(0, auth.getRetries());
        assertTrue(auth.getFirstAccess());
        assertEquals(1, auth.getSessionVersion());
        assertTrue(auth.getActive());
        assertNull(auth.getCreatedAt());
        assertNull(auth.getUpdatedAt());
    }

    @Test
    void testAuthModelLifecycle() {
        AuthModel auth = new AuthModel();
        auth.onCreate();

        assertNotNull(auth.getCreatedAt());
        assertNotNull(auth.getUpdatedAt());

        LocalDateTime originalCreated = auth.getCreatedAt();
        auth.onUpdate();
        assertTrue(auth.getUpdatedAt().isAfter(originalCreated) || auth.getUpdatedAt().equals(originalCreated));
        assertEquals(originalCreated, auth.getCreatedAt());
    }
}
