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
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.fugerit.java.demo.lab.broken.access.control.dto.PersonalNoteRequestDTO;
import org.fugerit.java.demo.lab.broken.access.control.persistence.PersonalNote;
import org.fugerit.java.demo.lab.broken.access.control.persistence.PersonalNoteRepository;

import java.util.List;
import java.util.UUID;

/**
 * SCENARIO 1 — Ownership-based access control (dati personali).
 *
 * <p>
 * Una nota è di proprietà di chi la crea ({@code ownerUpn} preso dall'identità, non dal client):
 * </p>
 * <ul>
 * <li><b>Lettura/lista:</b> owner OPPURE admin.</li>
 * <li><b>Modifica/cancellazione:</b> SOLO owner (l'admin può vedere, non modificare).</li>
 * <li>Accesso negato o risorsa inesistente → <b>403 uniforme</b> (anti-enumeration di base).</li>
 * </ul>
 */
@Slf4j
@ApplicationScoped
@Path("/doc/note")
@RolesAllowed({ "admin", "user", "guest" })
public class PersonalNoteResource {

    PersonalNoteRepository repository;

    SecurityIdentity securityIdentity;

    public PersonalNoteResource(PersonalNoteRepository repository, SecurityIdentity securityIdentity) {
        this.repository = repository;
        this.securityIdentity = securityIdentity;
    }

    private String currentUpn() {
        return this.securityIdentity.getPrincipal().getName();
    }

    private boolean isAdmin() {
        return this.securityIdentity.getRoles().contains("admin");
    }

    @Operation(operationId = "createNote", summary = "Crea una nota personale (owner = utente autenticato)")
    @Tag(name = "personal-note")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public Response create(@Valid PersonalNoteRequestDTO request) {
        PersonalNote note = new PersonalNote();
        note.setUuid(UUID.randomUUID().toString());
        // OWNERSHIP: il proprietario è l'identità autenticata, mai un dato fornito dal client
        note.setOwnerUpn(currentUpn());
        note.setTitle(request.getTitle());
        note.setContent(request.getContent());
        note.persistAndFlush();
        return Response.status(Response.Status.CREATED).entity(note.toDTO()).build();
    }

    @Operation(operationId = "listNotes", summary = "Elenca le note visibili (le proprie; l'admin vede tutte)")
    @Tag(name = "personal-note")
    @GET
    @Path("/list")
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public Response list() {
        List<PersonalNote> notes = isAdmin() ? this.repository.findAllOrdered()
                : this.repository.findByOwner(currentUpn());
        return Response.status(Response.Status.OK).entity(notes.stream().map(PersonalNote::toDTO).toList()).build();
    }

    @Operation(operationId = "readNote", summary = "Legge una nota (owner o admin)")
    @Tag(name = "personal-note")
    @GET
    @Path("/{uuid}")
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public Response read(@PathParam("uuid") String uuid) {
        PersonalNote note = this.repository.findByUuid(uuid);
        // VULNERABILITY: (7c) nota inesistente -> 404: rende le note enumerabili (404 vs 403). La soluzione
        // è restituire FORBIDDEN identico al caso non autorizzato.
        if (note == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        // VULNERABILITY: (7a) OWNERSHIP read: manca il controllo di proprietà (owner o admin): chiunque
        // legge la nota di chiunque. La soluzione è consentire la lettura solo a owner OPPURE admin.
        return Response.status(Response.Status.OK).entity(note.toDTO()).build();
    }

    @Operation(operationId = "editNote", summary = "Modifica una nota (solo owner)")
    @Tag(name = "personal-note")
    @PUT
    @Path("/{uuid}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public Response update(@PathParam("uuid") String uuid, @Valid PersonalNoteRequestDTO request) {
        PersonalNote note = this.repository.findByUuid(uuid);
        // VULNERABILITY: (7b) OWNERSHIP write: manca il controllo che SOLO l'owner possa modificare (un
        // admin può leggere ma non modificare): chiunque modifica la nota altrui. La soluzione è il guard
        // sull'owner. Si mantiene il 403 per nota inesistente (anti-enumeration).
        if (note == null) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        note.setTitle(request.getTitle());
        note.setContent(request.getContent());
        note.persistAndFlush();
        return Response.status(Response.Status.OK).entity(note.toDTO()).build();
    }

    @Operation(operationId = "deleteNote", summary = "Cancella una nota (solo owner)")
    @Tag(name = "personal-note")
    @DELETE
    @Path("/{uuid}")
    @Transactional
    public Response delete(@PathParam("uuid") String uuid) {
        PersonalNote note = this.repository.findByUuid(uuid);
        if (note == null || !note.getOwnerUpn().equals(currentUpn())) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        note.delete();
        return Response.status(Response.Status.OK).build();
    }

}
