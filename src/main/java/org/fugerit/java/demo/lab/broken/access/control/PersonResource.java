package org.fugerit.java.demo.lab.broken.access.control;

import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.fugerit.java.demo.lab.broken.access.control.dto.AddPersonRequestDTO;
import org.fugerit.java.demo.lab.broken.access.control.dto.AddPersonResponseDTO;
import org.fugerit.java.demo.lab.broken.access.control.dto.EditPersonRequestDTO;
import org.fugerit.java.demo.lab.broken.access.control.dto.PersonResponseDTO;
import org.fugerit.java.demo.lab.broken.access.control.persistence.Person;
import org.fugerit.java.demo.lab.broken.access.control.persistence.PersonRepository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@ApplicationScoped
@Path("/person")
@Schema(description = "Servizio rest per la gestione (CRUD) delle persone.")
@SecurityScheme(securitySchemeName = "SecurityScheme", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT", description = "JWT Bearer Token Authentication")
public class PersonResource {

    PersonRepository personRepository;

    SecurityIdentity securityIdentity;

    public PersonResource(PersonRepository personRepository, SecurityIdentity securityIdentity) {
        this.personRepository = personRepository;
        this.securityIdentity = securityIdentity;
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
    @Path("/add")
    @RolesAllowed("admin")
    @Transactional
    @SecurityRequirement(name = "SecurityScheme")
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
    @Path("/add")
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
    @Path("/find/{uuid}")
    @RolesAllowed({ "admin", "user" })
    @Transactional
    @SecurityRequirement(name = "SecurityScheme")
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
    @Path("/edit/{uuid}")
    @RolesAllowed({ "admin", "user" })
    @Transactional
    @SecurityRequirement(name = "SecurityScheme")
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
    @Path("/delete/{uuid}")
    // VULNERABILITY: (3) il ruolo 'user' è erroneamente tra quelli autorizzati alla cancellazione: secondo
    // le specifiche deve essere consentita solo ad 'admin'. Rimuovere 'user' dall'elenco @RolesAllowed.
    @RolesAllowed({ "admin", "user" })
    @Transactional
    @SecurityRequirement(name = "SecurityScheme")
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
    @Path("/list")
    @RolesAllowed({ "admin", "user" })
    @Transactional
    @SecurityRequirement(name = "SecurityScheme")
    public Response listPersons() {
        return Response.status(Response.Status.OK).entity(this.listAllPersons().stream().map(Person::toDTO).toList()).build();
    }

    /*
     * metodo che carica tutte le persone cui l'utente corrente ha accesso.
     * NOTA: duplicato in DocResource.listAllPersons() — mantenere sincronizzati.
     */
    private List<Person> listAllPersons() {
        Set<String> userRoles = this.securityIdentity.getRoles();
        log.info("user : {}, roles : {}", this.securityIdentity.getPrincipal().getName(), userRoles);
        List<Person> personsFromDb = this.personRepository.findByRolesOrderedByName(userRoles);
        log.info("Caricate {} persone database", personsFromDb.size());
        return personsFromDb;
    }

}
