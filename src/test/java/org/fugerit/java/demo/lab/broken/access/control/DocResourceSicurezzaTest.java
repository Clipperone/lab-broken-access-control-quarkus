package org.fugerit.java.demo.lab.broken.access.control;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import static io.restassured.RestAssured.given;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

@QuarkusTest
/*
 * questa test suite contiene due tipi di test :
 *
 * 1. usando @TestSecurity, annotation di quarkus con cui vengono iniettati utente e ruolo
 * 2. iniettando un JWT direttamente con RestAssured .header("Authorization", "Bearer " + "${JWT}")
 */
@Slf4j
class DocResourceSicurezzaTest {

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

}
