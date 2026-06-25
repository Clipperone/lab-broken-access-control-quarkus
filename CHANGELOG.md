# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Changed

- **Appuntamenti**: l'`office` non è più inviato dal client ma **derivato lato server** dallo scienziato (registro `Scientist`). Rimosso il campo `office` da `AppointmentRequestDTO`; nella console GUI lo scienziato diventa una tendina alimentata da `GET /scientist/list` e l'input "ufficio" è rimosso.
- Riclassificato il tag di gate **`temporal` → `business-logic`**: la finestra di cancellazione a 24h degli appuntamenti è una regola di business (rilevante per la sicurezza per via dell'enforcement lato server). Aggiornati `pom.xml` (`requiredTags`), i test e la documentazione (`README.md`, `SECURITY-UNIT-TEST.md`, `SECURITY-UNIT-TEST-QUICKSTART.md`, `JUNIT-TAG.md`, `CLAUDE.md`). Nota: il tag `business` (senza trattino) resta riservato ai test funzionali non di sicurezza (`DocResourceTest`).
- **Refactoring**: estratta `PersonResource` da `DocResource`. Il CRUD persone ora vive in `/person/*` (breaking change: `POST /doc/person/add` → `POST /person/add`, ecc.). `DocResource` mantiene solo `/doc/example.{md,html,adoc,pdf}`. Test rinominati di conseguenza: `DocResourceFieldLevelTest` → `PersonResourceFieldLevelTest`, `DocResourceFunctionLevelTest` → `PersonResourceFunctionLevelTest`, `DocResourceSicurezzaTest` splittato in `DocResourceSicurezzaTest` (`/doc/example.*`) + `PersonResourceSicurezzaTest` (`/person/*`).

### Added

- Scenario **Appuntamenti** (`/doc/appointment`, `Appointment`): visibilità multi-parte (creatore / scienziato destinatario / admin dello stesso ufficio), **eliminazione solo dal creatore e solo se mancano più di 24h** (finestra di cancellazione), spostamento solo dal creatore, anti-enumeration, creatore server-side. Test: `AppointmentResourceTest`. Sezione "Appuntamenti" anche nella console GUI.
- Classe di test **`business-logic`** (tag di gate) per le regole di business imposte lato server, e relativi scenari sugli appuntamenti: **niente doppia prenotazione** dello stesso scienziato nello stesso slot (409 Conflict, `POST`/`PUT .../move`) e **orizzonte massimo di prenotazione** oltre 1 anno (422 Unprocessable Entity, `POST`/`PUT .../move`). Aggiunto `AppointmentRepository.hasConflict` e un `@AfterEach` di pulizia in `AppointmentResourceTest`.
- Sfida **(Y)** non coperta dai test: bypass *move-then-delete* della finestra di cancellazione (lo spostamento non ha controllo temporale). Documentata in `README.md` e `SECURITY-UNIT-TEST.md` con soli suggerimenti teorici.
- **Registro scienziato→ufficio** (entity `Scientist` + `ScientistRepository` + `GET /scientist/list` via `ScientistResource`) come fonte di verità server-side. Scenario **(9i)**: l'ufficio dell'appuntamento è derivato dallo scienziato (anti mass-assignment/salto di tenant); l'`office` inviato dal client è ignorato e uno scienziato non nel registro è rifiutato con 422. Nuovi test in `AppointmentResourceTest` (`testOfficeDerivedFromScientistNotClient`, `testUnknownScientistRejected`, `testOfficeFollowsScientistNotCreator`); tabella `SCIENTIST` + seed in `init.sql`.


- Scenario **Ownership** (`/doc/note`, `PersonalNote`): dati visibili solo a owner o admin, modificabili solo dall'owner. Test: `PersonalNoteResourceTest`.
- Scenario **Multi-tenant per ufficio + gerarchia ruoli** (`/doc/officedoc`, `OfficeDocument`): visibilità per owner/ufficio/ruolo≥, isolamento di tenant assoluto (admin di altro ufficio escluso), stato draft/published, condivisione esplicita, owner/ufficio server-side (anti mass-assignment). Test: `OfficeDocumentResourceTest`.
- Claim JWT `office` (`DemoJwtGeneratorRest.generateOfficeToken`) e helper `security/RoleHierarchy` (gerarchia `guest<user<admin`).
- Tassonomia tag estesa con `ownership` e `tenant`, verificati dal gate `junit5-tag-check`.


- Guida formativa `SECURITY-UNIT-TEST.md`: anatomia di un security unit test di autorizzazione, ponte SAST/DAST → unit test, catalogo pattern/anti-pattern, learning path a difficoltà incrementale, matrice di copertura.
- Scenario **Function Level Access Control & verb tampering**: nuova classe `DocResourceFunctionLevelTest` (escalation verticale sulla creazione, verb tampering → 405).
- Scenario **Mass assignment & Field-Level Authorization**: nuovo endpoint `PUT /doc/person/edit/{uuid}` con `EditPersonRequestDTO`, nuova classe `DocResourceFieldLevelTest` (campi server-controlled non sovrascrivibili, `minRole` modificabile solo da 'admin').
- Tassonomia tag estesa con `object-level`, `function-level`, `field-level`, verificati dal gate `junit5-tag-check`.
- Deny-by-default: `quarkus.security.jaxrs.deny-unannotated-endpoints=true` (mitigazione strutturale della classe "Missing Function Level Access Control").

### Changed

- **README ristrutturato come hub snello**: nuova sezione "Due modi di usare il progetto" (laboratorio vs riferimento formativo), indice riallineato, sezioni riordinate (Il progetto → Quickstart → Scenario → Workflow → Vulnerabilità), Bonus UUID spostato dopo le vulnerabilità dimostrative, tabella ruoli deduplicata, riferimento a `branch-vulnerable` riformulato come roadmap. Identità demo, dati seed e walkthrough migrati in `GUIDA-OPERATIVA.md` (nuova sottosezione "Identità e dati demo", incluso il seed appuntamento).
- Endpoint demo JWT (`/demo/{roles}.txt`): `@PermitAll` esplicito e disattivazione automatica nel profilo `prod` (`@UnlessBuildProfile("prod")`).
- Rinominato `CONTRIBUITING.md` → `CONTRIBUTING.md`; rimosso il file `dependabot.yml` duplicato (mantenuto `.github/dependabot.yml`).
- `JUNIT-TAG.md` e `JUNIT-TEST.md` aggiornati e corretti.

## [2.0.0] - 2026-02-26

### Added

- Aggiunto campo UUID all'entità persona da usare al posto dell' ID numerico sequenziale (Breaking change)

### Fixed 

- IDOR, Insecure Direct Object Reference sull'ID delle persone

## [1.0.1] - 2026-02-17

### Fixed

- VULNERABILITY (x)
- VULNERABILITY (5)
- VULNERABILITY (4)
- VULNERABILITY (3)
- VULNERABILITY (2)
- VULNERABILITY (1)

## [1.0.0] - 2026-02-16

### Added

- versione vulnerabile del progetto, basato su Quarkus 3.31.3, con vulnerabilità broken access control presenti.
