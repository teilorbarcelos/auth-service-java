package com.app.core;

import com.app.core.dto.PaginatedResponse;
import com.app.infrastructure.auth.RequiresPermission;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.util.HashMap;
import java.util.Map;

/**
 * Base REST resource with full CRUD endpoints.
 * Equivalent to BaseController.php
 */
public abstract class BaseResource<E extends BaseEntity, S extends BaseService<E, ?>> {

    protected S service;

    @GET
    @RequiresPermission(action = "view")
    public Response listItems(@Context UriInfo uriInfo) {
        Map<String, String> params = extractQueryParams(uriInfo);
        PaginatedResponse<E> result = service.listItems(params, true);
        return Response.ok(result).build();
    }

    @GET
    @Path("/all")
    @RequiresPermission(action = "view")
    public Response listAllItems(@Context UriInfo uriInfo) {
        Map<String, String> params = extractQueryParams(uriInfo);
        PaginatedResponse<E> result = service.listAllItems(params);
        return Response.ok(result).build();
    }

    @GET
    @Path("/{id}")
    @RequiresPermission(action = "view")
    public Response getById(@PathParam("id") String id) {
        E entity = service.retrieveById(id);
        if (entity == null) {
            return Response.status(404).entity(Map.of("message", "Record not found")).build();
        }
        return Response.ok(entity).build();
    }

    @POST
    @RequiresPermission(action = "create")
    public Response create(@Valid E entity) {
        E result = service.create(entity);
        return Response.status(201).entity(result).build();
    }

    @PUT
    @Path("/{id}")
    @RequiresPermission(action = "create")
    public Response update(@PathParam("id") String id, @Valid E entity) {
        E result = service.update(id, entity);
        if (result == null) {
            return Response.status(404).entity(Map.of("message", "Record not found")).build();
        }
        return Response.ok(result).build();
    }

    @DELETE
    @Path("/{id}")
    @RequiresPermission(action = "delete")
    public Response delete(@PathParam("id") String id) {
        boolean success = service.delete(id);
        if (!success) {
            return Response.status(404).entity(Map.of("message", "Record not found")).build();
        }
        return Response.noContent().build();
    }

    protected Response internalToggleStatus(String id, Boolean active) {
        if (active == null)
            active = true;
        E result = service.setStatus(id, active);
        if (result == null) {
            return Response.status(404).entity(Map.of("message", "Record not found")).build();
        }
        return Response.ok(result).build();
    }

    protected Map<String, String> extractQueryParams(UriInfo uriInfo) {
        Map<String, String> params = new HashMap<>();
        uriInfo.getQueryParameters().forEach((key, values) -> {
            if (!values.isEmpty()) {
                params.put(key, values.getFirst());
            }
        });
        return params;
    }
}
