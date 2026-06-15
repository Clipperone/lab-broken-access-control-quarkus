package org.fugerit.java.demo.lab.broken.access.control;

import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;
import org.fugerit.java.demo.lab.broken.access.control.dto.AddPersonRequestDTO;
import org.fugerit.java.demo.lab.broken.access.control.dto.AddPersonResponseDTO;
import org.fugerit.java.demo.lab.broken.access.control.dto.EditPersonRequestDTO;
import org.fugerit.java.demo.lab.broken.access.control.dto.PersonResponseDTO;
import org.fugerit.java.demo.lab.broken.access.control.persistence.Person;
import org.fugerit.java.demo.lab.broken.access.control.persistence.PersonRepository;
import org.fugerit.java.doc.base.config.DocConfig;
import org.fugerit.java.doc.base.process.DocProcessContext;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Slf4j
@ApplicationScoped
@Path("/doc")
@Schema(description = "Servizio rest per la generazione di documento che contiene una lista di persone in vari formati, in aggiunta permette di manipolare l'elenco delle persone.")
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

    @APIResponse(responseCode = "201", description = "La persona è stata creata", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AddPersonResponseDTO.class)))
    @APIResponse(responseCode = "401", description = "Se l'autenticazione non è presente")
    @APIResponse(responseCode = "403", description = "Se l'utente non è autorizzato per la risorsa")
    @APIResponse(responseCode = "500", description = "In caso di errori non gestiti")
    @Tag(name = "person")
    @Operation(operationId = "addPerson", summary = "Aggiunge una persona al database (ruoli: admin)", description = "Vanno forniti i parametri, nome, cognome, titolo e ruolo minimo.")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/person/add")
    @RolesAllowed("admin")
    @Transactional
    public Response addPerson(@Valid AddPersonRequestDTO request) {
        Person person = new Person();
        person.setUuid(UUID.randomUUID().toString());
        person.setFirstName(request.getFirstName());
        person.setLastName(request.getLastName());
        person.setTitle(request.getTitle());
        person.setMinRole(request.getMinRole());
        person.persistAndFlush();
        AddPersonResponseDTO response = new AddPersonResponseDTO();
        response.setUuid(person.getUuid());
        response.setCreationDate(person.getCreationDate());
        return Response.status(Response.Status.CREATED).entity(response).build();
    }

    // VULNERABILITY: (X) una PUT senza controllo di autorizzazione è rimasta abilitata per errore (è
    // utilizzabile senza il ruolo 'admin'): la soluzione è rimuovere totalmente il metodo addPersonPut()
    @APIResponse(responseCode = "201", description = "La persona è stata creata", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AddPersonResponseDTO.class)))
    @APIResponse(responseCode = "401", description = "Se l'autenticazione non è presente")
    @APIResponse(responseCode = "403", description = "Se l'utente non è autorizzato per la risorsa")
    @APIResponse(responseCode = "500", description = "In caso di errori non gestiti")
    @Tag(name = "person")
    @Operation(operationId = "addPersonPut", summary = "Aggiunge una persona al database (ruoli: admin)", description = "Vanno forniti i parametri, nome, cognome, titolo e ruolo minimo.")
    @PUT
    @Path("/person/add")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @PermitAll
    @Transactional
    public Response addPersonPut(@Valid AddPersonRequestDTO request) {
        return this.addPerson(request);
    }

    @APIResponse(responseCode = "200", description = "La persona è stata creata", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AddPersonResponseDTO.class)))
    @APIResponse(responseCode = "401", description = "Se l'autenticazione non è presente")
    @APIResponse(responseCode = "403", description = "Se l'utente non è autorizzato per la risorsa")
    @APIResponse(responseCode = "500", description = "In caso di errori non gestiti")
    @Tag(name = "person")
    @Operation(operationId = "findPerson", summary = "Interroga i dati di una persona per ID (ruoli: admin, user)", description = "Sul risultato viene verificato che sia presente il ruolo minimo.")
    @GET
    @Path("/person/find/{uuid}")
    @RolesAllowed({ "admin", "user" })
    @Transactional
    public Response findPerson(@PathParam("uuid") String uuid) {
        Person person = this.personRepository.findByUuid(uuid);
        if (person == null) {
            // VULNERABILITY: (1) restituiamo NOT_FOUND: questo rende gli oggetti enumerabili (404 per uuid
            // inesistente vs 403 per non autorizzato). Va restituito FORBIDDEN, identico al caso non autorizzato.
            return Response.status(Response.Status.NOT_FOUND).build();
        } else {
            // VULNERABILITY: (4) manca la verifica del ruolo minimo (minRole) dell'oggetto: qualsiasi ruolo
            // legge qualsiasi persona. Va verificato che l'utente abbia il minRole richiesto, altrimenti FORBIDDEN.
            return Response.status(Response.Status.OK).entity(person.toDTO()).build();
        }
    }

    @APIResponse(responseCode = "200", description = "La persona è stata modificata", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = PersonResponseDTO.class)))
    @APIResponse(responseCode = "401", description = "Se l'autenticazione non è presente")
    @APIResponse(responseCode = "403", description = "Se l'utente non è autorizzato per la risorsa o per un campo privilegiato")
    @APIResponse(responseCode = "500", description = "In caso di errori non gestiti")
    @Tag(name = "person")
    @Operation(operationId = "editPerson", summary = "Modifica una persona per UUID (ruoli: admin, user)", description = "Il ruolo 'user' può modificare i dati anagrafici; solo 'admin' può modificare il campo privilegiato 'minRole'.")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/person/edit/{uuid}")
    @RolesAllowed({ "admin", "user" })
    @Transactional
    public Response editPerson(@PathParam("uuid") String uuid, @Valid EditPersonRequestDTO request) {
        Person person = this.personRepository.findByUuid(uuid);
        // anti-enumeration + object-level: persona inesistente o non accessibile per il ruolo minimo -> 403 uniforme
        if (person == null
                || (person.getMinRole() != null && !this.securityIdentity.getRoles().contains(person.getMinRole()))) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        // VULNERABILITY: (6) FIELD-LEVEL: 'minRole' è un campo PRIVILEGIATO ma qui viene applicato per
        // qualsiasi ruolo (anche 'user'): privilege escalation tramite manomissione del campo. La soluzione
        // è verificare lato server che SOLO un 'admin' possa valorizzare 'minRole' (altrimenti 403).
        // campi anagrafici (non privilegiati): modificabili dai ruoli autorizzati all'endpoint
        person.setFirstName(request.getFirstName());
        person.setLastName(request.getLastName());
        person.setTitle(request.getTitle());
        // campo privilegiato applicato SENZA controllo di ruolo (VULNERABILITY: (6))
        if (request.getMinRole() != null) {
            person.setMinRole(request.getMinRole());
        }
        person.persistAndFlush();
        return Response.status(Response.Status.OK).entity(person.toDTO()).build();
    }

    @APIResponse(responseCode = "200", description = "La persona è stata creata", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AddPersonResponseDTO.class)))
    @APIResponse(responseCode = "401", description = "Se l'autenticazione non è presente")
    @APIResponse(responseCode = "403", description = "Se l'utente non è autorizzato per la risorsa")
    @APIResponse(responseCode = "404", description = "Persona non trovata")
    @APIResponse(responseCode = "500", description = "In caso di errori non gestiti")
    @Tag(name = "person")
    @Operation(operationId = "deletePerson", summary = "Cancella una persona per ID (ruoli: admin)", description = "Cancella un utente")
    @DELETE
    @Path("/person/delete/{uuid}")
    // VULNERABILITY: (3) il ruolo 'user' è erroneamente tra quelli autorizzati alla cancellazione: secondo
    // le specifiche deve essere consentita solo ad 'admin'. Rimuovere 'user' dall'elenco @RolesAllowed.
    @RolesAllowed({ "admin", "user" })
    @Transactional
    public Response deletePerson(@PathParam("uuid") String uuid) {
        Person person = this.personRepository.findByUuid(uuid);
        if (person == null) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        person.delete();
        return Response.status(Response.Status.OK).build();
    }

    @APIResponse(responseCode = "200", description = "La persona è stata creata", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(type = SchemaType.ARRAY, implementation = AddPersonResponseDTO.class)))
    @APIResponse(responseCode = "401", description = "Se l'autenticazione non è presente")
    @APIResponse(responseCode = "403", description = "Se l'utente non è autorizzato per la risorsa")
    @APIResponse(responseCode = "500", description = "In caso di errori non gestiti")
    @Tag(name = "person")
    @Operation(operationId = "listPerson", summary = "Elenca le persone attualmente presenti (ruoli: admin, user)", description = "Il risultato viene filtrato in base al ruolo minimo")
    @GET
    @Path("/person/list")
    @RolesAllowed({ "admin", "user" })
    @Transactional
    public Response listPersons() {
        return Response.status(Response.Status.OK).entity(this.listAllPersons().stream().map(Person::toDTO).toList()).build();
    }

    /*
     * metodo che carica tutte le persone cui l'utente corrente ha accesso.
     */
    private List<Person> listAllPersons() {
        Set<String> userRoles = this.securityIdentity.getRoles();
        log.info("user : {}, roles : {}", this.securityIdentity.getPrincipal().getName(), userRoles);
        List<Person> personsFromDb = this.personRepository.findByRolesOrderedByName(userRoles);
        log.info("Caricate {} persone database", personsFromDb.size());
        return personsFromDb;
    }

}
