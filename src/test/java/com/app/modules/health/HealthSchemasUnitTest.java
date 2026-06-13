package com.app.modules.health;

import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class HealthSchemasUnitTest {

    @Test
    void testHealthSchemas() {
        assertNotNull(new HealthSchemas());
    }

    @Test
    void testHealthResponseDto() {
        HealthSchemas.HealthResponseDto dto = new HealthSchemas.HealthResponseDto();
        dto.status = "UP";
        dto.deployTimestamp = "2026-05-01";
        dto.checks = Map.of("db", Map.of("status", "OK", "message", "Connected"));
        
        assertEquals("UP", dto.status);
        assertEquals("2026-05-01", dto.deployTimestamp);
        assertNotNull(dto.checks);
    }
}
