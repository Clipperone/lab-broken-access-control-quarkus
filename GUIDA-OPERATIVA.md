# Guida operativa: avvio, API e test di sicurezza

Documento di onboarding per lo sviluppatore: cosa fare appena aperto il progetto, l'elenco di **tutti i
metodi esposti** (OpenAPI) con una descrizione sintetica, e il **catalogo dei test di sicurezza** dal
più basilare al più avanzato. Per il *come progettare* i test vedi [SECURITY-TESTING-GUIDE.md](SECURITY-TESTING-GUIDE.md).

> Legenda esiti: **401** = non autenticato (*chi sei?*) · **403** = autenticato ma non autorizzato
> (*cosa puoi fare?*) · **405** = metodo HTTP non consentito · **200/201** = operazione consentita.

---

## 1. Primi passi (step-by-step)

1. **Prerequisiti**: Java 21+, Maven 3.9.x.
2. **Verifica l'ambiente**: `mvn verify -P security` → deve essere verde (esegue i test di sicurezza e il gate sui tag).
3. **Avvia in dev**: `mvn quarkus:dev` → apri la [Swagger UI](http://localhost:8080/q/swagger-ui/).
4. **Ottieni un token** (endpoint demo) e premi **Authorize** incollando `Bearer <token>`:
   - ruoli semplici: `GET /demo/admin,user.txt`
   - con ufficio (scenari multi-tenant): `GET /demo/office/FISICA/EINSTEIN/user,guest.txt`
5. **Prova gli endpoint** con ruoli/uffici diversi e osserva **200 / 401 / 403 / 405**: è il modo più rapido per comprendere le regole.
6. **Leggi lo scenario** nel [README.md](README.md) (ruoli, persone, mappa permessi); le identità e i dati demo per gli scenari a grana fine sono [qui sotto](#identità-e-dati-demo-per-gli-scenari-a-grana-fine).
7. **Dove guardare il codice**: le risorse REST in `…/control/*Resource.java`; le regole fini dentro i metodi; i test in `src/test/java/…`.
8. **Ciclo di sviluppo**: branch dedicato → modifica → `mvn verify -P security` → tagga i nuovi test (`security` + esito + classe) o il gate fallisce.

### Identità e dati demo per gli scenari a grana fine

Per provare interattivamente gli scenari **ownership** (`/doc/note`), **multi-tenant** (`/doc/officedoc`)
e **appuntamenti** (`/doc/appointment`) genera il token via `GET /demo/office/{office}/{upn}/{roles}.txt`:

| upn       | ufficio | ruoli | token demo                                            |
|-----------|---------|-------|-------------------------------------------------------|
| EINSTEIN  | FISICA  | user  | `/demo/office/FISICA/EINSTEIN/user,guest.txt`         |
| BOHR      | FISICA  | admin | `/demo/office/FISICA/BOHR/admin,user,guest.txt`       |
| PLANCK    | FISICA  | guest | `/demo/office/FISICA/PLANCK/guest.txt`                |
| MENDELEEV | CHIMICA | admin | `/demo/office/CHIMICA/MENDELEEV/admin,user,guest.txt` |

> L'**ufficio** è un claim del JWT (`office`), non un dato di input: in produzione arriverebbe dall'IdP. In dev lo si ottiene dall'endpoint demo.

**Dati demo pre-caricati** (in `init.sql`, solo per esplorazione: i test non vi dipendono):

| Risorsa | UUID | Note |
|---------|------|------|
| Nota di Einstein | `a1a1a1a1-0000-0000-0000-000000000001` | visibile a Einstein o a un admin |
| Documento di Einstein (PUBLISHED, FISICA, soglia user) | `b1b1b1b1-0000-0000-0000-000000000001` | leggibile da FISICA con ruolo ≥ user |
| Documento di Bohr (PUBLISHED, FISICA, soglia admin) | `b1b1b1b1-0000-0000-0000-000000000002` | leggibile solo dagli admin di FISICA |
| Bozza di Mendeleev (DRAFT, CHIMICA) | `b1b1b1b1-0000-0000-0000-000000000003` | visibile solo all'owner finché non pubblicata |
| Appuntamento di Einstein con Bohr (FISICA, 2030) | `c1c1c1c1-0000-0000-0000-000000000001` | visibile a Einstein, Bohr o un admin di FISICA |

**Esempio** sul documento `b1b1b1b1-0000-0000-0000-000000000001` (`GET /doc/officedoc/{uuid}`): con il
token di **PLANCK** (FISICA, guest) → **403** (ruolo inferiore alla soglia `user`); con **BOHR**
(FISICA, admin) → **200**; con **MENDELEEV** (CHIMICA, admin) → **403**, perché un ufficio diverso non
accede *nemmeno se admin*.

---

## 2. Metodi esposti via OpenAPI (descrizione sintetica)

### `DocResource` — generazione documenti e gestione persone (`/doc`)

| Metodo & Path | operationId | Accesso | Descrizione |
|---------------|-------------|---------|-------------|
| `GET /doc/example.md` | MarkdownExample | admin, user, guest | Genera il documento in MarkDown |
| `GET /doc/example.html` | HTMLExample | admin, user | Genera il documento in HTML |
| `GET /doc/example.adoc` | AsciiDocExample | admin | Genera il documento in AsciiDoc |
| `GET /doc/example.pdf` | PDFExample | admin | Genera il documento in PDF |
| `GET /person/list` | listPerson | admin, user | Elenca le persone (lista **filtrata per ruolo**: chi non è admin non vede chi ha `minRole=admin`) |
| `GET /person/find/{uuid}` | findPerson | admin, user | Dettaglio persona per UUID, con **controllo object-level** sul `minRole`; inesistente/non autorizzato → 403 |
| `POST /person/add` | addPerson | admin | Crea una persona (uuid/data generati dal server) |
| `PUT /person/edit/{uuid}` | editPerson | admin, user | Modifica anagrafica; il campo privilegiato `minRole` è modificabile **solo da admin** (field-level) |
| `DELETE /person/delete/{uuid}` | deletePerson | admin | Cancella una persona |

### `DemoJwtGeneratorRest` — generazione JWT dimostrativi (`/demo`, solo dev/test)

| Metodo & Path | operationId | Accesso | Descrizione |
|---------------|-------------|---------|-------------|
| `GET /demo/{roles}.txt` | adminToken | pubblico (`@PermitAll`) | Token con i soli ruoli indicati (upn fisso `DEMOUSER`) |
| `GET /demo/office/{office}/{upn}/{roles}.txt` | officeToken | pubblico (`@PermitAll`) | Token con **ufficio** (claim `office`), **upn** e ruoli scelti — per gli scenari multi-tenant |

### `PersonalNoteResource` — note personali / ownership (`/doc/note`)

| Metodo & Path | operationId | Accesso | Descrizione |
|---------------|-------------|---------|-------------|
| `POST /doc/note` | createNote | autenticato | Crea una nota; l'**owner è l'utente autenticato** (dal token) |
| `GET /doc/note/list` | listNotes | autenticato | Le proprie note; un **admin** le vede tutte |
| `GET /doc/note/{uuid}` | readNote | owner o admin | Legge una nota |
| `PUT /doc/note/{uuid}` | editNote | **solo owner** | Modifica una nota (un admin può leggere ma non modificare) |
| `DELETE /doc/note/{uuid}` | deleteNote | **solo owner** | Cancella una nota |

### `OfficeDocumentResource` — documenti di ufficio / multi-tenant (`/doc/officedoc`)

| Metodo & Path | operationId | Accesso | Descrizione |
|---------------|-------------|---------|-------------|
| `POST /doc/officedoc` | createOfficeDoc | autenticato | Crea un documento; **owner/ufficio/ruolo dal token**, stato iniziale `DRAFT` |
| `GET /doc/officedoc/list` | listOfficeDocs | autenticato | I documenti visibili al chiamante (propri, condivisi, o published dello stesso ufficio con ruolo ≥) |
| `GET /doc/officedoc/{uuid}` | readOfficeDoc | regole multi-tenant | Legge: owner, oppure condiviso, oppure PUBLISHED + stesso ufficio + ruolo ≥ owner; altrimenti 403 |
| `PUT /doc/officedoc/{uuid}` | editOfficeDoc | owner o admin stesso ufficio (su PUBLISHED) | Modifica nome/contenuto |
| `DELETE /doc/officedoc/{uuid}` | deleteOfficeDoc | owner o admin stesso ufficio (su PUBLISHED) | Cancella il documento |
| `PUT /doc/officedoc/{uuid}/publish` | publishOfficeDoc | **solo owner** | Passa lo stato da DRAFT a PUBLISHED |
| `POST /doc/officedoc/{uuid}/share` | shareOfficeDoc | **solo owner** | Condivide il documento con un altro upn (gli concede la lettura) |

> Invarianti trasversali dello scenario di ufficio: **isolamento di tenant assoluto** (un altro ufficio non
> accede nemmeno se admin) e **anti-enumeration** (uuid inesistente → 403 identico al "non autorizzato").

### `AppointmentResource` — appuntamenti / visibilità multi-parte + regola temporale (`/doc/appointment`)

| Metodo & Path | operationId | Accesso | Descrizione |
|---------------|-------------|---------|-------------|
| `POST /doc/appointment` | createAppointment | autenticato | Prenota un appuntamento; **creatore dal token**, scienziato/ufficio/data dal body |
| `GET /doc/appointment/list` | listAppointments | autenticato | Gli appuntamenti visibili al chiamante |
| `GET /doc/appointment/{uuid}` | readAppointment | creatore, destinatario o admin di ufficio | Legge un appuntamento |
| `DELETE /doc/appointment/{uuid}` | deleteAppointment | **solo creatore, e solo > 24h prima** | Elimina (regola temporale) |
| `PUT /doc/appointment/{uuid}/move` | moveAppointment | **solo creatore** | Sposta l'appuntamento |

> Visibilità **multi-parte**: creatore O scienziato destinatario O admin dello stesso ufficio. L'eliminazione aggiunge una **regola temporale** (delete solo se mancano > 24h).

---

## 3. Catalogo dei test di sicurezza (dal più basilare al più avanzato)

I test sono organizzati per livello di difficoltà concettuale. Tra parentesi l'esito atteso.

### Livello 0 — Autenticazione (è *authn*, la base) — `DocResourceSicurezzaTest`
| Test | Descrizione |
|------|-------------|
| `testMarkdown401NoAuthorizationBearer` (401) | Accesso senza token → negato |
| `testUnauthorizedWithoutJwt` (401) | Richiesta senza header Authorization |
| `testUnauthorizedWithWrongJwt` (401) | Token malformato/non valido |
| `testExpiredJWT` (401) | Token scaduto |

### Livello 1 — RBAC a grana grossa (ruolo sull'endpoint) — `DocResourceSicurezzaTest`
| Test | Descrizione |
|------|-------------|
| `testHtmlOkNoAdminRole` (200) | `user` accede all'HTML (consentito) |
| `testPdfOkNoAdminRole` (200) | utente con tutti i ruoli accede al PDF |
| `testOkWithJwt` (200) | `admin` accede al PDF (via JWT reale) |
| `testOkJwtMarkDown` (200) | `guest` accede al MarkDown |
| `testOkJwtAsciiDoc` (200) | `admin` accede all'AsciiDoc |
| `testMarkdown403NoAdminRole` (403) | `user` sul PDF (admin-only) → negato |
| `testForbiddenWithJwt` (403) | `guest` sul PDF → negato |
| `testForbiddenJwtAsciiDoc` (403) | `guest` sull'AsciiDoc → negato |
| `testAddPersonAdminOk` (201) | `admin` crea una persona |
| `testAddDeletePersonAdminOk` (200) | `admin` crea e poi cancella una persona |
| `testDeletePersonAdminKoNonEsiste` (403) | `admin` cancella un id inesistente → 403 (uniforme) |
| `testDeletePersonUserKo` (403) | `user` tenta la cancellazione (admin-only) → escalation verticale negata |

### Livello 2 — Function-level & verb tampering — `PersonResourceFunctionLevelTest`
| Test | Descrizione |
|------|-------------|
| `testAddPersonNonAdminKo` (403, parametrico `user`/`guest`) | Qualsiasi non-admin che tenta la creazione → negato |
| `testVerbTamperingPutOnAddNotAllowed` (405) | `PUT` su un path che dichiara solo `POST` |
| `testVerbTamperingDeleteOnListNotAllowed` (405) | `DELETE` su un path che dichiara solo `GET` |

### Livello 3 — Data filtering per ruolo (escalation orizzontale) — `DocResourceSicurezzaTest`
| Test | Descrizione |
|------|-------------|
| `testOkMarkDownConVerificaContenutoAdmin` (200) | L'`admin` vede nel documento la persona riservata (Feynman) |
| `testOkMarkDownConVerificaContenutoUser` (200) | Lo `user` **non** vede la persona riservata (dati filtrati) |
| `testListPersonsResultKo` (200) | Lista per `user`: esclude la persona admin-only |
| `testListPersonsResultOk` (200) | Lista per `admin`: include la persona admin-only |

### Livello 4 — Object-level (BOLA/IDOR) & anti-enumeration — `DocResourceSicurezzaTest`
| Test | Descrizione |
|------|-------------|
| `testFindPersonOkAdmin` (200) | L'`admin` accede alla persona con `minRole=admin` |
| `testFindPersonOkUser` (200) | Lo `user` accede a una persona a lui consentita |
| `testFindPersonKoForbidden` (403) | Lo `user` su una persona admin-only → negato (BOLA) |
| `testFindPersonKoNotFound` (403) | Id inesistente → 403 (anti-enumeration: non rivela l'esistenza) |

### Livello 5 — Field-level & mass assignment — `PersonResourceFieldLevelTest`
| Test | Descrizione |
|------|-------------|
| `testAddPersonIgnoresServerControlledFields` (201) | `uuid`/`id`/`creationDate` inviati dal client vengono ignorati (li genera il server) |
| `testEditPersonUserCannotChangeMinRole` (403) | Lo `user` tenta di modificare il campo privilegiato `minRole` → negato |
| `testEditPersonUserCanEditAnagraphicFields` (200) | Lo `user` modifica i dati anagrafici; `minRole` resta invariato |
| `testEditPersonAdminCanChangeMinRole` (200) | L'`admin` può modificare `minRole` |

### Livello 6 — Ownership (dati personali) — `PersonalNoteResourceTest`
| Test | Descrizione |
|------|-------------|
| `testOwnerReadsOwnNote` (200) | L'owner legge la propria nota |
| `testAdminReadsAnyNote` (200) | Un admin legge la nota altrui |
| `testOtherUserCannotReadNote` (403) | Un altro utente non-admin non legge la nota altrui |
| `testOwnerCanEditNote` (200) | L'owner modifica la propria nota |
| `testNonOwnerAdminCannotEditNote` (403) | L'admin può leggere ma **non** modificare la nota altrui |
| `testReadNonExistentNote` (403) | Nota inesistente → 403 (anti-enumeration) |

### Livello 7 — Isolamento multi-tenant per ufficio, gerarchia di ruoli, draft/published e sharing — `OfficeDocumentResourceTest`
| Test | Descrizione |
|------|-------------|
| `testOwnerReadsOwnDraft` (200) | L'owner legge la propria bozza (DRAFT) |
| `testDraftNotVisibleToOfficeAdmin` (403) | La bozza non è visibile all'admin di ufficio finché non è pubblicata |
| `testPublishedVisibleToSameOfficeHigherRole` (200) | Dopo la pubblicazione, l'admin dello stesso ufficio (ruolo ≥) legge |
| `testSameOfficeLowerRoleForbidden` (403) | Stesso ufficio ma ruolo inferiore all'owner → non legge |
| `testCrossOfficeAdminForbidden` (403) | **Isolamento tenant**: admin di un altro ufficio → negato (anche se admin) |
| `testAntiEnumerationNonExistent` (403) | Uuid inesistente → 403 identico al "non autorizzato" |
| `testOwnerCanEdit` (200) | L'owner modifica il proprio documento |
| `testOfficeAdminCanEditPublished` (200) | L'admin dello stesso ufficio modifica un documento PUBLISHED |
| `testSameOfficeNonAdminCannotEdit` (403) | Stesso ufficio, può leggere ma non è admin → non modifica |
| `testCrossOfficeAdminCannotEdit` (403) | Admin di ufficio diverso → non modifica |
| `testMassAssignmentOwnerOfficeIgnored` (201) | `ownerUpn`/`ownerOffice`/`status` dal client vengono ignorati (impostati dal server) |
| `testSharingGrantsCrossOfficeRead` (200) | Un utente di ufficio diverso, se **condiviso**, può leggere |
| `testNotSharedCrossOfficeForbidden` (403) | Un utente di ufficio diverso **non** condiviso non legge |

### Livello 8 — Visibilità multi-parte, autorizzazione temporale e ownership del creatore — `AppointmentResourceTest`
| Test | Descrizione |
|------|-------------|
| `testCreatorCanView` (200) | Il creatore vede il proprio appuntamento |
| `testScientistCanView` (200) | Lo scienziato destinatario lo vede |
| `testOfficeAdminCanView` (200) | L'admin dello stesso ufficio lo vede |
| `testCrossOfficeAdminForbidden` (403) | L'admin di un altro ufficio non lo vede (isolamento di tenant) |
| `testUnrelatedSameOfficeForbidden` (403) | Un estraneo dello stesso ufficio non lo vede (visibilità relazionale) |
| `testAntiEnumerationNonExistent` (403) | Appuntamento inesistente → 403 |
| `testCreatorDeleteMoreThan24hOk` (200) | Il creatore elimina a più di 24h |
| `testCreatorDeleteWithin24hForbidden` (403) | Il creatore **non** elimina a meno di 24h (regola temporale) |
| `testNonCreatorCannotDelete` (403) | Un non-creatore (anche il destinatario) non elimina |
| `testCreatorCanMove` (200) | Il creatore sposta l'appuntamento |
| `testNonCreatorCannotMove` (403) | Un non-creatore non sposta |
| `testCreatorUpnIgnored` (201) | `creatorUpn` inviato dal client viene ignorato (mass-assignment) |

---

## Interfaccia grafica (console didattica)

Una **console statica** è servita da Quarkus su **<http://localhost:8080/ui/>** (avvia con `mvn quarkus:dev`).
Serve a *vedere* il comportamento autorizzativo; il backend resta l'unica autorità. Tre fasce:

- **Identità (alto)**: genera/cambia identità (upn, ufficio, ruoli) con **preset** per gli scienziati (Einstein/Bohr/Planck/Fermi in FISICA, Mendeleev/Lavoisier in CHIMICA); mostra il **token decodificato** (upn/office/roles/exp).
- **Azioni (sinistra)**: una sezione per dominio (Documenti, Persone, Note, Documenti di ufficio, Appuntamenti) con i pulsanti delle operazioni. I pulsanti restano **attivi anche per azioni che saranno negate**: vedrai il **403** dal server (la UI non è il confine di sicurezza). Dopo una *create*, l'uuid restituito viene copiato nel campo del dominio.
- **Esito (destra)**: per ogni chiamata, `METODO path → status` con **codice colore** (verde 200/201, giallo 401, rosso 403, arancio 405) e una **spiegazione**.

Prova rapida: preset **Bohr** (FISICA/admin) → *Documenti di ufficio* → Read `b1b1b1b1-0000-0000-0000-000000000001` → **200**; passa a **Mendeleev** (CHIMICA/admin) → stessa Read → **403** (isolamento di tenant).

> Solo dev: in `prod` gli endpoint `/demo*` sono disattivati (`@UnlessBuildProfile("prod")`), quindi la console non può generare token. File statici in `src/main/resources/META-INF/resources/ui/`.

## Appendice — test non di sicurezza (per completezza)
- `DocResourceTest` — smoke test positivi della generazione documenti (tag `business`/`success`).
- `DocHelperTest` — unit test del motore di rendering (Fugerit Venus Doc).
- `DemoJwtGeneratorRestTest` — test dell'endpoint demo di generazione JWT.
- `DocResourceIT` — riesecuzione in modalità *packaged*/nativa (failsafe, con `-Dnative`).

> Nota: questi tre non sono eseguiti dal `mvn verify` standard perché privi dei tag in `<groups>` di surefire (vedi `TODO.md` #1/#2).
