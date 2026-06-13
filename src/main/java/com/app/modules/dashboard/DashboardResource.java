package com.app.modules.dashboard;

import com.app.infrastructure.auth.Authenticated;
import com.app.infrastructure.auth.ResourceFeature;
import com.app.infrastructure.auth.RequiresPermission;
import com.app.modules.audit.Audited;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/v1/dashboard")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Dashboard")
@Authenticated
@ResourceFeature("dashboard")
public class DashboardResource {

    private final DashboardService dashboardService;

    @Inject
    public DashboardResource(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GET
    @Path("/stats")
    @RequiresPermission(action = "view")
    @Operation(summary = "Get dashboard statistics and aggregations")
    @APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = DashboardSchemas.DashboardStatsResponseDto.class)))
    public Response getStats(
            @QueryParam("createdAt_start") String createdAtStart,
            @QueryParam("createdAt_end") String createdAtEnd) {
        
        DashboardSchemas.DashboardStatsResponseDto stats = dashboardService.getStats(createdAtStart, createdAtEnd);
        return Response.ok(stats).build();
    }
}
