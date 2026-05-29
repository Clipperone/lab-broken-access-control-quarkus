package org.fugerit.java.demo.lab.broken.access.control;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static io.restassured.RestAssured.given;

/**
 * ESEMPIO DI RIFERIMENTO — Function Level Access Control & HTTP Verb Tampering (OWASP A01).
 *
 * <p>
 * Questa classe mostra come scrivere unit test di AUTORIZZAZIONE per la classe "Missing Function
 * Level Access Control": verificare che un'azione (metodo+path) sia consentita SOLO ai ruoli previsti
 * e che metodi HTTP non dichiarati non siano invocabili.
 * </p>
 *
 * <p>
 * Distinzione chiave (authz, non authn):
 * </p>
 * <ul>
 * <li><b>401 Unauthorized</b> = autenticazione mancante/non valida ("chi sei?") — NON è il focus qui.</li>
 * <li><b>403 Forbidden</b> = autenticato ma privo del permesso per l'azione ("cosa puoi fare?") — il
 * cuore del controllo function-level.</li>
 * <li><b>405 Method Not Allowed</b> = il verbo HTTP non è dichiarato per quella risorsa: difesa
 * "gratuita" offerta da JAX-RS contro il verb tampering.</li>
 * </ul>
 */
@QuarkusTest
@Slf4j
class DocResourceFunctionLevelTest {

    private static final String VALID_PERSON_JSON = "{\"firstName\": \"ADA\",\"lastName\": \"LOVELACE\",\"title\": \"Matematica\",\"minRole\": \"guest\"}";

    // --- Escalation verticale sulla CREAZIONE: solo 'admin' può creare persone ---

    /**
     * Un'unica prova parametrica: QUALSIASI ruolo non-admin riceve 403 sulla creazione.
     * Usa JWT reali (non `@TestSecurity`) perché il ruolo varia per ogni invocazione; ogni token porta
     * un solo ruolo non privilegiato, così il 403 dipende solo dall'assenza di 'admin'.
     */
    @ParameterizedTest(name = "(403) function-level: il ruolo ''{0}'' (non-admin) NON può creare una persona")
    @ValueSource(strings = { "user", "guest" })
    @Tag("security")
    @Tag("forbidden")
    @Tag("function-level")
    void testAddPersonNonAdminKo(String nonAdminRole) {
        String token = DemoJwtGeneratorRest.generateToken("NONADMIN", nonAdminRole);
        given()
                .header("Authorization", "Bearer " + token)
                .body(VALID_PERSON_JSON).contentType(ContentType.JSON).accept(ContentType.JSON)
                .when().post("/doc/person/add")
                .then().statusCode(Response.Status.FORBIDDEN.getStatusCode());
    }

    // --- Verb tampering: un metodo HTTP non dichiarato non deve essere invocabile (chiude la classe della vuln BONUS X) ---

    @Test
    @DisplayName("(405) verb tampering: PUT su /doc/person/add (dichiara solo POST) → Method Not Allowed")
    @Tag("security")
    @Tag("function-level")
    @TestSecurity(user = "USER2", roles = { "admin", "user", "guest" })
    void testVerbTamperingPutOnAddNotAllowed() {
        // autenticati come admin: l'UNICA ragione di rifiuto deve essere il verbo non dichiarato, non l'autorizzazione
        given()
                .body(VALID_PERSON_JSON).contentType(ContentType.JSON)
                .when().put("/doc/person/add")
                .then().statusCode(Response.Status.METHOD_NOT_ALLOWED.getStatusCode());
    }

    @Test
    @DisplayName("(405) verb tampering: DELETE su /doc/person/list (dichiara solo GET) → Method Not Allowed")
    @Tag("security")
    @Tag("function-level")
    @TestSecurity(user = "USER2", roles = { "admin", "user", "guest" })
    void testVerbTamperingDeleteOnListNotAllowed() {
        given()
                .when().delete("/doc/person/list")
                .then().statusCode(Response.Status.METHOD_NOT_ALLOWED.getStatusCode());
    }

}
