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
import org.fugerit.java.demo.lab.broken.access.control.dto.OfficeDocumentRequestDTO;
import org.fugerit.java.demo.lab.broken.access.control.dto.ShareRequestDTO;
import org.fugerit.java.demo.lab.broken.access.control.persistence.OfficeDocument;
import org.fugerit.java.demo.lab.broken.access.control.persistence.OfficeDocumentRepository;
import org.fugerit.java.demo.lab.broken.access.control.security.RoleHierarchy;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * SCENARIO 3 — Autorizzazione multi-tenant (per ufficio) + gerarchia di ruoli, con sharing e stati.
 *
 * <p>
 * Identità presa dal token: upn ({@code SecurityIdentity}), ufficio (claim {@code office}), ruoli.
 * Owner/ufficio/ruolo del documento sono impostati lato server alla creazione (niente mass assignment).
 * </p>
 *
 * <ul>
 * <li><b>Lettura:</b> owner, OPPURE condiviso esplicitamente, OPPURE (documento PUBLISHED e stesso
 * ufficio e ruolo ≥ a quello dell'owner).</li>
 * <li><b>Modifica/Cancellazione:</b> owner, OPPURE admin dello STESSO ufficio su documento PUBLISHED.</li>
 * <li><b>Isolamento di tenant assoluto:</b> da un ufficio diverso → 403 anche per un admin.</li>
 * <li><b>Anti-enumeration:</b> non autorizzato o inesistente → 403 identico (mai 404).</li>
 * <li><b>Draft:</b> una bozza è visibile/modificabile solo dall'owner finché non è pubblicata.</li>
 * </ul>
 */
@Slf4j
@ApplicationScoped
@Path("/doc/officedoc")
@RolesAllowed({ "admin", "user", "guest" })
public class OfficeDocumentResource {

    OfficeDocumentRepository repository;

    SecurityIdentity securityIdentity;

    JsonWebToken jwt;

    public OfficeDocumentResource(OfficeDocumentRepository repository, SecurityIdentity securityIdentity,
            JsonWebToken jwt) {
        this.repository = repository;
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

    /** Lettura: owner, oppure condiviso, oppure (PUBLISHED + stesso ufficio + ruolo ≥ owner). */
    private boolean canRead(OfficeDocument doc) {
        boolean owner = currentUpn().equals(doc.getOwnerUpn());
        boolean shared = doc.getSharedWith().contains(currentUpn());
        // VULNERABILITY: (8a) manca il vincolo che il documento sia PUBLISHED (le bozze devono restare private all'owner);
        // VULNERABILITY: (8b) manca l'isolamento per ufficio (Objects.equals(currentOffice(), doc.getOwnerOffice()));
        // VULNERABILITY: (8c) manca il vincolo di gerarchia di ruolo (RoleHierarchy.isAtLeast(ruoli, doc.getOwnerRole())).
        // Risultato: qualsiasi utente autenticato legge qualsiasi documento. La soluzione è ripristinare le tre condizioni in AND.
        boolean officeRule = true;
        return owner || shared || officeRule;
    }

    /** Modifica/cancellazione: owner, oppure admin dello stesso ufficio su documento PUBLISHED. */
    private boolean canWrite(OfficeDocument doc) {
        boolean owner = currentUpn().equals(doc.getOwnerUpn());
        // VULNERABILITY: (8b) manca l'isolamento per ufficio (Objects.equals(currentOffice(), doc.getOwnerOffice()));
        // VULNERABILITY: (8d) manca il vincolo che il chiamante non-owner sia admin (isAdmin()).
        // Risultato: chiunque può modificare/cancellare un documento PUBLISHED. La soluzione è richiedere stesso ufficio E admin.
        boolean officeAdminRule = OfficeDocument.STATUS_PUBLISHED.equals(doc.getStatus());
        return owner || officeAdminRule;
    }

    @Operation(operationId = "createOfficeDoc", summary = "Crea un documento di ufficio (owner/ufficio/ruolo dal token, stato DRAFT)")
    @Tag(name = "office-document")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public Response create(@Valid OfficeDocumentRequestDTO request) {
        OfficeDocument doc = new OfficeDocument();
        doc.setUuid(UUID.randomUUID().toString());
        // owner, ufficio e ruolo soglia derivano dall'identità (default server-side)
        doc.setOwnerUpn(currentUpn());
        doc.setOwnerOffice(currentOffice());
        doc.setOwnerRole(RoleHierarchy.highestRoleCode(this.securityIdentity.getRoles()));
        doc.setStatus(OfficeDocument.STATUS_DRAFT);
        // VULNERABILITY: (8e) MASS ASSIGNMENT: owner/ufficio/ruolo/stato sono campi server-managed, ma se
        // presenti nel DTO vengono accettati dal client, sovrascrivendo i valori derivati dall'identità.
        // La soluzione è NON esporre questi campi nel DTO e impostarli sempre e solo lato server.
        if (request.getOwnerUpn() != null) {
            doc.setOwnerUpn(request.getOwnerUpn());
        }
        if (request.getOwnerOffice() != null) {
            doc.setOwnerOffice(request.getOwnerOffice());
        }
        if (request.getOwnerRole() != null) {
            doc.setOwnerRole(request.getOwnerRole());
        }
        if (request.getStatus() != null) {
            doc.setStatus(request.getStatus());
        }
        doc.setFileName(request.getFileName());
        doc.setContent(request.getContent());
        doc.persistAndFlush();
        return Response.status(Response.Status.CREATED).entity(doc.toDTO()).build();
    }

    @Operation(operationId = "listOfficeDocs", summary = "Elenca i documenti visibili al chiamante")
    @Tag(name = "office-document")
    @GET
    @Path("/list")
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public Response list() {
        List<Object> visible = this.repository.findAllOrdered().stream().filter(this::canRead)
                .map(d -> (Object) d.toDTO()).toList();
        return Response.status(Response.Status.OK).entity(visible).build();
    }

    @Operation(operationId = "readOfficeDoc", summary = "Legge un documento (regole multi-tenant)")
    @Tag(name = "office-document")
    @GET
    @Path("/{uuid}")
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public Response read(@PathParam("uuid") String uuid) {
        OfficeDocument doc = this.repository.findByUuid(uuid);
        // VULNERABILITY: (8f) documento inesistente -> 404: lo rende enumerabile (404 vs 403). La soluzione
        // è restituire FORBIDDEN identico al caso non leggibile.
        if (doc == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        if (!canRead(doc)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        return Response.status(Response.Status.OK).entity(doc.toDTO()).build();
    }

    @Operation(operationId = "editOfficeDoc", summary = "Modifica un documento (owner o admin stesso ufficio su PUBLISHED)")
    @Tag(name = "office-document")
    @PUT
    @Path("/{uuid}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public Response update(@PathParam("uuid") String uuid, @Valid OfficeDocumentRequestDTO request) {
        OfficeDocument doc = this.repository.findByUuid(uuid);
        if (doc == null || !canWrite(doc)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        doc.setFileName(request.getFileName());
        doc.setContent(request.getContent());
        doc.persistAndFlush();
        return Response.status(Response.Status.OK).entity(doc.toDTO()).build();
    }

    @Operation(operationId = "deleteOfficeDoc", summary = "Cancella un documento (owner o admin stesso ufficio su PUBLISHED)")
    @Tag(name = "office-document")
    @DELETE
    @Path("/{uuid}")
    @Transactional
    public Response delete(@PathParam("uuid") String uuid) {
        OfficeDocument doc = this.repository.findByUuid(uuid);
        if (doc == null || !canWrite(doc)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        doc.delete();
        return Response.status(Response.Status.OK).build();
    }

    @Operation(operationId = "publishOfficeDoc", summary = "Pubblica un documento (solo owner)")
    @Tag(name = "office-document")
    @PUT
    @Path("/{uuid}/publish")
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public Response publish(@PathParam("uuid") String uuid) {
        OfficeDocument doc = this.repository.findByUuid(uuid);
        // solo l'owner può pubblicare; anti-enumeration uniforme
        if (doc == null || !currentUpn().equals(doc.getOwnerUpn())) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        doc.setStatus(OfficeDocument.STATUS_PUBLISHED);
        doc.persistAndFlush();
        return Response.status(Response.Status.OK).entity(doc.toDTO()).build();
    }

    @Operation(operationId = "shareOfficeDoc", summary = "Condivide un documento con un altro utente (solo owner)")
    @Tag(name = "office-document")
    @POST
    @Path("/{uuid}/share")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public Response share(@PathParam("uuid") String uuid, @Valid ShareRequestDTO request) {
        OfficeDocument doc = this.repository.findByUuid(uuid);
        // solo l'owner può condividere
        if (doc == null || !currentUpn().equals(doc.getOwnerUpn())) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        doc.getSharedWith().add(request.getTargetUpn());
        doc.persistAndFlush();
        return Response.status(Response.Status.OK).entity(doc.toDTO()).build();
    }

}
