package org.fugerit.java.demo.lab.broken.access.control;

import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;
import org.fugerit.java.demo.lab.broken.access.control.persistence.Person;
import org.fugerit.java.demo.lab.broken.access.control.persistence.PersonRepository;
import org.fugerit.java.doc.base.config.DocConfig;
import org.fugerit.java.doc.base.process.DocProcessContext;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Slf4j
@ApplicationScoped
@Path("/doc")
@Schema(description = "Servizio rest per la generazione di un documento che contiene una lista di persone in vari formati.")
@SecurityScheme(securitySchemeName = "SecurityScheme", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT", description = "JWT Bearer Token Authentication")
public class DocResource {

    DocHelper docHelper;

    PersonRepository personRepository;

    SecurityIdentity securityIdentity;

    public DocResource(DocHelper docHelper, PersonRepository personRepository, SecurityIdentity securityIdentity) {
        this.docHelper = docHelper;
        this.personRepository = personRepository;
        this.securityIdentity = securityIdentity;
    }

    @APIResponse(responseCode = "200", description = "The HTML document content")
    @APIResponse(responseCode = "401", description = "Se l'autenticazione non è presente")
    @APIResponse(responseCode = "403", description = "Se l'utente non è autorizzato per la risorsa")
    @APIResponse(responseCode = "500", description = "In caso di errori non gestiti")
    @Tag(name = "document")
    @Operation(operationId = "HTMLExample", summary = "Versione HTML del documento (ruoli: admin, user)", description = "Generato con Fugerti Venus Doc https://venusdocs.fugerit.org/")
    @GET
    @Produces("text/html")
    @Path("/example.html")
    @SecurityRequirement(name = "SecurityScheme")
    @RolesAllowed({ "admin", "user" })
    public Response htmlExample() throws IOException {
        return Response.status(Response.Status.OK).entity(processDocument(DocConfig.TYPE_HTML)).build();
    }

    @APIResponse(responseCode = "200", description = "The Markdown document content")
    @APIResponse(responseCode = "401", description = "Se l'autenticazione non è presente")
    @APIResponse(responseCode = "403", description = "Se l'utente non è autorizzato per la risorsa")
    @APIResponse(responseCode = "500", description = "In caso di errori non gestiti")
    @Tag(name = "document")
    @Operation(operationId = "MarkdownExample", summary = "Versione MarkDown del documento (ruoli: admin, user, guest)", description = "Generato con Fugerti Venus Doc https://venusdocs.fugerit.org/")
    @GET
    @Produces("text/markdown")
    @Path("/example.md")
    @SecurityRequirement(name = "SecurityScheme")
    // VULNERABILITY: (5) risolvi questa vulnerabilità in modo che il caso di test funzioni: l'endpoint è
    // ad accesso pubblico (@PermitAll), va protetto con l'elenco dei ruoli previsti dalle specifiche
    @PermitAll
    public Response markdownExample() throws IOException {
        return Response.status(Response.Status.OK).entity(processDocument(DocConfig.TYPE_MD)).build();
    }

    @APIResponse(responseCode = "200", description = "The AsciiDoc document content")
    @APIResponse(responseCode = "401", description = "Se l'autenticazione non è presente")
    @APIResponse(responseCode = "403", description = "Se l'utente non è autorizzato per la risorsa")
    @APIResponse(responseCode = "500", description = "In caso di errori non gestiti")
    @Tag(name = "document")
    @Operation(operationId = "AsciiDocExample", summary = "Versione AsciiDoc del documento (ruoli: admin)", description = "Generato con Fugerti Venus Doc https://venusdocs.fugerit.org/")
    @GET
    @Produces("text/asciidoc")
    @Path("/example.adoc")
    @SecurityRequirement(name = "SecurityScheme")
    @RolesAllowed("admin")
    public Response asciidocExample() throws IOException {
        return Response.status(Response.Status.OK).entity(processDocument(DocConfig.TYPE_ADOC)).build();
    }

    @APIResponse(responseCode = "200", description = "The PDF document content")
    @APIResponse(responseCode = "401", description = "Se l'autenticazione non è presente")
    @APIResponse(responseCode = "403", description = "Se l'utente non è autorizzato per la risorsa")
    @APIResponse(responseCode = "500", description = "In caso di errori non gestiti")
    @Tag(name = "document")
    @Operation(operationId = "PDFExample", summary = "Versione AsciiDoc del documento (ruoli: admin)", description = "Generato con Fugerti Venus Doc https://venusdocs.fugerit.org/")
    @GET
    @Produces("application/pdf")
    @Path("/example.pdf")
    @RolesAllowed("admin")
    public Response pdfExample() throws IOException {
        return Response.status(Response.Status.OK).entity(processDocument(DocConfig.TYPE_PDF)).build();
    }

    /*
     * metodo worker che genera effettivamente i documenti tramite il framework :
     * https://github.com/fugerit-org/fj-doc ( documentazione : https://venusdocs.fugerit.org/ )
     */
    byte[] processDocument(String handlerId) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            List<Person> personsFromDb = this.listAllPersons();

            // Converti le entità Person in oggetti People per il template
            List<People> listPeople = personsFromDb.stream()
                    .map(person -> new People(
                            person.getFirstName(),
                            person.getLastName(),
                            person.getTitle(),
                            person.getUuid()))
                    .toList();

            log.info("processDocument handlerId : {}", handlerId);
            String chainId = "document";
            // output generation
            this.docHelper.getDocProcessConfig().fullProcess(chainId, DocProcessContext.newContext("listPeople", listPeople),
                    handlerId, baos);
            // return the output
            return baos.toByteArray();
        }
    }

    /*
     * metodo che carica tutte le persone cui l'utente corrente ha accesso.
     * NOTA: duplicato in PersonResource.listAllPersons() — mantenere sincronizzati.
     */
    private List<Person> listAllPersons() {
        Set<String> userRoles = this.securityIdentity.getRoles();
        log.info("user : {}, roles : {}", this.securityIdentity.getPrincipal().getName(), userRoles);
        List<Person> personsFromDb = this.personRepository.findByRolesOrderedByName(userRoles);
        log.info("Caricate {} persone database", personsFromDb.size());
        return personsFromDb;
    }

}
