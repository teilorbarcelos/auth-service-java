package com.app.modules.product;

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

import com.app.infrastructure.auth.UserSession;

@Path("/v1/product")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Products")
@Authenticated
@ResourceFeature("product")
@Audited
public class ProductResource extends BaseResource<ProductModel, ProductService> {

    private final UserSession userSession;

    @Inject
    public ProductResource(ProductService productService, UserSession userSession) {
        this.service = productService;
        this.userSession = userSession;
    }

    @GET
    @Override
    @Operation(summary = "List products with pagination and filters")
    @APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = ProductSchemas.ListResponseDto.class)))
    public Response listItems(@Context UriInfo uriInfo) {
        return super.listItems(uriInfo);
    }

    @GET
    @Path("/all")
    @Override
    @Operation(summary = "List all products without pagination")
    @APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = ProductSchemas.ListResponseDto.class)))
    public Response listAllItems(@Context UriInfo uriInfo) {
        return super.listAllItems(uriInfo);
    }

    @GET
    @Path("/{id}")
    @Override
    @Operation(summary = "Get product by ID")
    @APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = ProductSchemas.ResponseDto.class)))
    public Response getById(@PathParam("id") String id) {
        return super.getById(id);
    }

    @POST
    @Override
    @Operation(summary = "Create a new product")
    @APIResponse(responseCode = "201", content = @Content(schema = @Schema(implementation = ProductSchemas.ResponseDto.class)))
    public Response create(
            @RequestBody(content = @Content(schema = @Schema(implementation = ProductSchemas.RequestDto.class))) ProductModel entity) {
        entity.setIdUser(userSession.getUserId());
        return super.create(entity);
    }

    @PUT
    @Path("/{id}")
    @Override
    @Operation(summary = "Update an existing product")
    @APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = ProductSchemas.ResponseDto.class)))
    public Response update(@PathParam("id") String id,
            @RequestBody(content = @Content(schema = @Schema(implementation = ProductSchemas.RequestDto.class))) ProductModel entity) {
        return super.update(id, entity);
    }

    @DELETE
    @Path("/{id}")
    @Override
    @Operation(summary = "Delete a product")
    @APIResponse(responseCode = "204")
    public Response delete(@PathParam("id") String id) {
        return super.delete(id);
    }

    @PATCH
    @Path("/{id}/status")
    @RequiresPermission(action = "activate")
    @Operation(summary = "Toggle product status")
    @APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = ProductSchemas.ResponseDto.class)))
    public Response toggleStatus(@PathParam("id") String id, ProductSchemas.StatusRequest body) {
        return super.internalToggleStatus(id, body != null ? body.active() : null);
    }
}
