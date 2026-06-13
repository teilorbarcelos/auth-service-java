package {{package}};

import com.app.core.BaseResource;
import com.app.infrastructure.auth.Authenticated;
import com.app.infrastructure.auth.ResourceFeature;
import com.app.infrastructure.auth.RequiresPermission;
import com.app.modules.audit.Audited;
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

@Path("/v1/{{module_snake}}")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "{{module_name}}s")
@Authenticated
@ResourceFeature("{{module_snake}}")
@Audited
public class {{module_name}}Resource extends BaseResource<{{module_name}}Model, {{module_name}}Service> {
    
    @Inject
    public {{module_name}}Resource({{module_name}}Service service) {
        this.service = service;
    }

    @GET
    @Override
    @Operation(summary = "List {{module_name_lower}}s with pagination and filters")
    @APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = {{module_name}}Schemas.ListResponseDto.class)))
    public Response listItems(@Context UriInfo uriInfo) {
        return super.listItems(uriInfo);
    }

    @GET
    @Path("/all")
    @Override
    @Operation(summary = "List all {{module_name_lower}}s without pagination")
    @APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = {{module_name}}Schemas.ListResponseDto.class)))
    public Response listAllItems(@Context UriInfo uriInfo) {
        return super.listAllItems(uriInfo);
    }

    @GET
    @Path("/{id}")
    @Override
    @Operation(summary = "Get {{module_name_lower}} by ID")
    @APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = {{module_name}}Schemas.ResponseDto.class)))
    @APIResponse(responseCode = "404", description = "{{module_name}} not found")
    public Response getById(@PathParam("id") String id) {
        return super.getById(id);
    }

    @POST
    @Override
    @Operation(summary = "Create a new {{module_name_lower}}")
    @APIResponse(responseCode = "201", content = @Content(schema = @Schema(implementation = {{module_name}}Schemas.ResponseDto.class)))
    public Response create(@RequestBody(content = @Content(schema = @Schema(implementation = {{module_name}}Schemas.RequestDto.class))) {{module_name}}Model entity) {
        return super.create(entity);
    }

    @PUT
    @Path("/{id}")
    @Override
    @Operation(summary = "Update an existing {{module_name_lower}}")
    @APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = {{module_name}}Schemas.ResponseDto.class)))
    public Response update(@PathParam("id") String id, @RequestBody(content = @Content(schema = @Schema(implementation = {{module_name}}Schemas.RequestDto.class))) {{module_name}}Model entity) {
        return super.update(id, entity);
    }

    @DELETE
    @Path("/{id}")
    @Override
    @Operation(summary = "Delete a {{module_name_lower}}")
    @APIResponse(responseCode = "204", description = "{{module_name}} deleted")
    public Response delete(@PathParam("id") String id) {
        return super.delete(id);
    }

    @PATCH
    @Path("/{id}/status")
    @RequiresPermission(action = "activate")
    @Operation(summary = "Toggle {{module_name_lower}} status")
    @APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = {{module_name}}Schemas.ResponseDto.class)))
    public Response toggleStatus(@PathParam("id") String id, {{module_name}}Schemas.StatusRequest body) {
        return super.internalToggleStatus(id, body != null ? body.active() : null);
    }
}
