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
 * ESEMPIO DI RIFERIMENTO — Ownership-based access control (SCENARIO 1).
 *
 * <p>
 * Una nota personale è visibile all'owner o a un admin; modificabile solo dall'owner. Usa JWT reali
 * (più identità per test): ALICE/BOB(admin)/CAROL.
 * </p>
 */
@QuarkusTest
@Slf4j
class PersonalNoteResourceTest {

    private static final String ALICE = "Bearer %s".formatted(DemoJwtGeneratorRest.generateToken("ALICE", "user", "guest"));
    private static final String BOB_ADMIN = "Bearer %s"
            .formatted(DemoJwtGeneratorRest.generateToken("BOB", "admin", "user", "guest"));
    private static final String CAROL = "Bearer %s".formatted(DemoJwtGeneratorRest.generateToken("CAROL", "user", "guest"));

    private static final String NOTE_JSON = "{\"title\": \"Promemoria\",\"content\": \"contenuto riservato\"}";

    private String createNoteAs(String authHeader) {
        return given().header("Authorization", authHeader)
                .body(NOTE_JSON).contentType(ContentType.JSON).accept(ContentType.JSON)
                .when().post("/doc/note")
                .then().statusCode(Response.Status.CREATED.getStatusCode())
                .extract().path("uuid");
    }

    @Test
    @DisplayName("(200) ownership: l'owner legge la propria nota")
    @Tag("security")
    @Tag("authorized")
    @Tag("ownership")
    void testOwnerReadsOwnNote() {
        String uuid = createNoteAs(ALICE);
        given().header("Authorization", ALICE)
                .when().get("/doc/note/%s".formatted(uuid))
                .then().statusCode(Response.Status.OK.getStatusCode())
                .body("ownerUpn", Matchers.equalTo("ALICE"));
    }

    @Test
    @DisplayName("(200) ownership: un admin può leggere la nota di un altro utente")
    @Tag("security")
    @Tag("authorized")
    @Tag("ownership")
    void testAdminReadsAnyNote() {
        String uuid = createNoteAs(ALICE);
        given().header("Authorization", BOB_ADMIN)
                .when().get("/doc/note/%s".formatted(uuid))
                .then().statusCode(Response.Status.OK.getStatusCode());
    }

    @Test
    @DisplayName("(403) ownership: un altro utente non-admin NON può leggere la nota altrui")
    @Tag("security")
    @Tag("forbidden")
    @Tag("ownership")
    void testOtherUserCannotReadNote() {
        String uuid = createNoteAs(ALICE);
        given().header("Authorization", CAROL)
                .when().get("/doc/note/%s".formatted(uuid))
                .then().statusCode(Response.Status.FORBIDDEN.getStatusCode());
    }

    @Test
    @DisplayName("(200) ownership: l'owner può modificare la propria nota")
    @Tag("security")
    @Tag("authorized")
    @Tag("ownership")
    void testOwnerCanEditNote() {
        String uuid = createNoteAs(ALICE);
        given().header("Authorization", ALICE)
                .body("{\"title\": \"Promemoria aggiornato\",\"content\": \"nuovo contenuto\"}")
                .contentType(ContentType.JSON).accept(ContentType.JSON)
                .when().put("/doc/note/%s".formatted(uuid))
                .then().statusCode(Response.Status.OK.getStatusCode())
                .body("title", Matchers.equalTo("Promemoria aggiornato"));
    }

    @Test
    @DisplayName("(403) ownership: un admin può leggere ma NON modificare la nota altrui")
    @Tag("security")
    @Tag("forbidden")
    @Tag("ownership")
    void testNonOwnerAdminCannotEditNote() {
        String uuid = createNoteAs(ALICE);
        given().header("Authorization", BOB_ADMIN)
                .body(NOTE_JSON).contentType(ContentType.JSON).accept(ContentType.JSON)
                .when().put("/doc/note/%s".formatted(uuid))
                .then().statusCode(Response.Status.FORBIDDEN.getStatusCode());
    }

    @Test
    @DisplayName("(403) ownership/anti-enumeration: nota inesistente → 403 (come 'non autorizzato')")
    @Tag("security")
    @Tag("forbidden")
    @Tag("ownership")
    void testReadNonExistentNote() {
        given().header("Authorization", ALICE)
                .when().get("/doc/note/%s".formatted("00000000-0000-0000-0000-000000000000"))
                .then().statusCode(Response.Status.FORBIDDEN.getStatusCode());
    }

}
