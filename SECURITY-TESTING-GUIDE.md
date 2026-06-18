# Unit Test per i controlli autorizzativi

Guida di riferimento per progettare e implementare **Unit Test di sicurezza dedicati ai controlli autorizzativi** (OWASP **A01 – Broken Access Control**).

Questi test **complementano, non sostituiscono**, SAST e DAST: SAST/DAST aiutano a individuare possibili difetti o superfici esposte; i test di autorizzazione fissano invece il **comportamento atteso** nel codice applicativo e lo proteggono da regressioni nel tempo.

> I test di autorizzazione dovrebbero coprire sia scenari positivi sia negativi. Gli scenari negativi sono particolarmente importanti per OWASP A01, perché dimostrano che un utente autenticato non può accedere a risorse, azioni o attributi fuori dal proprio perimetro autorizzativo, anche manipolando path parameter, query parameter o body della richiesta.

## Indice

- [Unit Test per i controlli autorizzativi](#unit-test-per-i-controlli-autorizzativi)
  - [Indice](#indice)
  - [Scopo e approccio](#scopo-e-approccio)
  - [Autenticazione vs Autorizzazione (401 vs 403)](#autenticazione-vs-autorizzazione-401-vs-403)
  - [Struttura di uno Unit Test per il controllo autorizzativo](#struttura-di-uno-unit-test-per-il-controllo-autorizzativo)
    - [given — prepara stato, dati e identità](#given--prepara-stato-dati-e-identità)
    - [when — esegue la singola azione sotto esame](#when--esegue-la-singola-azione-sotto-esame)
    - [then — verifica esito ed effetto reale](#then--verifica-esito-ed-effetto-reale)
  - [Pattern \& anti-pattern](#pattern--anti-pattern)
    - [Scelta del token factory](#scelta-del-token-factory)
    - [Anti-pattern da evitare](#anti-pattern-da-evitare)
  - [Scenari di Broken Access Control e test di riferimento](#scenari-di-broken-access-control-e-test-di-riferimento)
    - [1. Generazione documenti](#1-generazione-documenti)
    - [2. Lettura persone](#2-lettura-persone)
    - [3. Creazione e cancellazione persone](#3-creazione-e-cancellazione-persone)
    - [4. Modifica persona](#4-modifica-persona)
    - [5. Note personali (ownership)](#5-note-personali-ownership)
    - [6. Documenti d'ufficio (multi-tenant)](#6-documenti-dufficio-multi-tenant)
    - [7. Appuntamenti (visibilità multi-parte e regola temporale)](#7-appuntamenti-visibilità-multi-parte-e-regola-temporale)
  - [Checklist: aggiungere un nuovo security test](#checklist-aggiungere-un-nuovo-security-test)
  - [Comandi](#comandi)

## Scopo e approccio

L'obiettivo è fornire dei riferimenti per scrivere, **nel proprio codice applicativo**, test automatici
che verifichino i controlli autorizzativi. Il laboratorio propone esercizi ed esempi con **complessità incrementale**, per
fornire riferimenti sia semplici sia complessi da applicare all'interno del proprio codice
(dai più semplici — function/field-level — ai più articolati — ownership, multi-tenant, temporale:
vedi [Scenari di Broken Access Control e unit test di riferimento](#scenari-di-broken-access-control-e-unit-test-di-riferimento)).

Il focus è l'**autorizzazione**, *non* l'autenticazione.

## Autenticazione vs Autorizzazione (401 vs 403)

| | Domanda | Status | Tag | Esempio |
|---|---|---|---|---|
| **Autenticazione (authn)** | *"Chi sei?"* | `401 Unauthorized` | `unauthorized` | Token mancante, scaduto, malformato o non valido |
| **Autorizzazione (authz)** | *"Cosa puoi fare?"* | `403 Forbidden` | `forbidden` | ruolo insufficiente, owner errato, tenant errato, campo non modificabile |

>Errore classico: trattare i due come la stessa cosa. Un utente **autenticato** può comunque non essere **autorizzato**.

## Struttura di uno Unit Test per il controllo autorizzativo

Ogni test di autorizzazione segue lo schema **given / when / then**, espresso in modo fluente con RestAssured:

### given — prepara stato, dati e identità

Nel blocco `given` si costruisce lo scenario di partenza e si definisce l'identità del soggetto che esegue l'azione.

Preparare in modo esplicito:

- la risorsa oggetto del test
- il suo stato iniziale
- owner, tenant, ufficio, gruppo o perimetro di visibilità
- ruolo, permesso o claim richiesti dall'azione
- identità del chiamante
- eventuale payload di input

Nel contesto Quarkus, l'identità può essere simulata in modi diversi:

- con header HTTP `Authorization: Bearer <jwt>`, quando si vuole testare anche mapping dei claim, ruoli cumulativi, tenant, ufficio o configurazione del token;
- con `@TestSecurity`, quando si vuole fissare direttamente utente, ruoli e attributi nel contesto del test.

Il `given` deve rendere il test **deterministico, ripetibile e indipendente dagli altri test**. Evitare dipendenze da dati pre-caricati non controllati o dall'ordine di esecuzione della suite.

### when — esegue la singola azione sotto esame

Nel blocco `when` si esegue una sola azione applicativa o una singola chiamata HTTP verso l'endpoint da verificare.

La richiesta deve rappresentare esattamente il tentativo autorizzativo sotto esame:

- verbo HTTP `GET`/`PUT`/`POST`/`DELETE`;
- path;
- path parameter;
- query parameter;
- header rilevanti;
- body, se presente.

Regola pratica:

> **Un test = una decisione autorizzativa = una azione.**

Se servono più azioni per verificare comportamenti diversi, creare test separati. Le eventuali chiamate di setup, ad esempio `createPersonAsAdmin(...)`, sono accettabili purché restino parte del `given` e non siano l'oggetto del test.

### then — verifica esito ed effetto reale

Nel blocco `then` si verifica il risultato della decisione autorizzativa. Un test di autorizzazione non deve limitarsi a verificare che l’endpoint restituisca lo status HTTP atteso. Deve anche verificare che l’effetto reale sui dati sia coerente con la decisione autorizzativa: nessuna modifica non consentita, nessuna esposizione di dati fuori perimetro, nessun campo protetto alterato dal client e nessun side effect parziale nei casi di accesso negato.

---

Un test ben fatto contiene **due assert complementari**, che rispondono a due domande diverse:

1. **Lo *status* HTTP è quello atteso?** (`200`, `403`, `401`, `405`…). Dimostra che il *meccanismo* di controllo si è attivato e ha deciso come previsto — ad esempio un `403 Forbidden` quando un ruolo insufficiente prova un'azione riservata.
2. **L'*effetto* sui dati è corretto?** (corpo della risposta, stato persistito, contenuto delle liste). Lo status da solo può mentire: un endpoint potrebbe rispondere `200` e **comunque** aver applicato una modifica privilegiata o aver incluso dati riservati nella risposta. Per questo si verifica anche l'effetto reale — che il campo protetto sia *rimasto invariato*, che la lista *non contenga* i record fuori dal cono di visibilità, che owner/ufficio/stato siano quelli decisi dal server.

In breve: lo *status* certifica che il controllo è scattato, l'*effetto* certifica che ha prodotto la conseguenza giusta. Saltare il secondo assert è l'errore più comune e lascia passare proprio le vulnerabilità di Broken Access Control che vogliamo intercettare.

```java
@Test
@DisplayName("(403) field-level: un 'user' NON può modificare il campo privilegiato minRole")
@Tag("security")          // 1) tag generico di sicurezza (eseguito dal profilo `security`)
@Tag("forbidden")         // 2) esito HTTP atteso
@Tag("field-level")       // 3) classe di vulnerabilità (verificata dal gate)
void testEditPersonUserCannotChangeMinRole() {
    String uuid = createPersonAsAdmin(/* ... minRole=guest ... */);   // given: stato iniziale
    given()                                                            // when
        .header("Authorization", "Bearer " + DemoJwtGeneratorRest.generateUserToken())
        .body("{... \"minRole\": \"admin\"}").contentType(JSON)
    .when().put("/person/edit/%s".formatted(uuid))
    .then().statusCode(403);                                           // then: status
}
```

Principi:

1. **Scrivere sempre** il caso negativo *e* quello positivo. Il 403 dimostra che il controllo c'è; il 200 dimostra che non hai rotto la funzionalità.
2. **Verificare l'effetto, non solo lo status.** Es. dopo una modifica, controlla che il campo testato sia *rimasto invariato* (`testEditPersonUserCanEditAnagraphicFields`), o che la lista *non contenga* i dati riservati (`testListPersonsResultKo`).
3. **Identità: `@TestSecurity` o JWT.** Usa `@TestSecurity(roles=...)` per fissare una identità singola; usa i JWT di `DemoJwtGeneratorRest` quando un test richiede **più identità** (es. *admin crea, user modifica*).
4. **Isolare i dati.** Non utilizzare dati pre-caricati: crea dati nuovi nel test (vedi `createPersonAsAdmin`).
5. **Messaggi di assert utili.** `assertEquals("guest", minRoleDopo, "minRole non deve cambiare ...")` aiuta chi legge in caso di test fallito.

## Pattern & anti-pattern

**Pattern Consigliati:**
- Usare factory per i token, ad esempio `DemoJwtGeneratorRest.generate*Token()`, invece di JWT hardcoded nei sorgenti.
- Verificare sempre **status + effetto**.
- Usare messaggi di assert esplicativi.
- Scrivere il caso negativo e il caso positivo:
  - il negativo dimostra che il controllo blocca l'abuso
  - il positivo dimostra che la funzionalità legittima non è stata rotta
- Creare i dati necessari per il test all'interno del test stesso, non utilizzare i dati pre-caricati.
- Per i modifiche sui campi che riguardano il profilo o ruolo di un utente, che cambiano quindi il suo livello di autorizzazione, gestire sempre **lato server** con dati prelevati **lato server**; la validazione di tipo whitelist (es. il valore di minRole può essere solo uno tra guest|user|admin) è un filtro aggiuntivo, non un controllo di autorizzazione.
- Usare DTO separati per direzione e privilegi: request vs response, admin request vs user request, public response vs internal response.

### Scelta del token factory

Il metodo giusto dipende da che cosa decide il controllo.

| Il controllo decide in base a… | Factory da usare | Perché |
|---|---|---|
| Solo ruolo | `generateGuestToken()`, `generateUserToken()`, `generateAdminToken()` | L'identità non conta: bastano ruoli coerenti con lo scenario |
| Un ruolo singolo variabile | `generateToken("NONADMIN", ruolo)` | Utile per test parametrico: isola un solo ruolo |
| Identità / owner | `generateToken("EINSTEIN", ruoli...)` | Servono identità distinte e leggibili |
| Identità + ufficio / multi-tenant | `generateOfficeToken("EINSTEIN", "FISICA", ruoli...)` | Il claim di tenant/ufficio è parte dell'identità, non deve essere scelto dal client |

Regola pratica:

> Se per scrivere il test ti serve sapere **chi** agisce, e non solo **con che ruolo**, non usare scorciatoie generiche: crea identità esplicite con `generateToken`/`generateOfficeToken`.

### Anti-pattern da evitare

- ❌ JWT hardcoded lunghissimi nei sorgenti (difficili da mantenere; usali solo per casi specifici come "token scaduto").
- ❌ Solo casi positivi.
- ❌ Verificare solo lo status ignorando l'effetto reale.
- ❌ Confondere `401` e `403`.
- ❌ Fidarsi del client per campi server-managed o privilegiati (mass assignment / field tampering).
- ❌ Usare tag implementativi, come `Bearer` o `TestSecurity`, al posto dei tag semantici.
- ❌ Riutilizzare lo stesso DTO per input e output o per privilegi diversi.
- ❌ Demandare il controllo autorizzativo al frontend.

**Dettagli architetturali:**
- Il laboratorio implementa un **modello dei ruoli *set-membership*, non è implementata una gerarchia.** L'autorizzazione object-level confronta `securityIdentity.getRoles().contains(person.getMinRole())`: l'oggetto dichiara *un* ruolo richiesto (`minRole`) e l'accesso passa solo se quel ruolo è **presente nell'insieme** dei ruoli dell'utente. Non c'è alcun "≥": avere `admin` non implica di per sé soddisfare un `minRole = user`. In pratica funziona solo se i token sono coniati in modo **cumulativo** (`generateAdminToken` → `{admin, user, guest}`); un token con il solo `admin` *non* vedrebbe un oggetto con `minRole = user`. Dove serve davvero una gerarchia ordinata (`guest < user < admin`) la si modella esplicitamente con `RoleHierarchy.java` [RoleHierarchy](src/main/java/org/fugerit/java/demo/lab/broken/access/control/security/RoleHierarchy.java), come nello scenario multi-tenant — non la si dà per scontata.
- **Separa i DTO per ruolo e per direzione.** Non riusare la stessa classe per input e output né per tutti i livelli di privilegio: un campo privilegiato come `minRole` non dovrebbe nemmeno *comparire* nel contratto di chi non può modificarlo. DTO distinti (request vs response, e per livello di privilegio) chiudono mass assignment e over-posting alla radice, perché il campo sensibile non è proprio bindabile dal client non autorizzato — meglio non esporlo affatto che "ignorarlo" a runtime.
- **Deny-by-default.** Con `quarkus.security.jaxrs.deny-unannotated-endpoints=true` un endpoint privo di annotazione di sicurezza è **negato**, non aperto. È la rete di protezione contro l'"endpoint dimenticato": una rotta nuova, aggiunta senza pensare alla sicurezza, resta inaccessibile finché non dichiari esplicitamente chi può usarla, invece di finire esposta per distrazione.
---

## Scenari di Broken Access Control e test di riferimento

Questa sezione descrive gli scenari del laboratorio e le coppie di test più rappresentative, per ogni **scenario/endpoint** del laboratorio descrive *quali* controlli autorizzativi vanno verificati e *come* sono scritti i relativi unit test, mettendo sempre in luce la **coppia test negativo / test positivo**.

Come leggere ogni scenario:

- **❌ Test negativo** — un'azione illegittima viene **negata** (`403`/`405`/`401`). Dimostra che il controllo esiste e scatta. Il test è **superato** se l'azione non autorizzata non viene eseguita.
- **✅ Test positivo** — l'azione legittima **funziona** (`200`/`201`) e, dove conta, si verifica anche l'*effetto* (lista filtrata, campo invariato, owner/ufficio impostati dal server). Il test è **superato** se l'azione autorizzata viene eseguita correttamente.
- **Altri test della suite** — l'elenco completo dei restanti test dello scenario, così la mappa di copertura resta esaustiva anche se il codice è mostrato solo per la coppia rappresentativa.

Tabella-indice (classe OWASP A01 × caso d'uso × coppia di test):

| Scenario / Endpoint | Classe/i A01 | Cosa verificare | Esito | Test ❌ / ✅ |
|---|---|---|---|---|
| Generazione documenti — `GET /doc/example.{md,html,adoc,pdf}` | function-level + field-level (filtro contenuti) + authn | ruolo richiesto per formato; contenuto filtrato per ruolo; token valido | `403`, `401`, `200` | `testForbiddenWithJwt` ❌ `testOkMarkDownConVerificaContenutoUser` ✅ |
| Lettura persone — `GET /person/find/{uuid}`, `GET /person/list` | object-level (BOLA/IDOR) + anti-enumeration + data filtering | Accesso al singolo oggetto secondo `minRole`; lista filtrata | `403`, `200` | `testFindPersonKoForbidden` ❌ `testFindPersonOkUser` ✅ |
| Creazione/cancellazione persone — `POST /person/add`, `DELETE /person/delete/{uuid}` | function-level (escalation verticale) + verb tampering | Solo l' `admin` crea/cancella; verbo non dichiarato non invocabile | `403`, `405`, `201` | `testAddPersonNonAdminKo` ❌ `testAddPersonAdminOk` ✅ |
| Modifica persona — `PUT /person/edit/{uuid}` | field-level (`minRole`) + mass assignment | campo privilegiato solo da `admin`; campi server-managed ignorati | `403`, `200` | `testEditPersonUserCannotChangeMinRole` ❌ `testEditPersonUserCanEditAnagraphicFields` ✅ |
| Note personali — `GET/PUT /doc/note/{uuid}` | ownership | lettura a owner **o** admin; scrittura **solo** owner | `403`, `200` | `testNonOwnerAdminCannotEditNote` ❌ `testAdminReadsAnyNote` ✅ |
| Documenti d'ufficio — `/doc/officedoc` | tenant isolation + gerarchia ruoli + draft/published + sharing + mass assignment | isolamento per ufficio (anche fra admin); ruolo ≥ owner; bozza solo owner; sharing | `403`, `200` | `testCrossOfficeAdminForbidden` ❌ `testPublishedVisibleToSameOfficeHigherRole` ✅ |
| Appuntamenti — `/doc/appointment` | tenant/visibilità multi-parte + temporale + ownership + mass assignment | visibile a creatore/destinatario/admin d'ufficio; delete solo creatore e > 24h; move solo creatore | `403`, `200` | `testCreatorDeleteWithin24hForbidden` ❌ `testCreatorDeleteMoreThan24hOk` ✅ |

### 1. Generazione documenti

- **Endpoint:** `GET /doc/example.md|html|adoc|pdf`
- **Suite:** [DocResourceSicurezzaTest](src/test/java/org/fugerit/java/demo/lab/broken/access/control/DocResourceSicurezzaTest.java)
- **Classi:** function-level, field-level (filtro contenuti), autenticazione

Il documento è generato a partire dall'elenco persone, sono presenti due controlli:
- il **formato** richiede un ruolo (es. il PDF è riservato all'`admin`)
- il **contenuto** va filtrato per ruolo (un `user` non deve "vedere" nel
documento le persone con `minRole = admin`, come *Richard Feynman*). 

In più ci sono i test di *autenticazione* (401).

**❌ Test Negativo — function-level (formato riservato):** un utente con ruolo `guest` che chiede il formato PDF riceve `403`. Il ruolo basso non permette l'accesso ad un'azione riservata a un ruolo superiore.

```java
@Test
@DisplayName("(403) generazione documento, formato PDF con utente senza ruolo necessario (JWT), ruolo utente 'guest'.")
@Tag("security")
@Tag("forbidden")
@Tag("Bearer")
void testForbiddenWithJwt() {
        given().header("Authorization", "Bearer %s".formatted(DemoJwtGeneratorRest.generateGuestToken()))
        .when().get("/doc/example.pdf")
        .then().statusCode(Response.Status.FORBIDDEN.getStatusCode());
}
```

**✅ Test Positivo — field-level:** un ruolo `user` chiede il formato MarkDown, ottiene `200` **e** si verifica che il contenuto **non** includa la persona riservata. La sola verifica dello status non basterebbe, il controllo vero è sull'*effetto* (la stringa "Feynman" assente). Il test `testOkMarkDownConVerificaContenutoAdmin` verifica che l'`admin` invece la veda.

```java
@Test
@DisplayName("(200) generazione documento, formato MarkDown con utente che NON ha i ruoli per vedere tutti i risultati, ruolo utente 'user' e 'guest'.")
@Tag("security")
@Tag("success")
@Tag("Bearer")
void testOkMarkDownConVerificaContenutoUser() {
    String requestBody = given()
            .header("Authorization", String.format("Bearer %s", DemoJwtGeneratorRest.generateUserToken()))
            .when().get("/doc/example.md")
            .then().statusCode(Response.Status.OK.getStatusCode()).extract().body().asString();
    // i ruoli 'user o guest' NON hanno accesso alla persona 'Richard Feynman'
    Assertions.assertFalse(requestBody.contains("Feynman"));
}
```

**Altri test della suite:**
- `testHtmlOkNoAdminRole` / `testPdfOkNoAdminRole` — 200 sui formati consentiti (`user` su HTML; `admin` su PDF)
- `testMarkdown403NoAdminRole` — 403 sul formato riservato con `@TestSecurity`
- `testOkWithJwt` / `testOkJwtMarkDown` / `testOkJwtAsciiDoc` — 200 con JWT su PDF/MarkDown/AsciiDoc
- `testOkMarkDownConVerificaContenutoAdmin` — 200, l'`admin` **vede** "Feynman" (controprova del filtro field-level)
- `testForbiddenJwtAsciiDoc` — 403 con `guest` su formato riservato
- `testUnauthorizedWithoutJwt` / `testUnauthorizedWithWrongJwt` / `testExpiredJWT` / `testMarkdown401NoAuthorizationBearer` — **authn**: token assente/non valido/scaduto → 401

### 2. Lettura persone

- **Endpoint:** `GET /person/find/{uuid}`, `GET /person/list`
- **Suite:** [PersonResourceSicurezzaTest](src/test/java/org/fugerit/java/demo/lab/broken/access/control/PersonResourceSicurezzaTest.java)
- **Classi:** object-level (BOLA/IDOR), anti-enumeration, data filtering

Oltre al ruolo essere autorizzato per utilizzare l'endpoint, ogni persona ha un `minRole`: l'accesso al **singolo oggetto** va verificato (Object-Level Authorization). Questo requisito comporta due conseguenze:
- un oggetto fuori dal cono di visibilità è negato (`403`)
- un oggetto **inesistente** deve dare la **stessa** risposta del non autorizzato (`403`, non `404`) per non rivelare l'esistenza degli id (protezione anti-enumeration).

**❌ Test Negativo — object-level:** un utente con ruolo `user/guest` cerca di accedere ad un dato che per essere visualizzato richiede un ruolo `admin` e riceve `403`.

```java
@Test
@DisplayName("(403) trova persona con ruolo NON autorizzato, ruolo utente 'user' e 'guest'.")
@Tag("security")
@Tag("forbidden")
@Tag("object-level")
@TestSecurity(user = "USER1", roles = { "guest", "user" })
void testFindPersonKoForbidden() {
        given()
        .when().get("/person/find/10002")
        .then().statusCode(Response.Status.FORBIDDEN.getStatusCode());
}
```

**✅ Test Positivo — object-level:** lo stesso `user/guest` accede a una persona dentro il suo cono di visibilità (`minRole` compatibile), ottiene `200` e si verifica l'*effetto* (il dato atteso è nel corpo). Dimostra che il controllo non blocca gli accessi legittimi.

```java
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
```

**Altri test della suite (lettura):**
- `testFindPersonKoNotFound` — uuid inesistente → **403** (protezione anti-enumeration), non 404.
- `testFindPersonOkAdmin` — l'`admin` accede a una persona `minRole = admin` (200, corpo con "Feynman").
- `testListPersonsResultKo` — `user` su `/person/list`: 200 ma la lista **non** contiene "Feynman" (data filtering: stesso principio dello scenario 1, sull'*effetto*).
- `testListPersonsResultOk` — `admin` su `/person/list`: 200 e la lista **contiene** "Feynman" (controprova).

### 3. Creazione e cancellazione persone

- **Endpoint:** `POST /person/add`, `DELETE /person/delete/{uuid}`
- **Suite:** [PersonResourceFunctionLevelTest](src/test/java/org/fugerit/java/demo/lab/broken/access/control/PersonResourceFunctionLevelTest.java) (creazione, verb tampering) + [PersonResourceSicurezzaTest](src/test/java/org/fugerit/java/demo/lab/broken/access/control/PersonResourceSicurezzaTest.java) (cancellazione)
- **Classi:** function-level (escalation verticale), verb tampering

Le azioni di modifica (creare, cancellare) sono riservate all'`admin`: un ruolo basso che le invoca è
una **escalation verticale** e va negato (`403`). In più, un **verbo HTTP non dichiarato** sulla risorsa non deve essere invocabile: JAX-RS risponde `405 Method Not Allowed` come difesa contro il verb tampering.

**❌ Test Negativo — function-level (parametrico):** un'unica prova copre *tutti* i ruoli non-admin. Usa JWT reali (non `@TestSecurity`) perché il ruolo varia per invocazione; ogni token porta **un solo** ruolo non privilegiato, così
il `403` dipende solo dall'assenza di `admin`.

```java
@ParameterizedTest(name = "(403) function-level: il ruolo ''{0}'' (non-admin) NON può creare una persona")
@ValueSource(strings = { "user", "guest" })
@Tag("security")
@Tag("forbidden")
@Tag("function-level")
void testAddPersonNonAdminKo(String nonAdminRole) {
    String token = DemoJwtGeneratorRest.generateToken("NONADMIN", nonAdminRole);
        given().header("Authorization", "Bearer " + token).body(VALID_PERSON_JSON).contentType(ContentType.JSON).accept(ContentType.JSON)
        .when().post("/person/add")
        .then().statusCode(Response.Status.FORBIDDEN.getStatusCode());
}
```

**✅ Test Positivo — function-level:** l'`admin` crea la persona e ottiene `201`. Conferma che il controllo non
blocca chi ha il diritto.

```java
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
```

**❌ Test Negativo — verb tampering:** ci si autentica come `admin` (così l'**unica** ragione di rifiuto possibile è il verbo, non l'autorizzazione) e si invoca `PUT` su un path che dichiara solo `POST`: atteso `405`.

```java
@Test
@DisplayName("(405) verb tampering: PUT su /person/add (dichiara solo POST) → Method Not Allowed")
@Tag("security")
@Tag("function-level")
@TestSecurity(user = "USER2", roles = { "admin", "user", "guest" })
void testVerbTamperingPutOnAddNotAllowed() {
    // autenticati come admin: l'UNICA ragione di rifiuto deve essere il verbo non dichiarato, non l'autorizzazione
    given()
            .body(VALID_PERSON_JSON).contentType(ContentType.JSON)
            .when().put("/person/add")
            .then().statusCode(Response.Status.METHOD_NOT_ALLOWED.getStatusCode());
}
```



**Altri test della suite:**
- `testVerbTamperingDeleteOnListNotAllowed` — `DELETE` su `/person/list` (solo `GET`) → 405.
- `testDeletePersonUserKo` *(in PersonResourceSicurezzaTest)* — un `user` prova a cancellare → **403** (escalation verticale sulla delete).
- `testAddDeletePersonAdminOk` *(in PersonResourceSicurezzaTest)* — l'`admin` crea e poi cancella → 200 (dato fresco creato nel test).
- `testDeletePersonAdminKoNonEsiste` *(in PersonResourceSicurezzaTest)* — `admin` cancella un uuid inesistente → **403** (anti-enumeration).

### 4. Modifica persona

- **Endpoint:** `PUT /person/edit/{uuid}`
- **Suite:** [PersonResourceFieldLevelTest](src/test/java/org/fugerit/java/demo/lab/broken/access/control/PersonResourceFieldLevelTest.java)
- **Classi:** field-level (campo privilegiato `minRole`), mass assignment

L'autorizzazione non si ferma all'endpoint: anche quando un'azione è consentita, il server **non** deve fidarsi del
client per i **campi privilegiati**. Qui un `user` può modificare l'anagrafica, ma **non** il campo `minRole` (che
ne alzerebbe la visibilità): è *Field-Level Authorization*. Parallelamente, i campi **server-managed**
(`uuid`, `id`, `creationDate`) non devono essere accettati dal body (*mass assignment* / over-posting).

**❌ Test Negativo — field-level:** un `user` tenta di portare `minRole` a `admin` e riceve `403`. Si parte da un
dato fresco creato come admin (`createPersonAsAdmin`).

```java
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
```

**✅ Test Positivo — field-level:** lo stesso utente con ruolo `user` modifica **solo** i campi anagrafici
(request senza `minRole`): ottiene `200` e si verifica che `minRole` sia **rimasto invariato** (`"guest"`). Senza controllare l'effetto, una regressione che applica comunque `minRole` passerebbe inosservata.

```java
@Test
@DisplayName("(200) field-level: un 'user' può modificare i campi anagrafici; minRole resta invariato")
@Tag("security")
@Tag("authorized")
@Tag("field-level")
void testEditPersonUserCanEditAnagraphicFields() {
    String uuid = createPersonAsAdmin(
            "{\"firstName\": \"ROSALIND\",\"lastName\": \"FRANKLIN\",\"title\": \"Chimica\",\"minRole\": \"guest\"}");
    // request SENZA minRole: modifica consentita, il campo privilegiato non viene toccato
    String minRoleDopo = given()
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
```

**Altri test della suite:**
- `testEditPersonAdminCanChangeMinRole` — l'`admin` **può** portare `minRole` a `admin` (200, effetto verificato): controprova della regola field-level.
- `testAddPersonIgnoresServerControlledFields` — **mass assignment**: il client invia `uuid`/`id`/`creationDate`, ma il server li **ignora** e genera il proprio uuid (assert su `assertNotEquals` dell'uuid d'attacco).

### 5. Note personali (ownership)

- **Endpoint:** `GET/PUT /doc/note/{uuid}`
- **Suite:** [PersonalNoteResourceTest](src/test/java/org/fugerit/java/demo/lab/broken/access/control/PersonalNoteResourceTest.java)
- **Classe:** ownership

Una nota personale è un dato che ha una ownership: la **lettura** è concessa all'owner **o** a un admin, ma la **scrittura** è riservata **solo** all'owner. La coppia di test più istruttiva non è 403/200 sullo stesso verbo, ma l'**asimmetria read/write** sull'admin: stesso soggetto, esito opposto a seconda dell'azione. owner e identità derivano dal token (JWT reali: EINSTEIN owner, BOHR admin, PLANCK altro utente).

**❌ Test Negativo — scrittura riservata all'owner:** un admin (BOHR) prova a **modificare** la nota di EINSTEIN e
riceve `403`. Avere il ruolo admin abilita la lettura, **non** la scrittura del dato altrui.

```java
@Test
@DisplayName("(403) ownership: un admin può leggere ma NON modificare la nota altrui")
@Tag("security")
@Tag("forbidden")
@Tag("ownership")
void testNonOwnerAdminCannotEditNote() {
    String uuid = createNoteAs(EINSTEIN);
    given().header("Authorization", BOHR_ADMIN)
            .body(NOTE_JSON).contentType(ContentType.JSON).accept(ContentType.JSON)
            .when().put("/doc/note/%s".formatted(uuid))
            .then().statusCode(Response.Status.FORBIDDEN.getStatusCode());
}
```

**✅ Test Positivo — lettura concessa all'admin:** lo **stesso** admin (BOHR) **legge** la nota di EINSTEIN e ottiene
`200`. Messo accanto al test negativo, dimostra che la regola distingue correttamente lettura (owner *o* admin) da
scrittura (solo owner).

```java
@Test
@DisplayName("(200) ownership: un admin può leggere la nota di un altro utente")
@Tag("security")
@Tag("authorized")
@Tag("ownership")
void testAdminReadsAnyNote() {
    String uuid = createNoteAs(EINSTEIN);
    given().header("Authorization", BOHR_ADMIN)
            .when().get("/doc/note/%s".formatted(uuid))
            .then().statusCode(Response.Status.OK.getStatusCode());
}
```

**Altri test della suite:**
- `testOwnerReadsOwnNote` — l'owner legge la propria nota → 200, con `ownerUpn == "EINSTEIN"` (effetto).
- `testOtherUserCannotReadNote` — un utente non-owner e non-admin (PLANCK) → **403** in lettura.
- `testOwnerCanEditNote` — l'owner modifica la propria nota → 200 (titolo aggiornato, effetto verificato).
- `testReadNonExistentNote` — nota inesistente → **403** (anti-enumeration, indistinguibile dal non autorizzato).

### 6. Documenti d'ufficio (multi-tenant)

- **Endpoint:** `/doc/officedoc` (+ `/publish`, `/share`)
- **Suite:** [OfficeDocumentResourceTest](src/test/java/org/fugerit/java/demo/lab/broken/access/control/OfficeDocumentResourceTest.java) 
- **Classi:** tenant isolation, gerarchia ruoli, ciclo draft/published, sharing, mass assignment

Scenario più ricco: l'accesso dipende da **ufficio** (claim `office` nel token), **ruolo ≥ soglia dell'owner**
(qui la gerarchia è esplicita, via `RoleHierarchy`), **stato** (una bozza è visibile solo all'owner finché non è
pubblicata) e **sharing** esplicito. Regola chiave: l'isolamento per ufficio è **assoluto**, vale **anche tra admin**
di uffici diversi. owner/ufficio/stato sono impostati **lato server**, mai dal client.

**❌ Test Negativo — tenant isolation:** un admin di un altro ufficio (MENDELEEV / CHIMICA) prova a leggere un
documento pubblicato di FISICA e riceve `403`. Essere admin **non** supera il confine di tenant.

```java
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
```

**✅ Test Positivo — stesso ufficio + ruolo ≥ + pubblicato:** dopo la pubblicazione, l'admin **dello stesso ufficio**
(BOHR / FISICA, ruolo ≥ owner) legge il documento → `200`. Tutte le condizioni (ufficio, ruolo, stato) sono
soddisfatte.

```java
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
```

**✅ Test Positivo — mass assignment (owner/ufficio/stato server-side):** il client prova a impostare nel body
`ownerUpn`/`ownerOffice`/`ownerRole`/`status`, ma il server li **ignora** e usa i valori derivati dal token (owner =
chi chiama, ufficio = FISICA, stato = DRAFT). L'effetto è verificato sui campi della risposta.

```java
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
```

**Altri test della suite:**
- `testOwnerReadsOwnDraft` — l'owner legge la propria bozza → 200 (`status == DRAFT`, `ownerOffice == FISICA`).
- `testDraftNotVisibleToOfficeAdmin` — la bozza **non** è visibile all'admin di ufficio finché non è pubblicata → 403.
- `testSameOfficeLowerRoleForbidden` — stesso ufficio ma **ruolo inferiore** all'owner (PLANCK/guest) → 403 (gerarchia).
- `testAntiEnumerationNonExistent` — uuid inesistente → **403** (anti-enumeration).
- `testOwnerCanEdit` / `testOfficeAdminCanEditPublished` — modifica consentita a owner e ad admin stesso ufficio su PUBLISHED → 200.
- `testSameOfficeNonAdminCannotEdit` — stesso ufficio, può leggere ma **non** è admin → 403 in modifica.
- `testCrossOfficeAdminCannotEdit` — admin di ufficio diverso → 403 anche in modifica.
- `testSharingGrantsCrossOfficeRead` / `testNotSharedCrossOfficeForbidden` — lo **sharing** esplicito abilita la lettura cross-office (200); senza condivisione resta 403.

### 7. Appuntamenti (visibilità multi-parte e regola temporale)

**Endpoint:** `/doc/appointment` (+ `/move`) · **Suite:** [AppointmentResourceTest](src/test/java/org/fugerit/java/demo/lab/broken/access/control/AppointmentResourceTest.java) · **Classi:** tenant/visibilità multi-parte (relationship-based), autorizzazione temporale, ownership, mass assignment

Un appuntamento collega più soggetti: è visibile a **creatore**, **scienziato destinatario** e **admin dello stesso
ufficio** (visibilità *relationship-based*). Su di esso agiscono due controlli ulteriori: l'**ownership** (solo il
creatore può cancellare/spostare) e una regola **temporale** (la cancellazione è consentita solo se mancano **> 24h**
all'appuntamento). Le date sono calcolate a runtime nel test (`iso(plusHours)`).

**❌ Test Negativo — autorizzazione temporale:** il **creatore** prova a cancellare un appuntamento a **meno di 24h**
e riceve `403`. Il diritto esiste (è il creatore), ma la **finestra temporale** lo nega: l'autorizzazione dipende dal
contesto, non solo dall'identità.

```java
@Test
@DisplayName("(403) il creatore NON può eliminare a meno di 24h dall'appuntamento (regola temporale)")
@Tag("security")
@Tag("forbidden")
@Tag("temporal")
void testCreatorDeleteWithin24hForbidden() {
    String uuid = createApptAs(EINSTEIN, 12);
    given().header("Authorization", EINSTEIN)
            .when().delete("/doc/appointment/%s".formatted(uuid))
            .then().statusCode(Response.Status.FORBIDDEN.getStatusCode());
}
```

**✅ Test Positivo — autorizzazione temporale:** lo **stesso** creatore cancella un appuntamento a **più di 24h**
(48h) e ottiene `200`. La coppia 12h/48h isola esattamente la regola temporale, a parità di identità e azione.

```java
@Test
@DisplayName("(200) il creatore elimina un appuntamento a più di 24h")
@Tag("security")
@Tag("authorized")
@Tag("temporal")
void testCreatorDeleteMoreThan24hOk() {
    String uuid = createApptAs(EINSTEIN, 48);
    given().header("Authorization", EINSTEIN)
            .when().delete("/doc/appointment/%s".formatted(uuid))
            .then().statusCode(Response.Status.OK.getStatusCode());
}
```

**Altri test della suite:**
- `testCreatorCanView` / `testScientistCanView` / `testOfficeAdminCanView` — visibilità **multi-parte**: creatore, destinatario e admin di ufficio leggono → 200.
- `testCrossOfficeAdminForbidden` — admin di **altro** ufficio → 403 (isolamento di tenant).
- `testUnrelatedSameOfficeForbidden` — estraneo dello **stesso** ufficio, non creatore/destinatario/admin → 403 (la visibilità è per **relazione**, non per ufficio).
- `testAntiEnumerationNonExistent` — uuid inesistente → **403** (anti-enumeration).
- `testNonCreatorCannotDelete` — un non-creatore (anche il destinatario) → 403 sulla delete (**ownership**).
- `testCreatorCanMove` / `testNonCreatorCannotMove` — lo spostamento è consentito **solo** al creatore (200 vs 403).
- `testCreatorUpnIgnored` — **mass assignment**: `creatorUpn` inviato dal client è ignorato; il creatore è preso dal token (effetto verificato: `creatorUpn == "EINSTEIN"`).

## Checklist: aggiungere un nuovo security test

- [ ] Individua l'**azione** (verbo + path) e i **ruoli** attesi.
- [ ] Scrivi il caso **negativo** (403) *e* il **positivo** (200/201).
- [ ] Scegli l'identità: `@TestSecurity` (singola) o JWT (multi-identità).
- [ ] Assert su **status** *e* **effetto** (dati/campo), con messaggio.
- [ ] Aggiungi i **tag**: `security` + esito + classe.
- [ ] Esegui `mvn verify -P security` (il gate verifica la copertura delle classi).

## Comandi

```shell
mvn verify -P security   # esegue i test taggati e verifica la copertura delle classi di sicurezza
mvn quarkus:dev          # avvio in dev; Swagger UI su http://localhost:8080/q/swagger-ui/
```

Report del gate: `target/executed-test-tag-report.html`.
