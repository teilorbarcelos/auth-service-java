package com.app.modules;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Map;

/**
 * Root endpoint — shows API info.
 */
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class RootResource {

    @ConfigProperty(name = "app.version", defaultValue = "1.0.0")
    String version;

    @GET
    public Response root() {
        return Response.ok(Map.of(
                "name", "Backend Java Quarkus",
                "version", version,
                "message", "API is running"
        )).build();
    }
}
