package org.fugerit.java.demo.lab.broken.access.control;

import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.fugerit.java.demo.lab.broken.access.control.dto.AppointmentMoveRequestDTO;
import org.fugerit.java.demo.lab.broken.access.control.dto.AppointmentRequestDTO;
import org.fugerit.java.demo.lab.broken.access.control.persistence.Appointment;
import org.fugerit.java.demo.lab.broken.access.control.persistence.AppointmentRepository;
import org.fugerit.java.demo.lab.broken.access.control.persistence.Scientist;
import org.fugerit.java.demo.lab.broken.access.control.persistence.ScientistRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * SCENARIO APPUNTAMENTI — visibilità multi-parte (relationship-based) + autorizzazione temporale.
 *
 * <ul>
 * <li><b>Visibilità:</b> creatore, OPPURE scienziato destinatario, OPPURE admin dello STESSO ufficio.</li>
 * <li><b>Eliminazione:</b> solo il creatore e solo se mancano più di 24h all'appuntamento (regola temporale).</li>
 * <li><b>Spostamento:</b> solo il creatore (in qualsiasi momento).</li>
 * <li><b>Isolamento di tenant:</b> l'admin di un ufficio diverso non vede l'appuntamento.</li>
 * <li><b>Anti-enumeration:</b> inesistente o non visibile → 403 identico.</li>
 * </ul>
 */
@Slf4j
@ApplicationScoped
@Path("/doc/appointment")
@RolesAllowed({ "admin", "user", "guest" })
public class AppointmentResource {

    static final long CANCELLATION_LIMIT_HOURS = 24;

    // orizzonte massimo di prenotazione: non si può prenotare/spostare oltre questo numero di giorni
    static final long MAX_BOOKING_DAYS = 365;

    // 422 Unprocessable Entity: violazione di regola di business (assente da Response.Status)
    static final int UNPROCESSABLE_ENTITY = 422;

    AppointmentRepository repository;

    ScientistRepository scientistRepository;

    SecurityIdentity securityIdentity;

    JsonWebToken jwt;

    public AppointmentResource(AppointmentRepository repository, ScientistRepository scientistRepository,
            SecurityIdentity securityIdentity, JsonWebToken jwt) {
        this.repository = repository;
        this.scientistRepository = scientistRepository;
        this.securityIdentity = securityIdentity;
        this.jwt = jwt;
    }

    private String currentUpn() {
        return this.securityIdentity.getPrincipal().getName();
    }

    private String currentOffice() {
        return this.jwt.getClaim("office");
    }

    private boolean isAdmin() {
        return this.securityIdentity.getRoles().contains("admin");
    }

    /** Visibile a: creatore, scienziato destinatario, oppure admin dello stesso ufficio. */
    private boolean canView(Appointment a) {
        String me = currentUpn();
        return me.equals(a.getCreatorUpn())
                || me.equals(a.getScientistUpn())
                || (isAdmin() && Objects.equals(currentOffice(), a.getOffice()));
    }

    private boolean isCreator(Appointment a) {
        return currentUpn().equals(a.getCreatorUpn());
    }

    @Operation(operationId = "createAppointment", summary = "Prenota un appuntamento (creatore = utente autenticato)")
    @Tag(name = "appointment")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public Response create(@Valid AppointmentRequestDTO request) {
        // SOLUTION: (9h) orizzonte massimo - regola di business: non si prenota oltre MAX_BOOKING_DAYS giorni
        if (request.getAppointmentAt().isAfter(LocalDateTime.now().plusDays(MAX_BOOKING_DAYS))) {
            return Response.status(UNPROCESSABLE_ENTITY).build();
        }
        // SOLUTION: (9i) l'ufficio NON arriva dal client: si deriva dal registro degli scienziati (fonte di
        // verita server-side). Scienziato inesistente -> 422 (riferimento non valido).
        Scientist scientist = this.scientistRepository.findByUpn(request.getScientistUpn());
        if (scientist == null) {
            return Response.status(UNPROCESSABLE_ENTITY).build();
        }
        // SOLUTION: (9g) niente doppia prenotazione - stesso scienziato, stesso slot -> 409 Conflict
        if (this.repository.hasConflict(request.getScientistUpn(), request.getAppointmentAt(), "")) {
            return Response.status(Response.Status.CONFLICT).build();
        }
        Appointment a = new Appointment();
        a.setUuid(UUID.randomUUID().toString());
        // ANTI MASS-ASSIGNMENT: il creatore è l'identità autenticata, mai un dato del client
        a.setCreatorUpn(currentUpn());
        a.setScientistUpn(request.getScientistUpn());
        // SOLUTION: (9i) ufficio derivato dallo scienziato (server-side), mai dal client (anti salto di tenant)
        a.setOffice(scientist.getOffice());
        a.setAppointmentAt(request.getAppointmentAt());
        a.setSubject(request.getSubject());
        a.persistAndFlush();
        return Response.status(Response.Status.CREATED).entity(a.toDTO()).build();
    }

    @Operation(operationId = "listAppointments", summary = "Elenca gli appuntamenti visibili al chiamante")
    @Tag(name = "appointment")
    @GET
    @Path("/list")
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public Response list() {
        List<Object> visible = this.repository.findAllOrdered().stream().filter(this::canView)
                .map(a -> (Object) a.toDTO()).toList();
        return Response.status(Response.Status.OK).entity(visible).build();
    }

    @Operation(operationId = "readAppointment", summary = "Legge un appuntamento (creatore, destinatario o admin di ufficio)")
    @Tag(name = "appointment")
    @GET
    @Path("/{uuid}")
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public Response read(@PathParam("uuid") String uuid) {
        Appointment a = this.repository.findByUuid(uuid);
        if (a == null || !canView(a)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        return Response.status(Response.Status.OK).entity(a.toDTO()).build();
    }

    @Operation(operationId = "deleteAppointment", summary = "Elimina un appuntamento (solo creatore, e solo oltre 24h prima)")
    @Tag(name = "appointment")
    @DELETE
    @Path("/{uuid}")
    @Transactional
    public Response delete(@PathParam("uuid") String uuid) {
        Appointment a = this.repository.findByUuid(uuid);
        // ownership: solo il creatore. Inesistente/non creatore -> 403 uniforme (anti-enumeration)
        if (a == null || !isCreator(a)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        // AUTORIZZAZIONE TEMPORALE: consentita solo se mancano più di 24h all'appuntamento
        LocalDateTime limit = a.getAppointmentAt().minusHours(CANCELLATION_LIMIT_HOURS);
        if (!LocalDateTime.now().isBefore(limit)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        a.delete();
        return Response.status(Response.Status.OK).build();
    }

    @Operation(operationId = "moveAppointment", summary = "Sposta un appuntamento (solo creatore)")
    @Tag(name = "appointment")
    @PUT
    @Path("/{uuid}/move")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public Response move(@PathParam("uuid") String uuid, @Valid AppointmentMoveRequestDTO request) {
        Appointment a = this.repository.findByUuid(uuid);
        // solo il creatore può spostare (in qualsiasi momento)
        if (a == null || !isCreator(a)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        // SOLUTION: (9h) orizzonte massimo - regola di business: non si sposta oltre MAX_BOOKING_DAYS giorni
        if (request.getNewAppointmentAt().isAfter(LocalDateTime.now().plusDays(MAX_BOOKING_DAYS))) {
            return Response.status(UNPROCESSABLE_ENTITY).build();
        }
        // SOLUTION: (9g) niente doppia prenotazione - slot di destinazione occupato (escluso sé stesso) -> 409
        if (this.repository.hasConflict(a.getScientistUpn(), request.getNewAppointmentAt(), a.getUuid())) {
            return Response.status(Response.Status.CONFLICT).build();
        }
        a.setAppointmentAt(request.getNewAppointmentAt());
        a.persistAndFlush();
        return Response.status(Response.Status.OK).entity(a.toDTO()).build();
    }

}
