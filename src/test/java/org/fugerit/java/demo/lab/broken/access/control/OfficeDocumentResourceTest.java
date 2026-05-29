package org.fugerit.java.demo.lab.broken.access.control;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

/**
 * ESEMPIO DI RIFERIMENTO — Autorizzazione multi-tenant per ufficio + gerarchia ruoli (SCENARIO 3).
 *
 * <p>
 * Identità (upn / ufficio / ruoli), via JWT reali con claim {@code office}:
 * </p>
 * <ul>
 * <li>ALICE / A / user — owner tipico</li>
 * <li>BOB / A / admin — admin stesso ufficio</li>
 * <li>CAROL / A / guest — stesso ufficio, ruolo inferiore</li>
 * <li>GINA / A / user — stesso ufficio, ruolo ≥ ma non admin (può leggere, non modificare)</li>
 * <li>DAVE / B / admin — admin di ufficio diverso (NON deve poter accedere)</li>
 * <li>ERIN / B / user — ufficio diverso, non condiviso</li>
 * </ul>
 */
@QuarkusTest
@Slf4j
class OfficeDocumentResourceTest {

    private static final String ALICE = bearer(DemoJwtGeneratorRest.generateOfficeToken("ALICE", "A", "user", "guest"));
    private static final String BOB = bearer(DemoJwtGeneratorRest.generateOfficeToken("BOB", "A", "admin", "user", "guest"));
    private static final String CAROL = bearer(DemoJwtGeneratorRest.generateOfficeToken("CAROL", "A", "guest"));
    private static final String GINA = bearer(DemoJwtGeneratorRest.generateOfficeToken("GINA", "A", "user", "guest"));
    private static final String DAVE = bearer(DemoJwtGeneratorRest.generateOfficeToken("DAVE", "B", "admin", "user", "guest"));
    private static final String ERIN = bearer(DemoJwtGeneratorRest.generateOfficeToken("ERIN", "B", "user", "guest"));

    private static final String DOC_JSON = "{\"fileName\": \"report.txt\",\"content\": \"dati di ufficio\"}";

    private static String bearer(String token) {
        return "Bearer %s".formatted(token);
    }

    private String createDocAs(String auth) {
        return given().header("Authorization", auth)
                .body(DOC_JSON).contentType(ContentType.JSON).accept(ContentType.JSON)
                .when().post("/doc/officedoc")
                .then().statusCode(Response.Status.CREATED.getStatusCode())
                .extract().path("uuid");
    }

    private void publishAs(String auth, String uuid) {
        given().header("Authorization", auth).accept(ContentType.JSON)
                .when().put("/doc/officedoc/%s/publish".formatted(uuid))
                .then().statusCode(Response.Status.OK.getStatusCode());
    }

    private void shareAs(String auth, String uuid, String targetUpn) {
        given().header("Authorization", auth)
                .body("{\"targetUpn\": \"%s\"}".formatted(targetUpn)).contentType(ContentType.JSON).accept(ContentType.JSON)
                .when().post("/doc/officedoc/%s/share".formatted(uuid))
                .then().statusCode(Response.Status.OK.getStatusCode());
    }

    // --- Lettura / tenant / gerarchia / draft ---

    @Test
    @DisplayName("(200) tenant: l'owner legge la propria bozza (DRAFT)")
    @Tag("security")
    @Tag("authorized")
    @Tag("tenant")
    void testOwnerReadsOwnDraft() {
        String uuid = createDocAs(ALICE);
        given().header("Authorization", ALICE)
                .when().get("/doc/officedoc/%s".formatted(uuid))
                .then().statusCode(Response.Status.OK.getStatusCode())
                .body("status", Matchers.equalTo("DRAFT"))
                .body("ownerOffice", Matchers.equalTo("A"));
    }

    @Test
    @DisplayName("(403) draft: una bozza NON è visibile all'admin di ufficio finché non è pubblicata")
    @Tag("security")
    @Tag("forbidden")
    @Tag("tenant")
    void testDraftNotVisibleToOfficeAdmin() {
        String uuid = createDocAs(ALICE);
        given().header("Authorization", BOB)
                .when().get("/doc/officedoc/%s".formatted(uuid))
                .then().statusCode(Response.Status.FORBIDDEN.getStatusCode());
    }

    @Test
    @DisplayName("(200) tenant: dopo la pubblicazione, l'admin dello stesso ufficio (ruolo ≥) legge")
    @Tag("security")
    @Tag("authorized")
    @Tag("tenant")
    void testPublishedVisibleToSameOfficeHigherRole() {
        String uuid = createDocAs(ALICE);
        publishAs(ALICE, uuid);
        given().header("Authorization", BOB)
                .when().get("/doc/officedoc/%s".formatted(uuid))
                .then().statusCode(Response.Status.OK.getStatusCode());
    }

    @Test
    @DisplayName("(403) gerarchia: stesso ufficio ma ruolo inferiore all'owner → non legge")
    @Tag("security")
    @Tag("forbidden")
    @Tag("tenant")
    void testSameOfficeLowerRoleForbidden() {
        String uuid = createDocAs(ALICE);
        publishAs(ALICE, uuid);
        given().header("Authorization", CAROL)
                .when().get("/doc/officedoc/%s".formatted(uuid))
                .then().statusCode(Response.Status.FORBIDDEN.getStatusCode());
    }

    @Test
    @DisplayName("(403) tenant isolation: un admin di ufficio DIVERSO non può leggere (nemmeno se admin)")
    @Tag("security")
    @Tag("forbidden")
    @Tag("tenant")
    void testCrossOfficeAdminForbidden() {
        String uuid = createDocAs(ALICE);
        publishAs(ALICE, uuid);
        given().header("Authorization", DAVE)
                .when().get("/doc/officedoc/%s".formatted(uuid))
                .then().statusCode(Response.Status.FORBIDDEN.getStatusCode());
    }

    @Test
    @DisplayName("(403) anti-enumeration: uuid inesistente → 403 identico al 'non autorizzato'")
    @Tag("security")
    @Tag("forbidden")
    @Tag("object-level")
    void testAntiEnumerationNonExistent() {
        given().header("Authorization", DAVE)
                .when().get("/doc/officedoc/%s".formatted("11111111-1111-1111-1111-111111111111"))
                .then().statusCode(Response.Status.FORBIDDEN.getStatusCode());
    }

    // --- Modifica: owner o admin stesso ufficio su PUBLISHED ---

    @Test
    @DisplayName("(200) modifica: l'owner modifica il proprio documento")
    @Tag("security")
    @Tag("authorized")
    @Tag("tenant")
    void testOwnerCanEdit() {
        String uuid = createDocAs(ALICE);
        given().header("Authorization", ALICE)
                .body("{\"fileName\": \"report-v2.txt\",\"content\": \"aggiornato\"}")
                .contentType(ContentType.JSON).accept(ContentType.JSON)
                .when().put("/doc/officedoc/%s".formatted(uuid))
                .then().statusCode(Response.Status.OK.getStatusCode())
                .body("fileName", Matchers.equalTo("report-v2.txt"));
    }

    @Test
    @DisplayName("(200) modifica: l'admin dello stesso ufficio modifica un documento PUBLISHED")
    @Tag("security")
    @Tag("authorized")
    @Tag("tenant")
    void testOfficeAdminCanEditPublished() {
        String uuid = createDocAs(ALICE);
        publishAs(ALICE, uuid);
        given().header("Authorization", BOB)
                .body(DOC_JSON).contentType(ContentType.JSON).accept(ContentType.JSON)
                .when().put("/doc/officedoc/%s".formatted(uuid))
                .then().statusCode(Response.Status.OK.getStatusCode());
    }

    @Test
    @DisplayName("(403) modifica: stesso ufficio, può leggere (ruolo ≥) ma NON è admin → non modifica")
    @Tag("security")
    @Tag("forbidden")
    @Tag("tenant")
    void testSameOfficeNonAdminCannotEdit() {
        String uuid = createDocAs(ALICE);
        publishAs(ALICE, uuid);
        given().header("Authorization", GINA)
                .body(DOC_JSON).contentType(ContentType.JSON).accept(ContentType.JSON)
                .when().put("/doc/officedoc/%s".formatted(uuid))
                .then().statusCode(Response.Status.FORBIDDEN.getStatusCode());
    }

    @Test
    @DisplayName("(403) modifica: un admin di ufficio diverso non può modificare")
    @Tag("security")
    @Tag("forbidden")
    @Tag("tenant")
    void testCrossOfficeAdminCannotEdit() {
        String uuid = createDocAs(ALICE);
        publishAs(ALICE, uuid);
        given().header("Authorization", DAVE)
                .body(DOC_JSON).contentType(ContentType.JSON).accept(ContentType.JSON)
                .when().put("/doc/officedoc/%s".formatted(uuid))
                .then().statusCode(Response.Status.FORBIDDEN.getStatusCode());
    }

    // --- Mass assignment: owner/ufficio/stato impostati dal server, non dal client ---

    @Test
    @DisplayName("(201) mass-assignment: owner/ufficio/stato inviati dal client vengono IGNORATI")
    @Tag("security")
    @Tag("field-level")
    void testMassAssignmentOwnerOfficeIgnored() {
        String malicious = "{\"fileName\": \"x.txt\",\"content\": \"c\","
                + "\"ownerUpn\": \"BOB\",\"ownerOffice\": \"B\",\"ownerRole\": \"admin\",\"status\": \"PUBLISHED\"}";
        given().header("Authorization", ALICE)
                .body(malicious).contentType(ContentType.JSON).accept(ContentType.JSON)
                .when().post("/doc/officedoc")
                .then().statusCode(Response.Status.CREATED.getStatusCode())
                .body("ownerUpn", Matchers.equalTo("ALICE"))
                .body("ownerOffice", Matchers.equalTo("A"))
                .body("status", Matchers.equalTo("DRAFT"));
    }

    // --- Sharing esplicito: supera ufficio/ruolo (solo lettura) ---

    @Test
    @DisplayName("(200) sharing: un utente di ufficio diverso, se condiviso, può leggere")
    @Tag("security")
    @Tag("authorized")
    @Tag("tenant")
    void testSharingGrantsCrossOfficeRead() {
        String uuid = createDocAs(ALICE);
        shareAs(ALICE, uuid, "DAVE");
        given().header("Authorization", DAVE)
                .when().get("/doc/officedoc/%s".formatted(uuid))
                .then().statusCode(Response.Status.OK.getStatusCode());
    }

    @Test
    @DisplayName("(403) sharing: un utente di ufficio diverso NON condiviso non può leggere")
    @Tag("security")
    @Tag("forbidden")
    @Tag("tenant")
    void testNotSharedCrossOfficeForbidden() {
        String uuid = createDocAs(ALICE);
        shareAs(ALICE, uuid, "DAVE");
        given().header("Authorization", ERIN)
                .when().get("/doc/officedoc/%s".formatted(uuid))
                .then().statusCode(Response.Status.FORBIDDEN.getStatusCode());
    }

}
