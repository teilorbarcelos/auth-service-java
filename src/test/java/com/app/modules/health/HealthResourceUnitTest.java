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
    private com.app.infrastructure.messaging.RabbitMQProvider rabbitMQProvider;
    private com.app.infrastructure.storage.StorageProvider storageProvider;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setup() {
        em = mock(EntityManager.class);
        redisDataSource = mock(RedisDataSource.class);
        valueCommands = mock(ValueCommands.class);
        
        when(redisDataSource.value(eq(String.class))).thenReturn(valueCommands);
        
        
        rabbitMQProvider = mock(com.app.infrastructure.messaging.RabbitMQProvider.class);
        when(rabbitMQProvider.isEnabled()).thenReturn(false);

        healthResource = new HealthResource();
        healthResource.em = em;
        healthResource.redisDataSource = redisDataSource;
        healthResource.rabbitMQProvider = rabbitMQProvider;
        
        storageProvider = mock(com.app.infrastructure.storage.StorageProvider.class);
        when(storageProvider.checkHealth()).thenReturn(true);
        when(storageProvider.getActiveDisk()).thenReturn("local");
        healthResource.storageProvider = storageProvider;

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
        // This is implicit in the other tests, but helps coverage of the logic
        Response response = healthResource.health();
        Map<String, Object> data = (Map<String, Object>) response.getEntity();
        assertNotNull(data.get("uptime"));
        assertTrue(((String)data.get("uptime")).contains("d "));
    }

    @Test
    void testHealth_RabbitMQ_UP() {
        when(rabbitMQProvider.isEnabled()).thenReturn(true);
        when(rabbitMQProvider.isConnected()).thenReturn(true);
        // Mock DB and Redis as UP
        Query query = mock(Query.class);
        when(em.createNativeQuery(anyString())).thenReturn(query);
        when(query.getSingleResult()).thenReturn(1);
        when(valueCommands.get(anyString())).thenReturn(null);

        Response response = healthResource.health();

        assertEquals(200, response.getStatus());
        Map<String, Object> data = (Map<String, Object>) response.getEntity();
        Map<String, Map<String, String>> checks = (Map<String, Map<String, String>>) data.get("checks");
        assertEquals("OK", checks.get("rabbitmq").get("status"));
        assertEquals("Connected", checks.get("rabbitmq").get("message"));
        verify(rabbitMQProvider).isConnected();
    }

    @Test
    void testHealth_RabbitMQ_Down() {
        when(rabbitMQProvider.isEnabled()).thenReturn(true);
        when(rabbitMQProvider.isConnected()).thenReturn(false);
        
        // Mock DB and Redis as UP
        Query query = mock(Query.class);
        when(em.createNativeQuery(anyString())).thenReturn(query);
        when(query.getSingleResult()).thenReturn(1);
        when(valueCommands.get(anyString())).thenReturn(null);

        Response response = healthResource.health();

        assertEquals(503, response.getStatus());
        Map<String, Object> data = (Map<String, Object>) response.getEntity();
        assertEquals("DEGRADED", data.get("status"));
        
        Map<String, Map<String, String>> checks = (Map<String, Map<String, String>>) data.get("checks");
        assertEquals("ERROR", checks.get("rabbitmq").get("status"));
        assertEquals("Rabbit Connection Failed", checks.get("rabbitmq").get("message"));
    }

    @Test
    void testHealth_StorageDown() {
        when(storageProvider.checkHealth()).thenReturn(false);
        when(storageProvider.getActiveDisk()).thenReturn("local");

        Response response = healthResource.health();

        assertEquals(503, response.getStatus());
        Map<String, Object> data = (Map<String, Object>) response.getEntity();
        assertEquals("DEGRADED", data.get("status"));

        Map<String, Map<String, String>> checks = (Map<String, Map<String, String>>) data.get("checks");
        assertEquals("ERROR", checks.get("storage").get("status"));
        assertTrue(checks.get("storage").get("message").contains("not writable"));
    }

    @Test
    void testHealth_StorageException() {
        when(storageProvider.checkHealth()).thenThrow(new RuntimeException("Storage Error"));

        Response response = healthResource.health();

        Map<String, Object> data = (Map<String, Object>) response.getEntity();
        Map<String, Map<String, String>> checks = (Map<String, Map<String, String>>) data.get("checks");
        assertEquals("ERROR", checks.get("storage").get("status"));
        assertEquals("Storage Error", checks.get("storage").get("message"));
    }
}
