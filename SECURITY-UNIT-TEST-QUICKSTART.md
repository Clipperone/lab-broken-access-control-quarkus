# Security Unit Test — Quickstart

> Guida introduttiva per chi si avvicina per la prima volta al testing del Broken Access Control: dall'inquadramento teorico a un primo test funzionante. Per gli scenari completi e i pattern avanzati si rimanda a [SECURITY-UNIT-TEST.md](SECURITY-UNIT-TEST.md).

---

## Broken Access Control: inquadramento

**Broken Access Control (OWASP A01)** si verifica quando un'applicazione non verifica che l'utente autenticato abbia il *diritto* di eseguire una determinata azione su un determinato dato. Casi tipici:

- Un `user` riesce a cancellare una risorsa riservata agli `admin` (escalation verticale di privilegio).
- Un utente accede ai dati di un altro utente modificando l'UUID nell'URL (BOLA/IDOR).
- Un campo privilegiato (es. `ruolo`) viene modificato dal client senza controllo (mass assignment / field-level authorization).

Un **unit test di autorizzazione** dimostra — in modo automatico e ripetibile — che il controllo *esiste* e *scatta* correttamente.

---

## Prerequisiti

- Quarkus con `quarkus-test-security` e `rest-assured` nel classpath di test (già presenti in questo progetto).
- Un endpoint su cui esercitare il controllo.
- Due identità distinte: una *autorizzata* e una *non autorizzata*.

```xml
<!-- pom.xml — già incluse nel progetto -->
<dependency>
  <groupId>io.quarkus</groupId>
  <artifactId>quarkus-test-security</artifactId>
  <scope>test</scope>
</dependency>
<dependency>
  <groupId>io.rest-assured</groupId>
  <artifactId>rest-assured</artifactId>
  <scope>test</scope>
</dependency>
```

---

## Il pattern fondamentale: test in coppia

Un test di autorizzazione **non è mai isolato**. È necessaria sempre la coppia:

| Test | Cosa verifica | Esito atteso |
|------|---------------|--------------|
| ❌ **Negativo** | L'azione non autorizzata viene *bloccata* | `403 Forbidden` |
| ✅ **Positivo** | L'azione autorizzata *funziona* | `200 / 201 OK` |

Il test negativo dimostra che il controllo esiste. Il test positivo dimostra che il controllo non ostacola gli accessi legittimi.

---

## Esempio base: endpoint riservato all'admin

### Scenario

Endpoint `POST /person/add` per la creazione di una persona. L'accesso è riservato agli `admin`: un `user` non deve essere in grado di completare l'operazione.

### Test negativo (403)

```java
@QuarkusTest
class PersonResourceFunctionLevelTest {

    private static final String VALID_PERSON_JSON =
        "{\"firstName\":\"TEST\",\"lastName\":\"USER\",\"title\":\"Dev\",\"minRole\":\"guest\"}";

    // ❌ Identità non autorizzata → atteso 403
    @Test
    @DisplayName("(403) function-level: ruolo 'user' NON può creare una persona")
    @Tag("security")
    @Tag("forbidden")
    @Tag("function-level")
    @TestSecurity(user = "TESTUSER", roles = { "user" })
    void testAddPersonNonAdminKo() {
        given()
            .body(VALID_PERSON_JSON)
            .contentType(ContentType.JSON)
            .when().post("/person/add")
            .then().statusCode(Response.Status.FORBIDDEN.getStatusCode()); // 403
    }
}
```

**Nota su `@TestSecurity`**: l'annotazione inietta nel contesto di sicurezza di Quarkus un'identità con upn `TESTUSER` e ruolo `user`, senza generare un JWT reale. Questa modalità è veloce e indicata per scenari che coinvolgono una sola identità alla volta.

### Test positivo (201)

```java
    // ✅ Identità autorizzata → atteso 201
    @Test
    @DisplayName("(201) function-level: ruolo 'admin' PUÒ creare una persona")
    @Tag("security")
    @Tag("authorized")
    @Tag("function-level")
    @TestSecurity(user = "ADMINUSER", roles = { "admin", "user", "guest" })
    void testAddPersonAdminOk() {
        given()
            .body(VALID_PERSON_JSON)
            .contentType(ContentType.JSON).accept(ContentType.JSON)
            .when().post("/person/add")
            .then().statusCode(Response.Status.CREATED.getStatusCode()); // 201
    }
```

> **Ruoli cumulativi**: in questo progetto i token includono tutti i ruoli gerarchicamente inferiori (`admin` porta anche `user` e `guest`). In questo modo il controllo `contains("admin")` funziona senza richiedere una gerarchia esplicita. Per i dettagli si rimanda a [SECURITY-UNIT-TEST.md](SECURITY-UNIT-TEST.md).

### Esecuzione

```bash
# Esecuzione della sola classe di test
mvn test -Dtest=PersonResourceFunctionLevelTest

# Esecuzione completa con verifica della copertura dei tag
mvn verify -P security
```

---

## Risultato atteso

Al termine dell'esecuzione con `mvn verify -P security`:

```
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

Nel report del gate (`target/executed-test-tag-report.html`) risulteranno eseguiti i tag `security`, `forbidden`, `authorized` e `function-level`. L'assenza di un tag obbligatorio causa il fallimento della build: è il meccanismo che garantisce la copertura dei requisiti di sicurezza.

---

## JWT reali vs @TestSecurity

`@TestSecurity` è la scelta appropriata per scenari **mono-identità**. Quando il test richiede **più identità in sequenza** (es. un admin crea una risorsa, poi un user prova a modificarla), è necessario utilizzare JWT reali:

```java
// Token con ruolo singolo — la causa del 403 è il ruolo, non il JWT
String token = DemoJwtGeneratorRest.generateToken("NONADMIN", "user");

given()
    .header("Authorization", "Bearer " + token)
    .body(VALID_PERSON_JSON).contentType(ContentType.JSON)
    .when().post("/person/add")
    .then().statusCode(403);
```

---

## Tag obbligatori

I tag seguenti sono **obbligatori**: la loro assenza causa il fallimento di `mvn verify -P security`.

| Tag | Quando applicarlo |
|-----|-------------------|
| `security` | Su **ogni** test di autorizzazione — è il gruppo primario |
| `authorized` | Esito positivo (200/201) |
| `forbidden` | Esito 403 |
| `unauthorized` | Esito 401 (token mancante o non valido) |
| `function-level` | Ruolo insufficiente per l'azione (escalation verticale) |
| `object-level` | Accesso a un oggetto con `minRole` superiore al ruolo del richiedente |
| `field-level` | Campo privilegiato non modificabile dal ruolo del richiedente |
| `ownership` | Risorsa modificabile solo dall'owner |
| `tenant` | Isolamento multi-tenant (ufficio/organizzazione) |
| `temporal` | Autorizzazione dipendente da vincoli temporali |

---

## Checklist

- [ ] Test **negativo** implementato (403 per l'identità non autorizzata)?
- [ ] Test **positivo** implementato (200/201 per l'identità autorizzata)?
- [ ] Verificato l'**effetto** oltre allo status (es. il campo privilegiato non è stato modificato)?
- [ ] Aggiunti i **tag** `security` + esito + classe di vulnerabilità?
- [ ] `mvn verify -P security` è verde?

---

## Approfondimenti

| Argomento | Riferimento |
|-----------|-------------|
| Scenari completi con codice (BOLA, field-level, ownership, multi-tenant, temporal) | [SECURITY-UNIT-TEST.md](SECURITY-UNIT-TEST.md) |
| Catalogo dei test esistenti per livello di difficoltà | [GUIDA-OPERATIVA.md](GUIDA-OPERATIVA.md) — Sezione 3 |
| Architettura del progetto e comandi Maven | [README.md](README.md) |
| Avvio e utilizzo delle API in dev (Swagger UI, console didattica) | [GUIDA-OPERATIVA.md](GUIDA-OPERATIVA.md) — Sezione 1 |
