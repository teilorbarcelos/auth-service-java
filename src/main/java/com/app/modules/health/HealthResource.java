package com.app.modules.health;

import io.quarkus.redis.datasource.RedisDataSource;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

import java.lang.management.ManagementFactory;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Health check resource — verifies DB and Redis connectivity.
 * Equivalent to HealthController.php
 */
@Path("/health")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Health")
public class HealthResource implements HealthSchemas.Doc {
    private static final String CONNECTED = "Connected";


    private static final Logger LOG = Logger.getLogger(HealthResource.class);

    @Inject
    EntityManager em;

    @Inject
    RedisDataSource redisDataSource;

    @Inject
    com.app.infrastructure.messaging.RabbitMQProvider rabbitMQProvider;

    @Inject
    com.app.infrastructure.storage.StorageProvider storageProvider;

    @ConfigProperty(name = "app.version", defaultValue = "1.0.0")
    String appVersion;

    @GET
    public Response health() {
        String status = "UP";

        Map<String, Map<String, String>> checks = new LinkedHashMap<>();
        checks.put("database", checkDatabase());
        checks.put("redis", checkRedis());
        checks.put("rabbitmq", checkRabbitMQ());
        checks.put("storage", checkStorage());

        for (Map.Entry<String, Map<String, String>> entry : checks.entrySet()) {
            String checkStatus = entry.getValue().get("status");
            if (!"OK".equals(checkStatus) && !"DISABLED".equals(checkStatus)) {
                status = "DEGRADED";
                LOG.warnv("System Health Degraded: {0} is down - {1}",
                        entry.getKey(), entry.getValue().get("message"));
            }
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("status", status);
        data.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        data.put("deploy", Map.of("version", appVersion));
        data.put("uptime", getUptime());
        data.put("checks", checks);
        data.put("message", "API is running smoothly. All systems operational.");

        return Response.status("UP".equals(status) ? 200 : 503).entity(data).build();
    }

    private Map<String, String> checkDatabase() {
        try {
            em.createNativeQuery("SELECT 1").getSingleResult();
            return Map.of("status", "OK", "message", CONNECTED);
        } catch (Exception e) {
            return Map.of("status", "ERROR", "message", e.getMessage());
        }
    }

    private Map<String, String> checkRedis() {
        try {
            redisDataSource.value(String.class).get("health-check-ping");
            return Map.of("status", "OK", "message", CONNECTED);
        } catch (Exception e) {
            return Map.of("status", "ERROR", "message", e.getMessage());
        }
    }

    private Map<String, String> checkRabbitMQ() {
        if (!rabbitMQProvider.isEnabled()) {
            return Map.of("status", "DISABLED", "message", "Messaging is disabled in settings");
        }

        if (rabbitMQProvider.isConnected()) {
            return Map.of("status", "OK", "message", CONNECTED);
        }
        return Map.of("status", "ERROR", "message", "Rabbit Connection Failed");
    }

    private Map<String, String> checkStorage() {
        try {
            if (storageProvider.checkHealth()) {
                return Map.of("status", "OK", "message", "Storage [" + storageProvider.getActiveDisk() + "] is healthy");
            }
            return Map.of("status", "ERROR", "message", "Storage [" + storageProvider.getActiveDisk() + "] is not writable");
        } catch (Exception e) {
            return Map.of("status", "ERROR", "message", e.getMessage());
        }
    }

    private String getUptime() {
        long uptimeMs = ManagementFactory.getRuntimeMXBean().getUptime();
        long seconds = uptimeMs / 1000;
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        return String.format("%dd %dh %dm %ds", days, hours, minutes, secs);
    }
}
