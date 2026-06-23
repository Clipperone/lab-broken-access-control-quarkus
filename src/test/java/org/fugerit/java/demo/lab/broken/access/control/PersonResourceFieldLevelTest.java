package org.fugerit.java.demo.lab.broken.access.control;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import static io.restassured.RestAssured.given;
import io.restassured.http.ContentType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

/**
 * ESEMPIO DI RIFERIMENTO — Mass Assignment & Field-Level Authorization (OWASP A01).
 *
 * <p>
 * Lezione: l'autorizzazione non si ferma all'endpoint. Anche quando un'azione è consentita, il
 * server NON deve fidarsi del client per i CAMPI privilegiati o gestiti dal server.
 * </p>
 * <ul>
 * <li><b>Mass assignment</b>: il client non deve poter impostare campi server-controlled (qui
 * {@code uuid}, {@code id}, {@code creationDate}) tramite over-posting nel JSON.</li>
 * <li><b>Field-level authorization</b>: il campo privilegiato {@code minRole} è modificabile solo
 * dal ruolo 'admin'; un 'user' che lo manomette ottiene 403 (no privilege escalation di campo).</li>
 * </ul>
 *
 * <p>
 * Pattern di test utile: per scenari multi-identità (admin crea, user modifica) si usano JWT reali
 * via {@link DemoJwtGeneratorRest}, perché {@code @TestSecurity} fissa una sola identità per test.
 * </p>
 */
@QuarkusTest
@Slf4j
class PersonResourceFieldLevelTest {

    /** Crea una persona come admin e ne restituisce l'uuid generato dal server. */
    private String createPersonAsAdmin(String json) {
        return given()
                .header("Authorization", "Bearer " + DemoJwtGeneratorRest.generateAdminToken())
                .body(json).contentType(ContentType.JSON).accept(ContentType.JSON)
                .when().post("/person/add")
                .then().statusCode(Response.Status.CREATED.getStatusCode())
                .extract().path("uuid");
    }

    // --- Mass assignment: i campi server-controlled non sono sovrascrivibili dal client ---

    @Test
    @DisplayName("(201) mass assignment: uuid/id/creationDate inviati dal client vengono IGNORATI (li genera il server)")
    @Tag("security")
    @Tag("field-level")
    void testAddPersonIgnoresServerControlledFields() {
        String attackerUuid = "00000000-dead-beef-0000-000000000000";
        // includo campi che il DTO di richiesta NON espone
        String maliciousBody = "{\"firstName\": \"LISE\",\"lastName\": \"MEITNER\",\"title\": \"Fisica\",\"minRole\": \"guest\","
                + "\"uuid\": \"" + attackerUuid + "\",\"id\": 999999,\"creationDate\": \"2000-01-01T00:00:00\"}";
        String returnedUuid = 
                given()
                        .header("Authorization", "Bearer " + DemoJwtGeneratorRest.generateAdminToken())
                        .body(maliciousBody).contentType(ContentType.JSON).accept(ContentType.JSON)
                .when().post("/person/add")
                .then().statusCode(Response.Status.CREATED.getStatusCode())
                        .extract().path("uuid");
        log.info("testAddPersonIgnoresServerControlledFields returnedUuid : {}", returnedUuid);
        Assertions.assertNotEquals(attackerUuid, returnedUuid,
                "L'uuid deve essere generato dal server, non accettato dal body (mass assignment)");
        Assertions.assertNotNull(returnedUuid, "Il server deve generare un uuid");
    }

    // --- Field-level authorization sul campo privilegiato 'minRole' ---

    @Test
    @DisplayName("(403) field-level: un 'user' NON può modificare il campo privilegiato minRole")
    @Tag("security")
    @Tag("forbidden")
    @Tag("field-level")
    void testEditPersonUserCannotChangeMinRole() {
        String uuid = createPersonAsAdmin(
                "{\"firstName\": \"ROSALIND\",\"lastName\": \"FRANKLIN\",\"title\": \"Chimica\",\"minRole\": \"guest\"}");
        // lo 'user' tenta di alzare la visibilità della persona impostando minRole=admin -> 403
        given()
                .header("Authorization", "Bearer " + DemoJwtGeneratorRest.generateUserToken())
                .body("{\"firstName\": \"ROSALIND\",\"lastName\": \"FRANKLIN\",\"title\": \"Chimica\",\"minRole\": \"admin\"}")
                .contentType(ContentType.JSON).accept(ContentType.JSON)
        .when().put("/person/edit/%s".formatted(uuid))
        .then().statusCode(Response.Status.FORBIDDEN.getStatusCode());
    }

    @Test
    @DisplayName("(200) field-level: un 'user' può modificare i campi anagrafici; minRole resta invariato")
    @Tag("security")
    @Tag("authorized")
    @Tag("field-level")
    void testEditPersonUserCanEditAnagraphicFields() {
        String uuid = createPersonAsAdmin(
                "{\"firstName\": \"ROSALIND\",\"lastName\": \"FRANKLIN\",\"title\": \"Chimica\",\"minRole\": \"guest\"}");
        // request SENZA minRole: modifica consentita, il campo privilegiato non viene toccato
        String minRoleDopo = 
                given()
                        .header("Authorization", "Bearer " + DemoJwtGeneratorRest.generateUserToken())
                        .body("{\"firstName\": \"ROSALIND ELSIE\",\"lastName\": \"FRANKLIN\",\"title\": \"Biofisica\"}")
                        .contentType(ContentType.JSON).accept(ContentType.JSON)
                .when().put("/person/edit/%s".formatted(uuid))
                .then().statusCode(Response.Status.OK.getStatusCode())
                        .body("title", org.hamcrest.Matchers.equalTo("Biofisica"))
                        .extract().path("minRole");
        Assertions.assertEquals("guest", minRoleDopo,
                "minRole non deve cambiare per effetto di una modifica fatta da un 'user'");
    }

    @Test
    @DisplayName("(200) field-level: un 'admin' PUÒ modificare il campo privilegiato minRole")
    @Tag("security")
    @Tag("authorized")
    @Tag("field-level")
    void testEditPersonAdminCanChangeMinRole() {
        String uuid = createPersonAsAdmin(
                "{\"firstName\": \"ROSALIND\",\"lastName\": \"FRANKLIN\",\"title\": \"Chimica\",\"minRole\": \"guest\"}");
        given()
                .header("Authorization", "Bearer " + DemoJwtGeneratorRest.generateAdminToken())
                .body("{\"firstName\": \"ROSALIND\",\"lastName\": \"FRANKLIN\",\"title\": \"Chimica\",\"minRole\": \"admin\"}")
                .contentType(ContentType.JSON).accept(ContentType.JSON)
                .when().put("/person/edit/%s".formatted(uuid))
                .then().statusCode(Response.Status.OK.getStatusCode())
                .body("minRole", org.hamcrest.Matchers.equalTo("admin"));
    }

}
