package org.fugerit.java.demo.lab.broken.access.control;

import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.fugerit.java.demo.lab.broken.access.control.persistence.ScientistRepository;

import java.util.List;

/**
 * Registro degli scienziati: elenco di sola lettura usato per prenotare appuntamenti. L'ufficio
 * dell'appuntamento è derivato da questo registro lato server (mai dal client).
 */
@Slf4j
@ApplicationScoped
@Path("/scientist")
public class ScientistResource {

    ScientistRepository repository;

    public ScientistResource(ScientistRepository repository) {
        this.repository = repository;
    }

    @Operation(operationId = "listScientists", summary = "Elenca gli scienziati del registro (upn, ufficio)")
    @Tag(name = "scientist")
    @GET
    @Path("/list")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ "admin", "user", "guest" })
    public Response list() {
        List<Object> scientists = this.repository.findAllOrdered().stream().map(s -> (Object) s.toDTO()).toList();
        return Response.status(Response.Status.OK).entity(scientists).build();
    }

}
