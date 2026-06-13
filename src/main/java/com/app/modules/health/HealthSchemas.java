package com.app.modules.health;

import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import java.util.Map;

public class HealthSchemas {

    @Schema(name = "HealthResponse")
    public static class HealthResponseDto {
        @Schema(examples = "UP")
        public String status;
        @Schema(examples = "2026-05-01T20:00:00Z")
        public String deployTimestamp;
        public Map<String, Map<String, String>> checks;
    }

    public interface Doc {
        @Operation(summary = "Check system health status")
        @APIResponse(responseCode = "200", description = "System is healthy", content = @Content(schema = @Schema(implementation = HealthResponseDto.class)))
        @APIResponse(responseCode = "503", description = "System health is degraded")
        Response health();
    }
}
