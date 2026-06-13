package com.app.modules.feature;

import com.app.core.BaseResource;
import com.app.infrastructure.auth.Authenticated;
import com.app.infrastructure.auth.ResourceFeature;
import com.app.infrastructure.auth.RequiresPermission;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 * Feature REST resource — read-only.
 * Equivalent to FeatureController.php
 */
@Path("/v1/feature")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Features")
@Authenticated
@ResourceFeature("feature")
public class FeatureResource extends BaseResource<FeatureModel, FeatureService> {

    @Inject
    public FeatureResource(FeatureService featureService) {
        this.service = featureService;
    }

    @GET
    @Override
    @Operation(summary = "List features with pagination and filters")
    @APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = FeatureSchemas.ListResponseDto.class)))
    public Response listItems(@Context UriInfo uriInfo) {
        return super.listItems(uriInfo);
    }

    @GET
    @Path("/all")
    @Override
    @Operation(summary = "List all features without pagination")
    @APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = FeatureSchemas.ListResponseDto.class)))
    public Response listAllItems(@Context UriInfo uriInfo) {
        return super.listAllItems(uriInfo);
    }

    @GET
    @Path("/{id}")
    @Override
    @Operation(summary = "Get feature by ID")
    @APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = FeatureSchemas.ResponseDto.class)))
    public Response getById(@PathParam("id") String id) {
        return super.getById(id);
    }

    @PATCH
    @Path("/{id}/status")
    @RequiresPermission(action = "activate")
    @Operation(summary = "Toggle feature status")
    @APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = FeatureSchemas.ResponseDto.class)))
    public Response toggleStatus(@PathParam("id") String id, FeatureSchemas.StatusRequest body) {
        return super.internalToggleStatus(id, body != null ? body.active() : null);
    }
}
