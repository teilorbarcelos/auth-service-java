package com.app.infrastructure.pdf;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.io.InputStream;

/**
 * MicroProfile Rest Client for the external PDF service.
 */
@RegisterRestClient(configKey = "pdf-service")
@Path("/v1/pdf")
public interface RemotePdfClient {

    @POST
    @Path("/generate")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces("application/pdf")
    InputStream generate(PdfRequestDTO request);
}
