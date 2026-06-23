package org.fugerit.java.demo.lab.broken.access.control;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import static io.restassured.RestAssured.given;
import io.restassured.http.ContentType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

@QuarkusTest
/*
 * Test suite per gli endpoint /person/* (CRUD persone estratte da DocResource).
 *
 * Identità iniettate via @TestSecurity (singola identità) o JWT via RestAssured
 * (per scenari multi-identità, ad es. admin crea + user modifica).
 */
@Slf4j
class PersonResourceSicurezzaTest {

    private static final String ID_NON_ESISTE = "955b6a27-3da5-421f-a380-a86944e0c769"; // gli id partano da 10000

    private static final String ID_MARGHERITA_HACK = "46005e2d-4faa-4c5a-8ed2-6876d63622a7";

    private static final String ID_ALAN_TURING = "62472b90-14a5-45b5-891e-14f9e5659680";

    private static final String ID_RICHARD_FEYMAN = "3ad86124-765a-4104-a2dd-e99335ff1260";

    // test sul path /person/* (interroga / inserisci / cancella persone)

    @Test
    @DisplayName("(200) trova persona con ruolo autorizzato, ruolo utente 'admin', 'user' e 'guest'.")
    @Tag("security")
    @Tag("authorized")
    @Tag("object-level")
    @TestSecurity(user = "USER2", roles = { "guest", "user", "admin" })
    void testFindPersonOkAdmin() {
        String responseBody = given()
                .when().get("/person/find/%s".formatted(ID_RICHARD_FEYMAN)).then()
                .statusCode(Response.Status.OK.getStatusCode()).extract().body().asString();
        Assertions.assertTrue(responseBody.contains("Feynman"));
        log.info("testFindPersonOkAdmin : {}", responseBody);
    }

    @Test
    @DisplayName("(200) trova persona con ruolo autorizzato, ruolo utente 'user' e 'guest'.")
    @Tag("security")
    @Tag("authorized")
    @Tag("object-level")
    @TestSecurity(user = "USER1", roles = { "guest", "user" })
    void testFindPersonOkUser() {
        String responseBody = given()
                .when().get("/person/find/%s".formatted(ID_MARGHERITA_HACK)).then()
                .statusCode(Response.Status.OK.getStatusCode()).extract().body().asString();
        Assertions.assertTrue(responseBody.contains("Hack"));
        log.info("responseBody : {}", responseBody);
    }

    @Test
    @DisplayName("(403) trova persona con ruolo NON autorizzato, ruolo utente 'user' e 'guest'.")
    @Tag("security")
    @Tag("forbidden")
    @Tag("object-level")
    @TestSecurity(user = "USER1", roles = { "guest", "user" })
    void testFindPersonKoForbidden() {
        given()
                .when().get("/person/find/10002").then().statusCode(Response.Status.FORBIDDEN.getStatusCode());
    }

    // VULNERABILITY: (1) risolvi questa vulnerabilità in modo che il caso di test funzioni.
    @Test
    @DisplayName("VULNERABILITY: (1) - (403) Un utente che non esiste, restituisce un forbidden per evitare object enumeration.")
    @Tag("security")
    @Tag("forbidden")
    @Tag("object-level")
    @TestSecurity(user = "USER1", roles = { "guest", "user" })
    void testFindPersonKoNotFound() {
        given()
                .when().get("/person/find/%s".formatted(ID_NON_ESISTE)).then()
                .statusCode(Response.Status.FORBIDDEN.getStatusCode());
    }

    @Test
    @DisplayName("(200) Lista persona con ruolo 'user', non trova utenti per cui serve 'admin'.")
    @Tag("security")
    @Tag("authorized")
    @TestSecurity(user = "USER1", roles = { "guest", "user" })
    void testListPersonsResultKo() {
        String responseBody = given()
                .when().get("/person/list").then().statusCode(Response.Status.OK.getStatusCode()).extract().asString();
        log.info("responseBody testListPersonsResultKo : {}", responseBody);
        Assertions.assertFalse(responseBody.contains("Feynman"));
    }

    @Test
    @DisplayName("(200) Lista persona con ruolo 'admin', trova utenti per cui serve 'admin'.")
    @Tag("security")
    @Tag("authorized")
    @TestSecurity(user = "USER2", roles = { "admin", "user" })
    void testListPersonsResultOk() {
        String responseBody = given()
                .when().get("/person/list").then().statusCode(Response.Status.OK.getStatusCode()).extract().asString();
        log.info("responseBody testListPersonsResultOk : {}", responseBody);
        Assertions.assertTrue(responseBody.contains("Feynman"));
    }

    @Test
    @DisplayName("(201) Utente 'admin' inserisce una nuova persona.")
    @Tag("security")
    @Tag("authorized")
    @TestSecurity(user = "USER2", roles = { "admin", "user", "guest" })
    void testAddPersonAdminOk() {
        String addMarieCurie = "{\"firstName\": \"MARIE\",\"lastName\": \"CURIE\",\"title\": \"Fisica\",\"minRole\": \"guest\"}";
        given()
                .when()
                .body(addMarieCurie).contentType(ContentType.JSON).accept(ContentType.JSON)
                .post("/person/add")
                .then()
                .statusCode(Response.Status.CREATED.getStatusCode());
    }

    @Test
    @Tag("security")
    @Tag("authorized")
    @TestSecurity(user = "USER2", roles = { "admin", "user" })
    @DisplayName("(200) Utente 'admin' cancella una persona dopo averla inserita.")
    void testAddDeletePersonAdminOk() {
        // per assicurare l'integrità del test, prima inserisco un utente e poi lo cancello
        String addPierreCurie = "{\"firstName\": \"PIERRE\",\"lastName\": \"CURIE\",\"title\": \"Fisico\",\"minRole\": \"guest\"}";
        String uuid = given()
                .when()
                .body(addPierreCurie).contentType(ContentType.JSON).accept(ContentType.JSON)
                .post("/person/add")
                .then()
                .statusCode(Response.Status.CREATED.getStatusCode())
                .extract()
                .path("uuid");
        log.info("testAddDeletePersonAdminOk added pierre curie uuid : {}", uuid);
        given()
                .when()
                .delete("/person/delete/%s".formatted(uuid))
                .then()
                .statusCode(Response.Status.OK.getStatusCode());
    }

    @Test
    @DisplayName("(403) Utente 'admin' impedita cancellazione di utente che non esiste con un forbidden.")
    @Tag("security")
    @Tag("forbidden")
    @TestSecurity(user = "USER2", roles = { "admin", "user" })
    void testDeletePersonAdminKoNonEsiste() {
        given()
                .when()
                .delete("/person/delete/%s".formatted(ID_NON_ESISTE))
                .then()
                .statusCode(Response.Status.FORBIDDEN.getStatusCode());
    }

    @Test
    @DisplayName("(403) Utente 'user' impedita cancellazione di un utente.")
    @Tag("security")
    @Tag("forbidden")
    @TestSecurity(user = "USER1", roles = { "user" })
    void testDeletePersonUserKo() {
        given()
                .when()
                .delete("/person/delete/%s".formatted(ID_ALAN_TURING))
                .then()
                .statusCode(Response.Status.FORBIDDEN.getStatusCode());
    }

}
