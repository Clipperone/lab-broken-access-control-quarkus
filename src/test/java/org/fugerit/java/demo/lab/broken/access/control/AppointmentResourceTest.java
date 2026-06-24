package org.fugerit.java.demo.lab.broken.access.control;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.fugerit.java.demo.lab.broken.access.control.persistence.Appointment;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static io.restassured.RestAssured.given;

/**
 * ESEMPIO DI RIFERIMENTO — Appuntamenti: visibilità multi-parte (relationship-based) + autorizzazione
 * temporale (delete solo > 24h prima) + ownership su delete/move.
 *
 * <p>
 * Identità (upn / ufficio / ruoli): EINSTEIN/FISICA/user (creatore), FERMI/FISICA/user (scienziato
 * destinatario), BOHR/FISICA/admin (admin ufficio), MENDELEEV/CHIMICA/admin (admin altro ufficio),
 * PLANCK/FISICA/guest (estraneo dello stesso ufficio).
 * </p>
 */
@QuarkusTest
@Slf4j
class AppointmentResourceTest {

    private static final String EINSTEIN = bearer(
            DemoJwtGeneratorRest.generateOfficeToken("EINSTEIN", "FISICA", "user", "guest"));
    private static final String FERMI = bearer(DemoJwtGeneratorRest.generateOfficeToken("FERMI", "FISICA", "user", "guest"));
    private static final String BOHR = bearer(
            DemoJwtGeneratorRest.generateOfficeToken("BOHR", "FISICA", "admin", "user", "guest"));
    private static final String MENDELEEV = bearer(
            DemoJwtGeneratorRest.generateOfficeToken("MENDELEEV", "CHIMICA", "admin", "user", "guest"));
    private static final String PLANCK = bearer(DemoJwtGeneratorRest.generateOfficeToken("PLANCK", "FISICA", "guest"));

    private static String bearer(String token) {
        return "Bearer %s".formatted(token);
    }

    private static String iso(long plusHours) {
        return LocalDateTime.now().plusHours(plusHours).withNano(0).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    private static String body(String scientist, String office, String atIso) {
        return "{\"scientistUpn\": \"%s\",\"office\": \"%s\",\"appointmentAt\": \"%s\",\"subject\": \"colloquio\"}"
                .formatted(scientist, office, atIso);
    }

    /** Crea un appuntamento (scienziato FERMI, ufficio FISICA) tra plusHours ore e ne restituisce l'uuid. */
    private String createApptAs(String auth, long plusHours) {
        return given().header("Authorization", auth)
                .body(body("FERMI", "FISICA", iso(plusHours))).contentType(ContentType.JSON).accept(ContentType.JSON)
                .when().post("/doc/appointment")
                .then().statusCode(Response.Status.CREATED.getStatusCode())
                .extract().path("uuid");
    }

    /**
     * Ogni test parte da uno stato pulito: necessario per la regola anti-doppia-prenotazione, altrimenti
     * gli appuntamenti accumulati su slot identici (es. now+48h) collidono tra test diversi.
     */
    @AfterEach
    void cleanup() {
        QuarkusTransaction.requiringNew().run(() -> Appointment.deleteAll());
    }

    // --- Visibilità multi-parte ---

    @Test
    @DisplayName("(200) appuntamento visibile al creatore")
    @Tag("security")
    @Tag("authorized")
    @Tag("tenant")
    void testCreatorCanView() {
        String uuid = createApptAs(EINSTEIN, 48);
        given().header("Authorization", EINSTEIN)
                .when().get("/doc/appointment/%s".formatted(uuid))
                .then().statusCode(Response.Status.OK.getStatusCode())
                .body("creatorUpn", Matchers.equalTo("EINSTEIN"));
    }

    @Test
    @DisplayName("(200) appuntamento visibile allo scienziato destinatario")
    @Tag("security")
    @Tag("authorized")
    @Tag("tenant")
    void testScientistCanView() {
        String uuid = createApptAs(EINSTEIN, 48);
        given().header("Authorization", FERMI)
                .when().get("/doc/appointment/%s".formatted(uuid))
                .then().statusCode(Response.Status.OK.getStatusCode());
    }

    @Test
    @DisplayName("(200) appuntamento visibile all'admin dello stesso ufficio")
    @Tag("security")
    @Tag("authorized")
    @Tag("tenant")
    void testOfficeAdminCanView() {
        String uuid = createApptAs(EINSTEIN, 48);
        given().header("Authorization", BOHR)
                .when().get("/doc/appointment/%s".formatted(uuid))
                .then().statusCode(Response.Status.OK.getStatusCode());
    }

    @Test
    @DisplayName("(403) appuntamento NON visibile all'admin di un altro ufficio (isolamento di tenant)")
    @Tag("security")
    @Tag("forbidden")
    @Tag("tenant")
    void testCrossOfficeAdminForbidden() {
        String uuid = createApptAs(EINSTEIN, 48);
        given().header("Authorization", MENDELEEV)
                .when().get("/doc/appointment/%s".formatted(uuid))
                .then().statusCode(Response.Status.FORBIDDEN.getStatusCode());
    }

    @Test
    @DisplayName("(403) appuntamento NON visibile a un estraneo dello stesso ufficio (non creatore/destinatario/admin)")
    @Tag("security")
    @Tag("forbidden")
    @Tag("tenant")
    void testUnrelatedSameOfficeForbidden() {
        String uuid = createApptAs(EINSTEIN, 48);
        given().header("Authorization", PLANCK)
                .when().get("/doc/appointment/%s".formatted(uuid))
                .then().statusCode(Response.Status.FORBIDDEN.getStatusCode());
    }

    @Test
    @DisplayName("(403) anti-enumeration: appuntamento inesistente → 403")
    @Tag("security")
    @Tag("forbidden")
    @Tag("object-level")
    void testAntiEnumerationNonExistent() {
        given().header("Authorization", EINSTEIN)
                .when().get("/doc/appointment/%s".formatted("99999999-9999-9999-9999-999999999999"))
                .then().statusCode(Response.Status.FORBIDDEN.getStatusCode());
    }

    // --- Autorizzazione temporale sull'eliminazione ---

    @Test
    @DisplayName("(200) il creatore elimina un appuntamento a più di 24h")
    @Tag("security")
    @Tag("authorized")
    @Tag("business-logic")
    void testCreatorDeleteMoreThan24hOk() {
        String uuid = createApptAs(EINSTEIN, 48);
        given().header("Authorization", EINSTEIN)
                .when().delete("/doc/appointment/%s".formatted(uuid))
                .then().statusCode(Response.Status.OK.getStatusCode());
    }

    @Test
    @DisplayName("(403) il creatore NON può eliminare a meno di 24h dall'appuntamento (regola temporale)")
    @Tag("security")
    @Tag("forbidden")
    @Tag("business-logic")
    void testCreatorDeleteWithin24hForbidden() {
        String uuid = createApptAs(EINSTEIN, 12);
        given().header("Authorization", EINSTEIN)
                .when().delete("/doc/appointment/%s".formatted(uuid))
                .then().statusCode(Response.Status.FORBIDDEN.getStatusCode());
    }

    @Test
    @DisplayName("(403) un non-creatore (anche destinatario) non può eliminare")
    @Tag("security")
    @Tag("forbidden")
    @Tag("ownership")
    void testNonCreatorCannotDelete() {
        String uuid = createApptAs(EINSTEIN, 48);
        given().header("Authorization", FERMI)
                .when().delete("/doc/appointment/%s".formatted(uuid))
                .then().statusCode(Response.Status.FORBIDDEN.getStatusCode());
    }

    // --- Spostamento (solo creatore) ---

    @Test
    @DisplayName("(200) il creatore può spostare l'appuntamento")
    @Tag("security")
    @Tag("authorized")
    @Tag("ownership")
    void testCreatorCanMove() {
        String uuid = createApptAs(EINSTEIN, 48);
        given().header("Authorization", EINSTEIN)
                .body("{\"newAppointmentAt\": \"%s\"}".formatted(iso(72))).contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .when().put("/doc/appointment/%s/move".formatted(uuid))
                .then().statusCode(Response.Status.OK.getStatusCode());
    }

    @Test
    @DisplayName("(403) un non-creatore non può spostare l'appuntamento")
    @Tag("security")
    @Tag("forbidden")
    @Tag("ownership")
    void testNonCreatorCannotMove() {
        String uuid = createApptAs(EINSTEIN, 48);
        given().header("Authorization", BOHR)
                .body("{\"newAppointmentAt\": \"%s\"}".formatted(iso(72))).contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .when().put("/doc/appointment/%s/move".formatted(uuid))
                .then().statusCode(Response.Status.FORBIDDEN.getStatusCode());
    }

    // --- Mass assignment: il creatore è preso dal token, non dal body ---

    @Test
    @DisplayName("(201) mass-assignment: creatorUpn inviato dal client viene IGNORATO")
    @Tag("security")
    @Tag("field-level")
    void testCreatorUpnIgnored() {
        String malicious = "{\"scientistUpn\": \"FERMI\",\"office\": \"FISICA\",\"appointmentAt\": \"%s\",\"creatorUpn\": \"BOHR\"}"
                .formatted(iso(48));
        given().header("Authorization", EINSTEIN)
                .body(malicious).contentType(ContentType.JSON).accept(ContentType.JSON)
                .when().post("/doc/appointment")
                .then().statusCode(Response.Status.CREATED.getStatusCode())
                .body("creatorUpn", Matchers.equalTo("EINSTEIN"));
    }

    // --- Business logic: niente doppia prenotazione (9g) ---

    @Test
    @DisplayName("(409) niente doppia prenotazione: stesso scienziato sullo stesso slot")
    @Tag("security")
    @Tag("business-logic")
    void testDoubleBookingSameSlotConflict() {
        // lo slot va catturato UNA volta sola e riusato, altrimenti due iso(48) potrebbero differire di un secondo
        String at = iso(48);
        given().header("Authorization", EINSTEIN)
                .body(body("FERMI", "FISICA", at)).contentType(ContentType.JSON).accept(ContentType.JSON)
                .when().post("/doc/appointment")
                .then().statusCode(Response.Status.CREATED.getStatusCode());
        given().header("Authorization", EINSTEIN)
                .body(body("FERMI", "FISICA", at)).contentType(ContentType.JSON).accept(ContentType.JSON)
                .when().post("/doc/appointment")
                .then().statusCode(Response.Status.CONFLICT.getStatusCode());
    }

    @Test
    @DisplayName("(201) due appuntamenti per lo stesso scienziato su slot diversi sono consentiti")
    @Tag("security")
    @Tag("authorized")
    @Tag("business-logic")
    void testDoubleBookingDifferentSlotOk() {
        createApptAs(EINSTEIN, 48);
        createApptAs(EINSTEIN, 72);
    }

    // --- Business logic: orizzonte massimo di prenotazione (9h) ---

    @Test
    @DisplayName("(422) non si può prenotare oltre l'orizzonte massimo (1 anno)")
    @Tag("security")
    @Tag("business-logic")
    void testCreateBeyondHorizon() {
        given().header("Authorization", EINSTEIN)
                .body(body("FERMI", "FISICA", iso(366 * 24))).contentType(ContentType.JSON).accept(ContentType.JSON)
                .when().post("/doc/appointment")
                .then().statusCode(422);
    }

    @Test
    @DisplayName("(422) non si può spostare un appuntamento oltre l'orizzonte massimo (1 anno)")
    @Tag("security")
    @Tag("business-logic")
    void testMoveBeyondHorizon() {
        String uuid = createApptAs(EINSTEIN, 48);
        given().header("Authorization", EINSTEIN)
                .body("{\"newAppointmentAt\": \"%s\"}".formatted(iso(366 * 24))).contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .when().put("/doc/appointment/%s/move".formatted(uuid))
                .then().statusCode(422);
    }

}
