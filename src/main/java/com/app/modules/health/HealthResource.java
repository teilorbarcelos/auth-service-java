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

    @ConfigProperty(name = "app.version", defaultValue = "1.0.0")
    String appVersion;

    @GET
    public Response health() {
        String status = "UP";

        Map<String, Map<String, String>> checks = new LinkedHashMap<>();
        checks.put("database", checkDatabase());
        checks.put("redis", checkRedis());

        for (Map.Entry<String, Map<String, String>> entry : checks.entrySet()) {
            String checkStatus = entry.getValue().get("status");
            if (!"OK".equals(checkStatus)) {
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

        return Response.status("UP".equals(status) ? 200 : 503).entity(data).build();
    }

    @GET
    @Path("/liveness")
    public Response liveness() {
        return Response.ok(Map.of("status", "alive", "uptime", ManagementFactory.getRuntimeMXBean().getUptime())).build();
    }

    @GET
    @Path("/ready")
    public Response readiness() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("status", "UP");
        data.put("database", checkDatabase().get("status").equals("OK"));
        data.put("redis", checkRedis().get("status").equals("OK"));

        boolean ok = (boolean) data.get("database") && (boolean) data.get("redis");
        return Response.status(ok ? 200 : 503).entity(data).build();
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
