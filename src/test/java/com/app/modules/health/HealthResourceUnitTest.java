package com.app.modules.health;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.value.ValueCommands;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class HealthResourceUnitTest {

    private HealthResource healthResource;
    private EntityManager em;
    private RedisDataSource redisDataSource;
    private ValueCommands<String, String> valueCommands;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setup() {
        em = mock(EntityManager.class);
        redisDataSource = mock(RedisDataSource.class);
        valueCommands = mock(ValueCommands.class);

        when(redisDataSource.value(eq(String.class))).thenReturn(valueCommands);

        healthResource = new HealthResource();
        healthResource.em = em;
        healthResource.redisDataSource = redisDataSource;
        healthResource.appVersion = "1.0.0";
    }

    @Test
    void testHealth_UP() {
        Query query = mock(Query.class);
        when(em.createNativeQuery(anyString())).thenReturn(query);
        when(query.getSingleResult()).thenReturn(1);
        when(valueCommands.get(anyString())).thenReturn(null);

        Response response = healthResource.health();

        assertEquals(200, response.getStatus());
        Map<String, Object> data = (Map<String, Object>) response.getEntity();
        assertEquals("UP", data.get("status"));
    }

    @Test
    void testHealth_DatabaseDown() {
        when(em.createNativeQuery(anyString())).thenThrow(new RuntimeException("DB Connection Failed"));
        when(valueCommands.get(anyString())).thenReturn(null);

        Response response = healthResource.health();

        assertEquals(503, response.getStatus());
        Map<String, Object> data = (Map<String, Object>) response.getEntity();
        assertEquals("DEGRADED", data.get("status"));

        Map<String, Map<String, String>> checks = (Map<String, Map<String, String>>) data.get("checks");
        assertEquals("ERROR", checks.get("database").get("status"));
        assertEquals("DB Connection Failed", checks.get("database").get("message"));
    }

    @Test
    void testHealth_RedisDown() {
        Query query = mock(Query.class);
        when(em.createNativeQuery(anyString())).thenReturn(query);
        when(query.getSingleResult()).thenReturn(1);
        when(valueCommands.get(anyString())).thenThrow(new RuntimeException("Redis Connection Failed"));

        Response response = healthResource.health();

        assertEquals(503, response.getStatus());
        Map<String, Object> data = (Map<String, Object>) response.getEntity();
        assertEquals("DEGRADED", data.get("status"));

        Map<String, Map<String, String>> checks = (Map<String, Map<String, String>>) data.get("checks");
        assertEquals("ERROR", checks.get("redis").get("status"));
        assertEquals("Redis Connection Failed", checks.get("redis").get("message"));
    }

    @Test
    void testGetUptime() {
        Response response = healthResource.health();
        Map<String, Object> data = (Map<String, Object>) response.getEntity();
        assertNotNull(data.get("uptime"));
        assertTrue(((String)data.get("uptime")).contains("d "));
    }

    @Test
    void testLiveness() {
        Response response = healthResource.liveness();
        assertEquals(200, response.getStatus());
        Map<String, Object> data = (Map<String, Object>) response.getEntity();
        assertEquals("alive", data.get("status"));
    }

    @Test
    void testReadiness_UP() {
        Query query = mock(Query.class);
        when(em.createNativeQuery(anyString())).thenReturn(query);
        when(query.getSingleResult()).thenReturn(1);
        when(valueCommands.get(anyString())).thenReturn(null);

        Response response = healthResource.readiness();
        assertEquals(200, response.getStatus());
        Map<String, Object> data = (Map<String, Object>) response.getEntity();
        assertEquals("UP", data.get("status"));
    }

    @Test
    void testReadiness_DOWN() {
        when(em.createNativeQuery(anyString())).thenThrow(new RuntimeException("DB Down"));

        Response response = healthResource.readiness();
        assertEquals(503, response.getStatus());
        Map<String, Object> data = (Map<String, Object>) response.getEntity();
        assertEquals((int) Response.Status.SERVICE_UNAVAILABLE.getStatusCode(), response.getStatus());
        assertEquals("UP", data.get("status"));
    }

    @Test
    void testReadiness_RedisDown() {
        Query query = mock(Query.class);
        when(em.createNativeQuery(anyString())).thenReturn(query);
        when(query.getSingleResult()).thenReturn(1);
        when(valueCommands.get(anyString())).thenThrow(new RuntimeException("Redis Down"));

        Response response = healthResource.readiness();
        assertEquals(503, response.getStatus());
        Map<String, Object> data = (Map<String, Object>) response.getEntity();
        assertFalse((Boolean) data.get("redis"));
        assertTrue((Boolean) data.get("database"));
    }
}
