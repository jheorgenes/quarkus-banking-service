package br.com.alura.controller;

import br.com.alura.domain.Agencia;
import br.com.alura.service.AgenciaService;
import io.smallrye.common.annotation.NonBlocking;
import io.smallrye.faulttolerance.api.RateLimit;
import io.smallrye.mutiny.Uni;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.UriInfo;
import org.jboss.resteasy.reactive.RestResponse;

@Path("/agencias")
public class AgenciaController {

    private final AgenciaService agenciaService;

    AgenciaController(AgenciaService agenciaService) {
        this.agenciaService = agenciaService;
    }

    @POST
    @NonBlocking
    @Transactional
    public Uni<RestResponse<Void>> cadastrar(Agencia agencia, @Context UriInfo uriInfo) {
        return this.agenciaService.cadastrar(agencia)
                .replaceWith(RestResponse.created(uriInfo.getAbsolutePathBuilder().build()));
    }

    @GET
    @RateLimit(value = 5, window = 10) //Define a quantidade de requisições por validação
    @Path("{id}")
    public Uni<RestResponse<Agencia>> buscarPorId(Long id) {
        return agenciaService.buscarPorId(id).onItem().transform(RestResponse::ok);
    }

    @DELETE
    @Path("{id}")
    @Transactional
    public Uni<RestResponse<Void>> deletar(Long id) {
        return agenciaService.deletar(id).replaceWith(RestResponse.ok());
    }

    @PUT
    @Path("{id}")
    @Transactional
    public Uni<RestResponse<Void>> alterar(Agencia agencia) {
        return agenciaService.alterar(agencia).replaceWith(RestResponse.ok());
    }
}
