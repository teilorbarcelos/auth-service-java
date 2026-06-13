package com.app.modules.user;

import com.app.core.BaseResource;
import com.app.infrastructure.auth.Authenticated;
import com.app.infrastructure.auth.ResourceFeature;
import com.app.infrastructure.auth.RequiresPermission;
import com.app.modules.audit.Audited;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;

import java.io.InputStream;
import java.util.Map;
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

@Path("/v1/user")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Users")
@Authenticated
@ResourceFeature("user")
@Audited
public class UserResource extends BaseResource<UserModel, UserService> {

    @Inject
    public UserResource(UserService userService) {
        this.service = userService;
    }

    @GET
    @Override
    @Operation(summary = "List users with pagination and filters")
    @APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = UserSchemas.ListResponseDto.class)))
    public Response listItems(@Context UriInfo uriInfo) {
        return super.listItems(uriInfo);
    }

    @GET
    @Path("/all")
    @Override
    @Operation(summary = "List all users without pagination")
    @APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = UserSchemas.ListResponseDto.class)))
    public Response listAllItems(@Context UriInfo uriInfo) {
        return super.listAllItems(uriInfo);
    }

    @GET
    @Path("/{id}")
    @Override
    @Operation(summary = "Get user by ID")
    @APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = UserSchemas.ResponseDto.class)))
    public Response getById(@PathParam("id") String id) {
        return super.getById(id);
    }

    @POST
    @Override
    @Operation(summary = "Create a new user")
    @APIResponse(responseCode = "201", content = @Content(schema = @Schema(implementation = UserSchemas.ResponseDto.class)))
    public Response create(
            @RequestBody(content = @Content(schema = @Schema(implementation = UserSchemas.RequestDto.class))) UserModel entity) {
        return super.create(entity);
    }

    @PUT
    @Path("/{id}")
    @Override
    @Operation(summary = "Update an existing user")
    @APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = UserSchemas.ResponseDto.class)))
    public Response update(@PathParam("id") String id,
            @RequestBody(content = @Content(schema = @Schema(implementation = UserSchemas.RequestDto.class))) UserModel entity) {
        return super.update(id, entity);
    }

    @DELETE
    @Path("/{id}")
    @Override
    @Operation(summary = "Delete a user")
    @APIResponse(responseCode = "204")
    public Response delete(@PathParam("id") String id) {
        return super.delete(id);
    }

    @PATCH
    @Path("/{id}/status")
    @RequiresPermission(action = "activate")
    @Operation(summary = "Toggle user status")
    @APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = UserSchemas.ResponseDto.class)))
    public Response toggleStatus(@PathParam("id") String id, UserSchemas.StatusRequest body) {
        return super.internalToggleStatus(id, body != null ? body.active() : null);
    }

    @GET
    @Path("/export/pdf")
    @Produces("application/pdf")
    @RequiresPermission(action = "view")
    @Operation(summary = "Export users as PDF")
    @APIResponse(responseCode = "200", description = "PDF report generated successfully")
    public Response exportPdf(@Context UriInfo uriInfo) {
        Map<String, String> params = extractQueryParams(uriInfo);
        InputStream pdfStream = service.exportPdf(params);
        return Response.ok(pdfStream)
                .header("Content-Disposition", "attachment; filename=\"usuarios.pdf\"")
                .build();
    }
}
