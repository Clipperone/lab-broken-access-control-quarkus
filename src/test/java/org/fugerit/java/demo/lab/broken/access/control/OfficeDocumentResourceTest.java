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
 * Identità (upn / ufficio / ruoli), via JWT reali con claim {@code office}. Nomi coerenti con gli
 * scienziati del progetto; gli "uffici" sono reparti (FISICA, CHIMICA):
 * </p>
 * <ul>
 * <li>EINSTEIN / FISICA / user — owner tipico</li>
 * <li>BOHR / FISICA / admin — admin stesso ufficio</li>
 * <li>PLANCK / FISICA / guest — stesso ufficio, ruolo inferiore</li>
 * <li>FERMI / FISICA / user — stesso ufficio, ruolo ≥ ma non admin (può leggere, non modificare)</li>
 * <li>MENDELEEV / CHIMICA / admin — admin di ufficio diverso (NON deve poter accedere)</li>
 * <li>LAVOISIER / CHIMICA / user — ufficio diverso, non condiviso</li>
 * </ul>
 */
@QuarkusTest
@Slf4j
class OfficeDocumentResourceTest {

    private static final String EINSTEIN = bearer(
            DemoJwtGeneratorRest.generateOfficeToken("EINSTEIN", "FISICA", "user", "guest"));
    private static final String BOHR = bearer(
            DemoJwtGeneratorRest.generateOfficeToken("BOHR", "FISICA", "admin", "user", "guest"));
    private static final String PLANCK = bearer(DemoJwtGeneratorRest.generateOfficeToken("PLANCK", "FISICA", "guest"));
    private static final String FERMI = bearer(DemoJwtGeneratorRest.generateOfficeToken("FERMI", "FISICA", "user", "guest"));
    private static final String MENDELEEV = bearer(
            DemoJwtGeneratorRest.generateOfficeToken("MENDELEEV", "CHIMICA", "admin", "user", "guest"));
    private static final String LAVOISIER = bearer(
            DemoJwtGeneratorRest.generateOfficeToken("LAVOISIER", "CHIMICA", "user", "guest"));

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
        String uuid = createDocAs(EINSTEIN);
        given().header("Authorization", EINSTEIN)
                .when().get("/doc/officedoc/%s".formatted(uuid))
                .then().statusCode(Response.Status.OK.getStatusCode())
                .body("status", Matchers.equalTo("DRAFT"))
                .body("ownerOffice", Matchers.equalTo("FISICA"));
    }

    @Test
    @DisplayName("(403) draft: una bozza NON è visibile all'admin di ufficio finché non è pubblicata")
    @Tag("security")
    @Tag("forbidden")
    @Tag("tenant")
    void testDraftNotVisibleToOfficeAdmin() {
        String uuid = createDocAs(EINSTEIN);
        given().header("Authorization", BOHR)
                .when().get("/doc/officedoc/%s".formatted(uuid))
                .then().statusCode(Response.Status.FORBIDDEN.getStatusCode());
    }

    @Test
    @DisplayName("(200) tenant: dopo la pubblicazione, l'admin dello stesso ufficio (ruolo ≥) legge")
    @Tag("security")
    @Tag("authorized")
    @Tag("tenant")
    void testPublishedVisibleToSameOfficeHigherRole() {
        String uuid = createDocAs(EINSTEIN);
        publishAs(EINSTEIN, uuid);
        given().header("Authorization", BOHR)
                .when().get("/doc/officedoc/%s".formatted(uuid))
                .then().statusCode(Response.Status.OK.getStatusCode());
    }

    @Test
    @DisplayName("(403) gerarchia: stesso ufficio ma ruolo inferiore all'owner → non legge")
    @Tag("security")
    @Tag("forbidden")
    @Tag("tenant")
    void testSameOfficeLowerRoleForbidden() {
        String uuid = createDocAs(EINSTEIN);
        publishAs(EINSTEIN, uuid);
        given().header("Authorization", PLANCK)
                .when().get("/doc/officedoc/%s".formatted(uuid))
                .then().statusCode(Response.Status.FORBIDDEN.getStatusCode());
    }

    @Test
    @DisplayName("(403) tenant isolation: un admin di ufficio DIVERSO non può leggere (nemmeno se admin)")
    @Tag("security")
    @Tag("forbidden")
    @Tag("tenant")
    void testCrossOfficeAdminForbidden() {
        String uuid = createDocAs(EINSTEIN);
        publishAs(EINSTEIN, uuid);
        given().header("Authorization", MENDELEEV)
                .when().get("/doc/officedoc/%s".formatted(uuid))
                .then().statusCode(Response.Status.FORBIDDEN.getStatusCode());
    }

    @Test
    @DisplayName("(403) anti-enumeration: uuid inesistente → 403 identico al 'non autorizzato'")
    @Tag("security")
    @Tag("forbidden")
    @Tag("object-level")
    void testAntiEnumerationNonExistent() {
        given().header("Authorization", MENDELEEV)
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
        String uuid = createDocAs(EINSTEIN);
        given().header("Authorization", EINSTEIN)
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
        String uuid = createDocAs(EINSTEIN);
        publishAs(EINSTEIN, uuid);
        given().header("Authorization", BOHR)
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
        String uuid = createDocAs(EINSTEIN);
        publishAs(EINSTEIN, uuid);
        given().header("Authorization", FERMI)
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
        String uuid = createDocAs(EINSTEIN);
        publishAs(EINSTEIN, uuid);
        given().header("Authorization", MENDELEEV)
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
                + "\"ownerUpn\": \"BOHR\",\"ownerOffice\": \"CHIMICA\",\"ownerRole\": \"admin\",\"status\": \"PUBLISHED\"}";
        given().header("Authorization", EINSTEIN)
                .body(malicious).contentType(ContentType.JSON).accept(ContentType.JSON)
                .when().post("/doc/officedoc")
                .then().statusCode(Response.Status.CREATED.getStatusCode())
                .body("ownerUpn", Matchers.equalTo("EINSTEIN"))
                .body("ownerOffice", Matchers.equalTo("FISICA"))
                .body("status", Matchers.equalTo("DRAFT"));
    }

    // --- Sharing esplicito: supera ufficio/ruolo (solo lettura) ---

    @Test
    @DisplayName("(200) sharing: un utente di ufficio diverso, se condiviso, può leggere")
    @Tag("security")
    @Tag("authorized")
    @Tag("tenant")
    void testSharingGrantsCrossOfficeRead() {
        String uuid = createDocAs(EINSTEIN);
        shareAs(EINSTEIN, uuid, "MENDELEEV");
        given().header("Authorization", MENDELEEV)
                .when().get("/doc/officedoc/%s".formatted(uuid))
                .then().statusCode(Response.Status.OK.getStatusCode());
    }

    @Test
    @DisplayName("(403) sharing: un utente di ufficio diverso NON condiviso non può leggere")
    @Tag("security")
    @Tag("forbidden")
    @Tag("tenant")
    void testNotSharedCrossOfficeForbidden() {
        String uuid = createDocAs(EINSTEIN);
        shareAs(EINSTEIN, uuid, "MENDELEEV");
        given().header("Authorization", LAVOISIER)
                .when().get("/doc/officedoc/%s".formatted(uuid))
                .then().statusCode(Response.Status.FORBIDDEN.getStatusCode());
    }

}
