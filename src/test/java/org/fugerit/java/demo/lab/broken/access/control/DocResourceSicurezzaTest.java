package org.fugerit.java.demo.lab.broken.access.control;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

@QuarkusTest
/*
 * questa test suite contiene due tipi di test :
 *
 * 1. usando @TestSecurity, annotation di quarkus con cui vengono iniettati utente e ruolo
 * 2. iniettando un JWT direttamente con RestAssured .header("Authorization", "Bearer " + "${JWT}")
 */
@Slf4j
class DocResourceSicurezzaTest {

    private static final String ID_NON_ESISTE = "955b6a27-3da5-421f-a380-a86944e0c769"; // gli id partano da 10000

    private static final String ID_MARGHERITA_HACK = "46005e2d-4faa-4c5a-8ed2-6876d63622a7";

    private static final String ID_ALAN_TURING = "62472b90-14a5-45b5-891e-14f9e5659680";

    private static final String ID_RICHARD_FEYMAN = "3ad86124-765a-4104-a2dd-e99335ff1260";

    private static final String EXPIRED_JWT = "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJodHRwczovL3VuaXR0ZXN0ZGVtb2FwcC5mdWdlcml0Lm9yZyIsInVwbiI6IkRFTU9VU0VSIiwiZ3JvdXBzIjpbImFkbWluIiwiZ3Vlc3QiLCJ1c2VyIl0sInN1YiI6IkRFTU9VU0VSIiwiaWF0IjoxNzcxMjQ2NzE3LCJleHAiOjE3NzEyNTAzMTcsImp0aSI6Ijc1MDA3YjBlLTBmYzktNDdkMS05OTY2LTEyMmIxODNkMDZlMyJ9.FblIqZcvhCpgJzlgulOBH0nWXkYwLJv9IpCuTAArvwTTZN2sAsFiGV7bH9tnalbINmgrVfSMAWoSVG1o4WtMY5Tg_ZtIGr1JJQY5zpH584CBWZIqDo9NJkVmTB1H1aK-ZiENGjghbdpVyxdy-JwS6YRdqfRtNWAG4jlzzXuEtsWKqCTeUt9cp1PVVOFyKVqOwG0tbPcjuEimCP3Z47XmFhe2TVll78BDY7AuRN-sWLRXAoSmOuTUY5I59Zqu_5PzqA_l2xDc8NtOlQDJXhFX1L1_WNYQMbNes8P4oS8_KDs_r5A_yxpjA8wPunfCOkJIsQ6QcWuvO7TB6pYfs_PeoxpSm2wMvKW2sRsmNqSHQ2oVKbLXp1Z4r2Wny0-CqkG7dTtBBhX9GRY79x67V9aoX_yH_gu2J0ujN6uPsrESSLDuBlOPpWGSn_OTES8fGhkLqalWmLAMQfE-oCphzmJ-4ktYwmpvOz4zczDBsbFZdGf6ARH3ahrvCbeiTM2SG_b4WBZBiNJ7kOSBoScRhIXaZT0ElfI6YhjyPn85P1qlVyxgzbmSKQWvYCVmZehGXHIA1Up4R9O39o7nsMhQjku3PMlTwfyQJ_x5OxeRs0ktmfrfm8Pzn0fMW3SMLUcDJPrmMT52mr5mscDBJVv0VBH_51o_bXTOFSjeVIvFXIk44mQ";

    // test sul path /doc/example.* (generazioni documenti)

    @Test
    @DisplayName("(200) generazione documento, formato HTML con utente autorizzato, ruolo utente 'user'")
    @Tag("security")
    @Tag("authorized")
    @Tag("TestSecurity")
    @TestSecurity(user = "USER1", roles = { "user" })
    void testHtmlOkNoAdminRole() {
        given().when().get("/doc/example.html").then().statusCode(Response.Status.OK.getStatusCode());
    }

    @Test
    @DisplayName("(200) generazione documento, formato PDF con utente autorizzato, ruolo utente 'admin', 'user' e 'guest'")
    @Tag("security")
    @Tag("authorized")
    @Tag("TestSecurity")
    @TestSecurity(user = "USER2", roles = { "guest", "user", "admin" })
    void testPdfOkNoAdminRole() {
        given().when().get("/doc/example.pdf").then().statusCode(Response.Status.OK.getStatusCode());
    }

    // VULNERABILITY: (5) risolvi questa vulnerabilità in modo che il caso di test funzioni.
    @Test
    @DisplayName("VULNERABILITY: (5) - (401) generazione documento, formato MarkDown con utente non valido, nessun ruolo associato.")
    @Tag("security")
    @Tag("unauthorized")
    @Tag("TestSecurity")
    void testMarkdown401NoAuthorizationBearer() {
        given()
                .when().get("/doc/example.md").then().statusCode(Response.Status.UNAUTHORIZED.getStatusCode());
    }

    @Test
    @DisplayName("(403) generazione documento, formato PDF con utente senza ruolo necessario, ruolo utente 'user'.")
    @Tag("security")
    @Tag("forbidden")
    @Tag("TestSecurity")
    @TestSecurity(user = "USER1", roles = { "user" })
    void testMarkdown403NoAdminRole() {
        given()
                .when().get("/doc/example.pdf").then().statusCode(Response.Status.FORBIDDEN.getStatusCode());
    }

    @Test
    @DisplayName("(200) generazione documento, formato PDF con utente con i ruoli necessari (tramite bearer token), ruolo utente 'admin', 'user' e 'guest'.")
    @Tag("security")
    @Tag("success")
    @Tag("Bearer")
    void testOkWithJwt() {
        given()
                .header("Authorization", String.format("Bearer %s", DemoJwtGeneratorRest.generateAdminToken()))
                .when().get("/doc/example.pdf").then().statusCode(Response.Status.OK.getStatusCode());
    }

    @Test
    @DisplayName("(200) generazione documento, formato MarkDown con utente che ha i ruoli per vedere tutti i risultati, ruolo utente 'admin', 'user' e 'guest'.")
    @Tag("security")
    @Tag("success")
    @Tag("Bearer")
    void testOkMarkDownConVerificaContenutoAdmin() {
        String requestBody = given()
                .header("Authorization", String.format("Bearer %s", DemoJwtGeneratorRest.generateAdminToken()))
                .when().get("/doc/example.md").then().statusCode(Response.Status.OK.getStatusCode()).extract().body()
                .asString();
        log.info("testOkMarkDownConVerificaContenutoAdmin : {}", requestBody);
        // il ruolo 'admin' ha accesso alla persona 'Richard Feynman'
        Assertions.assertTrue(requestBody.contains("Feynman"));
    }

    // VULNERABILITY: (2) risolvi questa vulnerabilità in modo che il caso di test funzioni.
    @Test
    @DisplayName("VULNERABILITY: (2) A - (200) generazione documento, formato MarkDown con utente che NON ha i ruoli per vedere tutti i risultati, ruolo utente 'user' e 'guest'.")
    @Tag("security")
    @Tag("success")
    @Tag("Bearer")
    void testOkMarkDownConVerificaContenutoUser() {
        String requestBody = given()
                .header("Authorization", String.format("Bearer %s", DemoJwtGeneratorRest.generateUserToken()))
                .when().get("/doc/example.md").then().statusCode(Response.Status.OK.getStatusCode()).extract().body()
                .asString();
        log.info("testOkMarkDownConVerificaContenutoUser : {}", requestBody);
        // i ruoli 'user o guest' NON hanno accesso alla persona 'Richard Feynman'
        Assertions.assertFalse(requestBody.contains("Feynman"));
    }

    @Test
    @DisplayName("(403) generazione documento, formato PDF con utente senza ruolo necessario (JWT), ruolo utente 'guest'.")
    @Tag("security")
    @Tag("forbidden")
    @Tag("Bearer")
    void testForbiddenWithJwt() {
        given().header("Authorization", "Bearer %s".formatted(DemoJwtGeneratorRest.generateGuestToken()))
                .when().get("/doc/example.pdf").then().statusCode(Response.Status.FORBIDDEN.getStatusCode());
    }

    @Test
    @DisplayName("(401) generazione documento, formato PDF con utente senza autenticazione.")
    @Tag("security")
    @Tag("unauthorized")
    @Tag("Bearer")
    void testUnauthorizedWithoutJwt() {
        given().when().get("/doc/example.pdf").then().statusCode(Response.Status.UNAUTHORIZED.getStatusCode());
    }

    @Test
    @DisplayName("(401) generazione documento, formato PDF con utente con JWT non valido.")
    @Tag("security")
    @Tag("unauthorized")
    @Tag("Bearer")
    void testUnauthorizedWithWrongJwt() {
        given()
                .header("Authorization", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJVU0VSMSIsIm5hbWUi")
                .when().get("/doc/example.pdf").then().statusCode(Response.Status.UNAUTHORIZED.getStatusCode());
    }

    @Test
    @DisplayName("(401) generazione documento, formato PDF con utente con JWT scaduto.")
    @Tag("security")
    @Tag("unauthorized")
    @Tag("Bearer")
    void testExpiredJWT() {
        given()
                .header("Authorization", "Bearer %s".formatted(EXPIRED_JWT))
                .when().get("/doc/example.pdf").then().statusCode(Response.Status.UNAUTHORIZED.getStatusCode());
    }

    @Test
    @DisplayName("(200) generazione documento, formato MarkDown con utente con ruolo necessario (JWT), ruolo utente 'guest'.")
    @Tag("security")
    @Tag("authorized")
    @Tag("Bearer")
    void testOkJwtMarkDown() {
        given()
                .header("Authorization", "Bearer %s".formatted(DemoJwtGeneratorRest.generateGuestToken()))
                .when().get("/doc/example.md").then().statusCode(Response.Status.OK.getStatusCode());
    }

    @Test
    @DisplayName("(200) generazione documento, formato AsciiDoc con utente con ruolo necessario (JWT), ruolo utente 'admin', 'user' e 'guest'.")
    @Tag("security")
    @Tag("authorized")
    @Tag("Bearer")
    void testOkJwtAsciiDoc() {
        given()
                .header("Authorization", "Bearer %s".formatted(DemoJwtGeneratorRest.generateAdminToken()))
                .when().get("/doc/example.adoc").then().statusCode(Response.Status.OK.getStatusCode());
    }

    @Test
    @DisplayName("(403) generazione documento, formato AsciiDoc con utente senza ruolo necessario (JWT), ruolo utente 'guest'.")
    @Tag("security")
    @Tag("forbidden")
    @Tag("Bearer")
    void testForbiddenJwtAsciiDoc() {
        given()
                .header("Authorization", "Bearer %s".formatted(new DemoJwtGeneratorRest().newToken("guest")))
                .when().get("/doc/example.pdf").then().statusCode(Response.Status.FORBIDDEN.getStatusCode());
    }

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

    // VULNERABILITY: (4) risolvi questa vulnerabilità in modo che il caso di test funzioni.
    @Test
    @DisplayName("VULNERABILITY: (4) - (403) trova persona con ruolo NON autorizzato, ruolo utente 'user' e 'guest'.")
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

    // VULNERABILITY: (2) risolvi questa vulnerabilità in modo che il caso di test funzioni.
    @Test
    @DisplayName("VULNERABILITY: (2) B - (200) Lista persona con ruolo 'user', non trova utenti per cui serve 'admin'.")
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

    // VULNERABILITY: (3) risolvi questa vulnerabilità in modo che il caso di test funzioni.
    @Test
    @DisplayName("VULNERABILITY: (3) - (403) Utente 'user' impedita cancellazione di un utente.")
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