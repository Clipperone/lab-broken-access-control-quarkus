# Security Unit Test — Quickstart

> Hai il tuo codice, qualcuno ti ha detto *"devi testare il Broken Access Control"* e non sai da dove iniziare? Questa guida ti porta da zero a un primo test funzionante in meno di 15 minuti. Per lo scenario completo e i pattern avanzati vedi [SECURITY-UNIT-TEST.md](SECURITY-UNIT-TEST.md).

---

## Cos'è il Broken Access Control (in due righe)

**Broken Access Control (OWASP A01)** = l'applicazione non verifica che l'utente autenticato abbia il *diritto* di eseguire quella specifica azione su quel dato. Esempi tipici:

- Un `user` riesce a cancellare una risorsa riservata agli `admin` (escalation verticale).
- Un utente accede ai dati di un altro utente modificando l'UUID nell'URL (BOLA/IDOR).
- Un campo privilegiato (es. `ruolo`) viene modificato dal client senza controllo (mass assignment / field-level).

Un **unit test di autorizzazione** dimostra — in modo automatico e ripetibile — che il controllo *esiste* e *scatta* correttamente.

---

## Cosa ti serve

- Quarkus con `quarkus-test-security` e `rest-assured` nel classpath di test (già presenti se usi questo progetto).
- Un endpoint su cui testare.
- Due identità: una *autorizzata* e una *non autorizzata*.

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

## Il pattern fondamentale: sempre in coppia

Un test di autorizzazione **non è mai uno solo**. Serve *sempre* la coppia:

| Test | Cosa verifica | Esito atteso |
|------|---------------|--------------|
| ❌ **Negativo** | L'azione non autorizzata viene *bloccata* | `403 Forbidden` |
| ✅ **Positivo** | L'azione autorizzata *funziona* | `200 / 201 OK` |

Il test negativo dimostra che il controllo esiste. Il test positivo dimostra che non blocca gli accessi legittimi (e che non hai rotto nulla).

---

## Esempio base: endpoint riservato all'admin

### Scenario

Hai un endpoint `POST /person/add` che crea una persona. Solo gli `admin` possono farlo — un `user` non deve riuscirci.

### Step 1 — Scrivi il test negativo (403)

```java
@QuarkusTest
class PersonResourceFunctionLevelTest {

    private static final String VALID_PERSON_JSON =
        "{\"firstName\":\"TEST\",\"lastName\":\"USER\",\"title\":\"Dev\",\"minRole\":\"guest\"}";

    // ❌ Un non-admin che prova a creare → deve ricevere 403
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

**Cosa fa materialmente `@TestSecurity`**: inietta nel contesto di sicurezza di Quarkus un'identità con upn `TESTUSER` e ruolo `user`, senza generare un JWT reale. Veloce, ideale per testare *un solo* ruolo alla volta.

### Step 2 — Scrivi il test positivo (201)

```java
    // ✅ Un admin che crea → deve ricevere 201
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

> **Perché i ruoli sono cumulativi?** In questo progetto i token sono coniati con tutti i ruoli "inferiori" inclusi (`admin` porta anche `user` e `guest`). Così il controllo `contains("admin")` funziona senza una gerarchia esplicita. Vedi [SECURITY-UNIT-TEST.md](SECURITY-UNIT-TEST.md) per i dettagli.

### Step 3 — Esegui i test

```bash
# Esegui solo questa classe
mvn test -Dtest=PersonResourceFunctionLevelTest

# Esegui tutti i test di sicurezza + verifica la copertura dei tag
mvn verify -P security
```

---

## Cosa trovi alla fine

Dopo aver scritto la coppia di test e averla eseguita con `mvn verify -P security`:

```
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

E nel report del gate (`target/executed-test-tag-report.html`) vedrai che i tag `security`, `forbidden`, `authorized` e `function-level` sono stati eseguiti. Se dimentichi un tag, la build fallisce — è il *safety net* che garantisce la copertura.

---

## Quando usare JWT reali invece di @TestSecurity

`@TestSecurity` è perfetto per scenari **mono-identità**. Se il tuo test richiede **più identità** (es. admin crea una risorsa, poi user prova a modificarla), usa i JWT reali:

```java
// Token con ruolo singolo — l'UNICA ragione del 403 è il ruolo, non il JWT
String token = DemoJwtGeneratorRest.generateToken("NONADMIN", "user");

given()
    .header("Authorization", "Bearer " + token)
    .body(VALID_PERSON_JSON).contentType(ContentType.JSON)
    .when().post("/person/add")
    .then().statusCode(403);
```

---

## Tag da usare

I tag sono **obbligatori** o `mvn verify -P security` fallisce:

| Tag | Quando usarlo |
|-----|---------------|
| `security` | Su **ogni** test di autorizzazione — è il gruppo primario |
| `authorized` | Esito positivo (200/201) |
| `forbidden` | Esito 403 |
| `unauthorized` | Esito 401 (token mancante/non valido) |
| `function-level` | Ruolo sbagliato per l'azione (escalation verticale) |
| `object-level` | Accesso a un oggetto con `minRole` superiore al tuo |
| `field-level` | Campo privilegiato non modificabile dal tuo ruolo |
| `ownership` | Risorsa modificabile solo dall'owner |
| `tenant` | Isolamento multi-tenant (ufficio/organizzazione) |
| `temporal` | Autorizzazione dipendente dal tempo |

---

## Checklist rapida

- [ ] Hai scritto il test **negativo** (403 per l'utente non autorizzato)?
- [ ] Hai scritto il test **positivo** (200/201 per l'utente autorizzato)?
- [ ] Hai verificato l'**effetto** oltre allo status (es. il campo privilegiato non è cambiato)?
- [ ] Hai aggiunto i **tag** `security` + esito + classe?
- [ ] `mvn verify -P security` è verde?

---

## Prossimi passi

| Vuoi capire… | Vai a… |
|---|---|
| Tutti gli scenari con codice reale (BOLA, field-level, ownership, multi-tenant, temporal) | [SECURITY-UNIT-TEST.md](SECURITY-UNIT-TEST.md) |
| L'elenco completo dei test esistenti per livello di difficoltà | [GUIDA-OPERATIVA.md](GUIDA-OPERATIVA.md) — Sezione 3 |
| L'architettura del progetto e i comandi Maven | [README.md](README.md) |
| Come avviare e provare le API in dev (Swagger UI, console didattica) | [GUIDA-OPERATIVA.md](GUIDA-OPERATIVA.md) — Sezione 1 |
