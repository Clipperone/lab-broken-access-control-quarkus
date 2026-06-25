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
@Path("/scientist/appointment")
@RolesAllowed({ "admin", "user", "guest" })
public class AppointmentResource {

    static final long CANCELLATION_LIMIT_HOURS = 24;

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
        // VULNERABILITY: (9a) la clausola admin ha perso l'isolamento per ufficio (Objects.equals(currentOffice(), a.getOffice())):
        //   un admin di QUALSIASI ufficio vede l'appuntamento.
        // VULNERABILITY: (9b) è stata aggiunta una clausola che concede visibilità a CHIUNQUE dello stesso ufficio,
        //   non solo a creatore/scienziato/admin: visibilità relationship-based troppo ampia.
        // La soluzione è: creatore OPPURE scienziato destinatario OPPURE (admin E stesso ufficio).
        return me.equals(a.getCreatorUpn())
                || me.equals(a.getScientistUpn())
                || isAdmin()
                || Objects.equals(currentOffice(), a.getOffice());
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
        // VULNERABILITY: (9h) manca il controllo dell'ORIZZONTE MASSIMO di prenotazione: una data troppo lontana
        // (oltre 1 anno) dovrebbe essere respinta con 422. Va ripristinato il check appointmentAt <= now + N giorni.
        // VULNERABILITY: (9g) manca il controllo ANTI DOPPIA-PRENOTAZIONE: lo stesso scienziato non dovrebbe avere
        // due appuntamenti nello stesso slot (409). Va ripristinato repository.hasConflict(...).
        Appointment a = new Appointment();
        a.setUuid(UUID.randomUUID().toString());
        // il creatore deriva dall'identità (default server-side)
        a.setCreatorUpn(currentUpn());
        // VULNERABILITY: (9e) MASS ASSIGNMENT: 'creatorUpn' è server-managed, ma se presente nel DTO viene
        // accettato dal client, sovrascrivendo l'identità. La soluzione è non esporlo nel DTO e impostarlo lato server.
        if (request.getCreatorUpn() != null) {
            a.setCreatorUpn(request.getCreatorUpn());
        }
        a.setScientistUpn(request.getScientistUpn());
        // l'ufficio di default deriva dallo scienziato (registro)
        Scientist scientist = this.scientistRepository.findByUpn(request.getScientistUpn());
        a.setOffice(scientist != null ? scientist.getOffice() : null);
        // VULNERABILITY: (9i) MASS ASSIGNMENT: se il client invia 'office' sovrascrive quello derivato dallo
        // scienziato (salto di tenant); manca inoltre la validazione (scienziato inesistente dovrebbe dare 422).
        // La soluzione è derivare SEMPRE l'ufficio dal registro, ignorare l'office del client e respingere lo
        // scienziato ignoto con 422.
        if (request.getOffice() != null) {
            a.setOffice(request.getOffice());
        }
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
        // VULNERABILITY: (9f) appuntamento inesistente -> 404: lo rende enumerabile (404 vs 403). La soluzione
        // è restituire FORBIDDEN identico al caso non visibile.
        if (a == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        if (!canView(a)) {
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
        // VULNERABILITY: (9d) manca il controllo di ownership (solo il creatore può cancellare): chiunque
        // cancella l'appuntamento altrui. La soluzione è il guard isCreator(a). Si mantiene il 403 per
        // appuntamento inesistente (anti-enumeration).
        if (a == null) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        // VULNERABILITY: (9c) manca l'AUTORIZZAZIONE TEMPORALE: la cancellazione deve essere consentita solo
        // se mancano più di 24h all'appuntamento (limite = appointmentAt - 24h, e ora < limite). Va ripristinata.
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
        // VULNERABILITY: (9d) manca il controllo che SOLO il creatore possa spostare: chiunque sposta
        // l'appuntamento altrui. La soluzione è il guard isCreator(a). Si mantiene il 403 per inesistente.
        if (a == null) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        // VULNERABILITY: (9h) manca il controllo dell'orizzonte massimo anche sullo spostamento (-> 422).
        // VULNERABILITY: (9g) manca il controllo anti doppia-prenotazione sullo slot di destinazione (-> 409).
        a.setAppointmentAt(request.getNewAppointmentAt());
        a.persistAndFlush();
        return Response.status(Response.Status.OK).entity(a.toDTO()).build();
    }

}
