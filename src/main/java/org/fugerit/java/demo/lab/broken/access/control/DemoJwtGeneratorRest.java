package org.fugerit.java.demo.lab.broken.access.control;

import io.quarkus.arc.profile.UnlessBuildProfile;
import io.smallrye.jwt.build.Jwt;
import jakarta.annotation.security.PermitAll;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.jwt.Claims;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.fugerit.java.demo.lab.broken.access.control.security.EnumRoles;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;

/**
 * Endpoint dimostrativo per la generazione di JWT.
 *
 * <p>
 * LEZIONE DI SICUREZZA: questo endpoint conia token con ruoli arbitrari ed è un classico esempio di
 * "broken access control" se lasciato attivo in produzione. Per questo:
 * </p>
 * <ul>
 * <li>{@code @UnlessBuildProfile("prod")} → il bean (e quindi l'endpoint REST) NON viene registrato
 * nel profilo {@code prod}: in produzione l'autenticazione deve passare da un IDP esterno.</li>
 * <li>{@code @PermitAll} → è deliberatamente pubblico (in dev/test). Reso esplicito perché, con
 * {@code quarkus.security.jaxrs.deny-unannotated-endpoints=true} (deny-by-default), un endpoint
 * privo di annotazione di sicurezza verrebbe altrimenti negato.</li>
 * </ul>
 */
@Slf4j
@ApplicationScoped
@UnlessBuildProfile("prod")
@Path("/demo")
public class DemoJwtGeneratorRest {

    private static final String ISSUER = "https://unittestdemoapp.fugerit.org";

    private static final long JWT_DURATION_IN_MINUTES = 60;

    @APIResponse(responseCode = "201", description = "Generazione del JWT")
    @Tag(name = "jwt authorization demo")
    @Operation(operationId = "adminToken", summary = "Genera un nuovo JWT, i ruoli vanno passati come path param, separati da una virgola. (es. 'admin,user,guest')", description = "Attenzione : da utilizzare solo per motivi dimostrativi! (la durata del JWT sarà di un'ora)")
    @GET
    @Produces("text/plain")
    @Path("/{roles}.txt")
    @PermitAll
    public String newToken(@PathParam("roles") String roles) {
        return generateToken("DEMOUSER", roles.split(","));
    }

    /**
     * Genera un JWT per un utente con ruolo guest
     *
     * return il token JWT generato
     */
    public static String generateGuestToken() {
        String[] roles = { EnumRoles.GUEST.getCode() };
        return generateToken("USER3", roles);
    }

    /**
     * Genera un JWT per un utente con ruoli user e guest
     *
     * return il token JWT generato
     */
    public static String generateUserToken() {
        String[] roles = { EnumRoles.USER.getCode(), EnumRoles.GUEST.getCode() };
        return generateToken("USER1", roles);
    }

    /**
     * Genera un JWT per un utente con ruoli admin, user e guest
     *
     * return il token JWT generato
     */
    public static String generateAdminToken() {
        String[] roles = { EnumRoles.ADMIN.getCode(), EnumRoles.USER.getCode(), EnumRoles.GUEST.getCode() };
        return generateToken("USER2", roles);
    }

    /**
     * Genera un JWT personalizzato
     *
     * @param username lo username da usare per il JWT (verrà inserito come upn e claim sub)
     * @param roles l'elenco dei ruoli da associare all'utente
     *
     *        return il token JWT generato
     */
    public static String generateToken(String username, String... roles) {
        return Jwt.issuer(ISSUER)
                .upn(username)
                .groups(new HashSet<>(Arrays.asList(roles)))
                .claim(Claims.sub.name(), username)
                .expiresIn(Duration.ofMinutes(JWT_DURATION_IN_MINUTES))
                .sign();
    }

}