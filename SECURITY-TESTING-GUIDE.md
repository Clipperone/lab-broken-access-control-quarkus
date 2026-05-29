# Guida ai Security Unit Test per i controlli di autorizzazione

> Riferimento aziendale per progettare e implementare **unit test di sicurezza sull'autorizzazione**
> (OWASP **A01 – Broken Access Control**). Complementa, non sostituisce, gli strumenti **SAST/DAST**:
> dove SAST/DAST segnalano *che* esiste un problema, gli unit test fissano *il comportamento atteso*
> e lo proteggono da regressioni nel tempo.

## Indice

- [Guida ai Security Unit Test per i controlli di autorizzazione](#guida-ai-security-unit-test-per-i-controlli-di-autorizzazione)
  - [Indice](#indice)
  - [Scopo e approccio](#scopo-e-approccio)
  - [Autenticazione vs Autorizzazione (401 vs 403)](#autenticazione-vs-autorizzazione-401-vs-403)
  - [Le classi di Broken Access Control coperte](#le-classi-di-broken-access-control-coperte)
  - [Anatomia di uno Unit Test per il controllo autorizzativo](#anatomia-di-uno-unit-test-per-il-controllo-autorizzativo)
  - [Ponte SAST/DAST → unit test](#ponte-sastdast--unit-test)
  - [Matrice di copertura](#matrice-di-copertura)
  - [Catalogo pattern \& anti-pattern](#catalogo-pattern--anti-pattern)
  - [Learning path](#learning-path)
    - [Riferimenti semplici — esercizi hands-on (TDD)](#riferimenti-semplici--esercizi-hands-on-tdd)
    - [Riferimenti complessi — esempi ed estensione](#riferimenti-complessi--esempi-ed-estensione)
  - [Checklist: aggiungere un nuovo security test](#checklist-aggiungere-un-nuovo-security-test)
  - [Comandi](#comandi)

## Scopo e approccio

L'obiettivo è formare gli sviluppatori a scrivere, **nel proprio codice applicativo**, test automatici
che verifichino i controlli autorizzativi. Esercizi ed esempi hanno **difficoltà incrementale**, per
fornire riferimenti sia semplici sia complessi da applicare all'interno del proprio codice
(vedi [Learning path](#learning-path)):

- **Riferimenti semplici** → esercizi hands-on TDD: parti da una vulnerabilità, fai fallire il test, applica la correzione.
- **Riferimenti complessi** → esempi da replicare, estensione della copertura, nuance architetturali.

Il focus è l'**autorizzazione**, *non* l'autenticazione.

## Autenticazione vs Autorizzazione (401 vs 403)

| | Domanda | Status | Tag | Esempio |
|---|---|---|---|---|
| **Autenticazione (authn)** | *"Chi sei?"* | `401 Unauthorized` | `unauthorized` | token mancante/scaduto/non valido |
| **Autorizzazione (authz)** | *"Cosa puoi fare?"* | `403 Forbidden` | `forbidden` | ruolo insufficiente per l'azione/oggetto/campo |

Errore classico: trattare i due come la stessa cosa. Un utente **autenticato** può comunque non essere
**autorizzato**. Questo laboratorio si concentra sui 403 e sulle classi di autorizzazione qui sotto;
i test 401 esistono (`DocResourceSicurezzaTest`) ma sono test di *autenticazione*.

## Le classi di Broken Access Control coperte

| Classe | Tag | Cosa verifica | Dove vederla |
|--------|-----|---------------|--------------|
| **Function Level Access Control** (escalation verticale, verb tampering) | `function-level` | un ruolo basso non può eseguire un'azione riservata; un verbo HTTP non dichiarato è rifiutato | [DocResourceFunctionLevelTest](src/test/java/org/fugerit/java/demo/lab/broken/access/control/DocResourceFunctionLevelTest.java) |
| **Object Level Authorization** (BOLA / IDOR) | `object-level` | non puoi accedere a un oggetto fuori dai tuoi permessi; un oggetto inesistente non è distinguibile (anti-enumeration) | `DocResourceSicurezzaTest.testFindPersonKoForbidden` / `testFindPersonKoNotFound` |
| **Mass assignment / Field-Level Authorization** | `field-level` | il client non controlla campi server-managed; un campo privilegiato è modificabile solo dal ruolo idoneo | [DocResourceFieldLevelTest](src/test/java/org/fugerit/java/demo/lab/broken/access/control/DocResourceFieldLevelTest.java) |
| **Data filtering per ruolo** (escalation orizzontale) | `authorized` + `security` | liste/documenti mostrano solo i dati consentiti al ruolo | `DocResourceSicurezzaTest.testListPersonsResultKo` / `testOkMarkDownConVerificaContenutoUser` |
| **Ownership-based access** (dati personali) | `ownership` | un dato è accessibile solo all'owner (e a un admin); modificabile solo dall'owner | [PersonalNoteResourceTest](src/test/java/org/fugerit/java/demo/lab/broken/access/control/PersonalNoteResourceTest.java) |
| **Multi-tenant / isolamento per ufficio** (+ gerarchia ruoli) | `tenant` | accesso definito da owner/ufficio/ruolo; un altro tenant non accede nemmeno se admin; draft/published; sharing | [OfficeDocumentResourceTest](src/test/java/org/fugerit/java/demo/lab/broken/access/control/OfficeDocumentResourceTest.java) |

## Anatomia di uno Unit Test per il controllo autorizzativo

Struttura del test **given / when / then** con RestAssured, e **due assert**: lo *status* e l'*effetto* (corpo/dati).

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
    .when().put("/doc/person/edit/%s".formatted(uuid))
    .then().statusCode(403);                                           // then: status
}
```

Principi:

1. **Scrivere sempre** il caso negativo *e* quello positivo. Il 403 dimostra che il controllo c'è; il 200 dimostra che non hai rotto la funzionalità.
2. **Verificare l'effetto, non solo lo status.** Es. dopo una modifica, controlla che il campo testato sia *rimasto invariato* (`testEditPersonUserCanEditAnagraphicFields`), o che la lista *non contenga* i dati riservati (`testListPersonsResultKo`).
3. **Identità: `@TestSecurity` o JWT.** Usa `@TestSecurity(roles=...)` per fissare una identità singola; usa i JWT di `DemoJwtGeneratorRest` quando un test richiede **più identità** (es. *admin crea, user modifica*).
4. **Isola i dati.** Non mutare i dati pre-caricati: crea dati freschi nel test (vedi `createPersonAsAdmin`).
5. **Messaggi di assert utili.** `assertEquals("guest", minRoleDopo, "minRole non deve cambiare ...")` aiuta chi legge in caso di test fallito.

## Ponte SAST/DAST → unit test

Quando uno strumento segnala un finding, traducilo in un **test di regressione** che blocchi la reintroduzione del problema. Mappa di riferimento:

| Finding tipico SAST/DAST | Classe OWASP A01 | Test di regressione da scrivere | Esempio nel repo |
|--------------------------|------------------|---------------------------------|------------------|
| Endpoint senza controllo di accesso / "missing authorization" | Function-level | ruolo insufficiente sull'azione → 403 | `DocResourceFunctionLevelTest.testAddPersonUserKo` |
| HTTP verb tampering / method override | Function-level | verbo non dichiarato → 405 | `DocResourceFunctionLevelTest.testVerbTamperingPutOnAddNotAllowed` |
| IDOR / "object reference not authorized" | BOLA / object-level | oggetto non consentito → 403; oggetto inesistente → 403 (anti-enumeration) | `DocResourceSicurezzaTest.testFindPersonKoForbidden` / `testFindPersonKoNotFound` |
| Mass assignment / over-posting | Field-level | campi server-controlled inviati dal client vengono ignorati | `DocResourceFieldLevelTest.testAddPersonIgnoresServerControlledFields` |
| Privilege escalation via parametro/campo | Field-level | ruolo basso non può valorizzare un campo privilegiato → 403 | `DocResourceFieldLevelTest.testEditPersonUserCannotChangeMinRole` |
| Escalation verticale su azione (delete/create) | Function-level | ruolo basso non può eseguire l'azione admin → 403 | `DocResourceSicurezzaTest.testDeletePersonUserKo` |
| Esposizione dati per ruolo / escalation orizzontale | Data filtering | la lista/documento è filtrata per ruolo | `DocResourceSicurezzaTest.testListPersonsResultKo` |
| IDOR su risorsa di proprietà / "broken object ownership" | Ownership | solo owner/admin accede al proprio dato; modifica solo owner | `PersonalNoteResourceTest.testOtherUserCannotReadNote` / `testNonOwnerAdminCannotEditNote` |
| Cross-tenant access / insecure multi-tenancy | Tenant isolation | un utente di un altro tenant (ufficio) non accede, **nemmeno admin** | `OfficeDocumentResourceTest.testCrossOfficeAdminForbidden` |
| Broken/weak authentication *(authn — fuori scope authz)* | — | token assente/scaduto/non valido → 401 | `DocResourceSicurezzaTest.testUnauthorizedWithoutJwt` / `testExpiredJWT` |

> Difesa strutturale complementare: `quarkus.security.jaxrs.deny-unannotated-endpoints=true` (deny-by-default)
> nega gli endpoint privi di annotazione di sicurezza, riducendo il rischio della classe
> "endpoint dimenticato senza controllo".

## Matrice di copertura

Esito atteso per **endpoint × ruolo** (✅ = test presente). Le celle senza test sono ottimi esercizi di estensione.

| Endpoint | admin | user | guest | anonimo |
|----------|-------|------|-------|---------|
| `GET /doc/example.md` | 200 ✅ | 200 | 200 ✅ | 401 ✅ |
| `GET /doc/example.html` | 200 | 200 ✅ | 403 | 401 |
| `GET /doc/example.adoc` | 200 ✅ | 403 | 403 ✅ | 401 |
| `GET /doc/example.pdf` | 200 ✅ | 403 ✅ | 403 ✅ | 401 ✅ |
| `GET /doc/person/list` | 200 (tutti) ✅ | 200 (filtrato) ✅ | 403 | 401 |
| `GET /doc/person/find/{uuid}` | 200 ✅ | 200 / 403 object ✅ | 403 | 401 |
| `POST /doc/person/add` | 201 ✅ | 403 ✅ | 403 ✅ | 401 |
| `PUT /doc/person/edit/{uuid}` | 200 (+minRole) ✅ | 200 anagrafica ✅ / 403 minRole ✅ | 403 object | 401 |
| `DELETE /doc/person/delete/{uuid}` | 200 ✅ | 403 ✅ | 403 | 401 |
| verbo non dichiarato (es. `PUT /doc/person/add`) | 405 ✅ | — | — | — |

## Catalogo pattern & anti-pattern

**Pattern (fai così):**
- Factory per i token (`DemoJwtGeneratorRest.generate*Token()`) invece di stringhe JWT incollate.
- Assert su *status + effetto*, con messaggio esplicativo.
- Tag a tre livelli: `security` (generico) + esito (`authorized`/`forbidden`) + classe (`object-level`/`function-level`/`field-level`).
- Dati di test creati nel test, non mutazione del seed.
- Per i campi privilegiati: autorizzazione sempre **lato server** con dati prelevati **lato server**; la validazione di formato (whitelist) è un filtro aggiuntivo, non un controllo di autorizzazione.

**Anti-pattern (evita):**
- ❌ JWT hardcoded lunghissimi nei sorgenti (difficili da mantenere; usali solo per casi specifici come "token scaduto").
- ❌ Solo casi positivi: i test negativi (403) sono il cuore dell'autorizzazione.
- ❌ Verificare solo lo status ignorando l'effetto (es. il campo è davvero rimasto invariato?).
- ❌ Confondere `401` (authn) e `403` (authz).
- ❌ Fidarsi del client per campi server-managed o privilegiati (mass assignment / field tampering).
- ❌ Mescolare tag implementativi (`@Tag("TestSecurity")`/`@Tag("Bearer")`) con i tag semantici di classe.

## Learning path

### Riferimenti semplici — esercizi hands-on (TDD)
1. Leggi [Autenticazione vs Autorizzazione](#autenticazione-vs-autorizzazione-401-vs-403).
2. Sul branch vulnerabile, per ogni `// VULNERABILITY: (n)`: esegui i test, osserva il rosso, applica la correzione fino al verde.
3. Estendi al nuovo scenario field-level/function-level seguendo gli esempi di riferimento.
4. Verifica con `mvn verify -P security`.

### Riferimenti complessi — esempi ed estensione
1. Studia [DocResourceFunctionLevelTest](src/test/java/org/fugerit/java/demo/lab/broken/access/control/DocResourceFunctionLevelTest.java) e [DocResourceFieldLevelTest](src/test/java/org/fugerit/java/demo/lab/broken/access/control/DocResourceFieldLevelTest.java).
2. Completa la [matrice di copertura](#matrice-di-copertura) per le celle senza test (es. `guest` su `list`/`find`).
3. Affronta le nuance: modello dei ruoli (il filtro `minRole in (...)` è *set-membership*, non gerarchia), separazione dei DTO per ruolo, deny-by-default.
4. Applica il [ponte SAST/DAST → unit test](#ponte-sastdast--unit-test) ai finding reali del tuo progetto.

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
