package com.app.modules.debug;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import com.app.infrastructure.auth.Authenticated;

@Path("/v1/debug")
@Produces(MediaType.APPLICATION_JSON)
public class DebugResource {

    @GET
    @Path("/error")
    @Authenticated
    public Response triggerError() {
        throw new RuntimeException("ESTE É UM ERRO DE TESTE PARA A TABELA TB_ERROR_LOG");
    }
}
