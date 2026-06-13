package com.app.modules.role;

import com.app.core.BaseResource;
import com.app.infrastructure.auth.Authenticated;
import com.app.infrastructure.auth.ResourceFeature;
import com.app.infrastructure.auth.RequiresPermission;
import com.app.modules.audit.Audited;
import com.app.modules.feature.FeatureModel;
import com.app.modules.feature.FeatureRepository;
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
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import java.util.List;

@Path("/v1/role")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Roles")
@Authenticated
@ResourceFeature("role")
@Audited
public class RoleResource extends BaseResource<RoleModel, RoleService> {

    @Inject
    FeatureRepository featureRepository;

    @Inject
    public RoleResource(RoleService roleService) {
        this.service = roleService;
    }

    @GET
    @Path("/features")
    @RequiresPermission(action = "view")
    @Operation(summary = "List all available features", description = "Returns a list of all features that can be assigned to roles.")
    @APIResponse(responseCode = "200", description = "Success", content = @Content(schema = @Schema(implementation = FeatureModel.class, type = org.eclipse.microprofile.openapi.annotations.enums.SchemaType.ARRAY)))
    public Response listFeatures() {
        List<FeatureModel> features = featureRepository.listAll();
        return Response.ok(features).build();
    }

    @GET
    @Override
    @Operation(summary = "List roles with pagination and filters")
    @APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = RoleSchemas.ListResponseDto.class)))
    public Response listItems(@Context UriInfo uriInfo) {
        return super.listItems(uriInfo);
    }

    @GET
    @Path("/all")
    @Override
    @Operation(summary = "List all roles without pagination")
    @APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = RoleSchemas.ListResponseDto.class)))
    public Response listAllItems(@Context UriInfo uriInfo) {
        return super.listAllItems(uriInfo);
    }

    @GET
    @Path("/{id}")
    @Override
    @Operation(summary = "Get role by ID")
    @APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = RoleSchemas.ResponseDto.class)))
    public Response getById(@PathParam("id") String id) {
        return super.getById(id);
    }

    @POST
    @Override
    @Operation(summary = "Create a new role")
    @APIResponse(responseCode = "201", content = @Content(schema = @Schema(implementation = RoleSchemas.ResponseDto.class)))
    public Response create(@RequestBody(content = @Content(schema = @Schema(implementation = RoleSchemas.RequestDto.class))) RoleModel entity) {
        return super.create(entity);
    }

    @PUT
    @Path("/{id}")
    @Override
    @Operation(summary = "Update an existing role")
    @APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = RoleSchemas.ResponseDto.class)))
    public Response update(@PathParam("id") String id, @RequestBody(content = @Content(schema = @Schema(implementation = RoleSchemas.RequestDto.class))) RoleModel entity) {
        return super.update(id, entity);
    }

    @DELETE
    @Path("/{id}")
    @Override
    @Operation(summary = "Delete a role")
    @APIResponse(responseCode = "204")
    public Response delete(@PathParam("id") String id) {
        return super.delete(id);
    }

    @PATCH
    @Path("/{id}/status")
    @RequiresPermission(action = "activate")
    @Operation(summary = "Toggle role status")
    @APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = RoleSchemas.ResponseDto.class)))
    public Response toggleStatus(@PathParam("id") String id, RoleSchemas.StatusRequest body) {
        return super.internalToggleStatus(id, body != null ? body.active() : null);
    }
}
