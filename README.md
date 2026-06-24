# Broken Access Control Lab

Un laboratorio educativo completo per testare e comprendere le vulnerabilità [Broken Access Control](https://owasp.org/Top10/2025/A01_2025-Broken_Access_Control/) nelle applicazioni Java.

> ⚠️ **ATTENZIONE**: Questo progetto contiene **intenzionalmente vulnerabilità di sicurezza** a scopo educativo. **NON utilizzare in produzione** e **NON esporre pubblicamente** senza aver rimosso tutte le vulnerabilità dimostrative.

> 🟢 Questo branch contiene la **versione sanata** (vulnerabilità corrette, test verdi). Per gli esercizi TDD red→green usa il branch `branch-vulnerable`.

Le vulnerabilità di tipo [Broken Access Control](https://owasp.org/Top10/2025/A01_2025-Broken_Access_Control/) sono attualmente le più diffuse secondo il progetto [OWASP](https://owasp.org/). Sono al primo posto sia nella [OWASP Top 10](https://owasp.org/Top10/) del [2021](https://owasp.org/Top10/2021/) che [2025](https://owasp.org/Top10/2025/).

[![Keep a Changelog v1.1.0 badge](https://img.shields.io/badge/changelog-Keep%20a%20Changelog%20v1.1.0-%23E05735)](CHANGELOG.md)
[![License: MIT](https://img.shields.io/badge/License-MIT-teal.svg)](https://opensource.org/licenses/MIT)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=lab-sca_lab-broken-access-control-quarkus&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=lab-sca_lab-broken-access-control-quarkus)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=lab-sca_lab-broken-access-control-quarkus&metric=coverage)](https://sonarcloud.io/summary/new_code?id=lab-sca_lab-broken-access-control-quarkus)

## Indice

- [Il progetto](#il-progetto)
- [Due modi di usare il progetto](#due-modi-di-usare-il-progetto)
- [Quickstart](#quickstart)
- [Lo scenario](#lo-scenario)
- [Workflow del laboratorio](#workflow-del-laboratorio)
- [Vulnerabilità dimostrative](#vulnerabilità-dimostrative)
  - [🎁 Bonus - Conversione degli ID in UUID](#-bonus--conversione-degli-id-in-uuid)
- [Architettura della sicurezza](#architettura-della-sicurezza)
- [📚 Riferimenti rapidi](#-riferimenti-rapidi)
- [❓ FAQ / Problemi comuni](#-faq--problemi-comuni)
- [Licenza](#licenza)

## Il progetto

Questo progetto mostra come identificare vulnerabilità di tipo Broken Access Control e come implementare una strategia di testing basata su tag JUnit per garantire la copertura dei requisiti di sicurezza in un'applicazione Quarkus con autenticazione JWT e RBAC (Role-Based Access Control).

Il laboratorio copre competenze pratiche su:

- 🔐 **Autenticazione JWT**: implementazione e configurazione in Quarkus
- 🛡️ **RBAC**: design e implementazione di Role-Based Access Control
- 🐛 **Vulnerability Detection**: identificazione di BOLA, IDOR e privilege escalation
- ✅ **Security Testing**: strategia di test con JUnit tags e coverage
- 📊 **Security Metrics**: misurazione della copertura dei requisiti di sicurezza
- 🔒 **Defense in Depth**: approccio a più livelli per la sicurezza applicativa

## Due modi di usare il progetto

### 🧪 Percorso 1 - Il laboratorio

Parti da `branch-vulnerable`, fai fallire i test e correggi le vulnerabilità **(1)–(9f)** distribuite su `DocResource`, `PersonResource`, `PersonalNoteResource`, `OfficeDocumentResource` e `AppointmentResource`.

### 🏢 Percorso 2 - Riferimento per la Scrittura Unit Test di Sicurezza

Il progetto è anche un **riferimento per scrivere unit test di sicurezza sui controlli di autorizzazione** (OWASP A01), a complemento degli strumenti SAST/DAST. Sono stati rappresentati vari scenari di riferimento:

- **Function Level Access Control** (escalation verticale di ruolo)
- **Mass Assignment** (campi server-managed imposti dal client)
- **Field-Level Authorization** (campi privilegiati modificabili solo dal ruolo autorizzato)
- **Ownership-based access** (dati personali: visibili solo a owner o admin)
- **Isolamento multi-tenant per ufficio** (dati visibili solo se claim `office` corrisponde)
- **Gerarchia di ruoli** (accesso al documento solo se il ruolo di chi cerca di accedere al dato è ≥ del ruolo dell'owner del dato)
- **Visibilità multi-utente** (gestione appuntamenti, i dati sono visibili solo a: creatore, destinatario o admin di ufficio)
- **Autorizzazione temporale** (cancellazione di un appuntamento consentita solo al creatore e solo entro 24h prima)
- **Verb Tampering** (verbo HTTP non dichiarato rifiutato con 405)

Documenti dedicati:

- 🚀 **[SECURITY-UNIT-TEST-QUICKSTART.md](SECURITY-UNIT-TEST-QUICKSTART.md)** - breve guida introduttiva: prerequisiti, esempio completo con codice e spiegazione, cosa fare dopo.
- 📘 **[SECURITY-UNIT-TEST.md](SECURITY-UNIT-TEST.md)** - *come progettare* i test: struttura di un test di autorizzazione, casi d'uso → unit test, pattern/anti-pattern, matrice di copertura.
- 🧭 **[GUIDA-OPERATIVA.md](GUIDA-OPERATIVA.md)** - onboarding step-by-step, descrizione di tutti i metodi OpenAPI, catalogo dei test dal più basilare al più avanzato, **identità e dati demo** per provare gli scenari a grana fine.
- 🖥️ **Console didattica** su <http://localhost:8080/ui/> (in dev): prova gli scenari dal browser cambiando identità e osservando esito + spiegazione.

### Stack tecnologico

- [Quarkus - Stack cloud-native ottimizzato per OpenJDK HotSpot e GraalVM](https://quarkus.io/)
- [junit5-tag-check-maven-plugin - Plugin Maven che permette di verificare che dei test con tag specifici siano stati eseguiti](https://github.com/fugerit-org/junit5-tag-check-maven-plugin)
- [Fugerit Venus Doc - Framework per la generazione di documenti in vari formati (usato solo per le funzionalità dimostrative)](https://github.com/fugerit-org/fj-doc)

## Quickstart

### Requisiti

* Maven 3.9.x
* Java 21+

### Verifica dell'applicazione

Per eseguire i test standard:
```shell
mvn verify
```

Per attivare anche la verifica dei tag di sicurezza con il plugin `junit5-tag-check-maven-plugin`:
```shell
mvn verify -P security
```

### Avvio dell'applicazione
```shell
mvn quarkus:dev
```

### Utilizzo dell'applicazione

1. Apri la [Swagger UI](http://localhost:8080/q/swagger-ui/)
2. Genera un JWT token
3. Autorizza le richieste con il token
4. Testa gli endpoint disponibili

### Generazione e utilizzo dei JWT token

Usa l'endpoint `/demo/{roles}.txt` per generare un JWT con i ruoli desiderati; per gli scenari
multi-tenant usa `/demo/office/{office}/{upn}/{roles}.txt`, che aggiunge il claim `office` e l'upn
(vedi le [identità demo](GUIDA-OPERATIVA.md#identità-e-dati-demo-per-gli-scenari-a-grana-fine)).

> ⏱️ **Durata token**: 1 ora (3600 secondi)  
> 🔑 **Algoritmo**: RS256 (RSA Signature con SHA-256)  
> 📝 **Issuer**: `https://unittestdemoapp.fugerit.org`

**Esempi di generazione da console:**
```bash
# Token con singolo ruolo
curl http://localhost:8080/demo/guest.txt
```
```bash
# Token con ruoli multipli (separati da virgola)
curl http://localhost:8080/demo/admin,user.txt
```

> ⚠️ **Nota importante**: L'endpoint `/demo/{roles}.txt` è fornito **solo per scopi dimostrativi**.
> In produzione, l'autenticazione deve avvenire tramite un Identity Provider (IDP) esterno.

**Esempi di generazione da Quarkus Swagger UI:**

![generazione del jwt dimostrativo](./src/docs/image/04-01-jwt-demo-generation.png)

Payload del JWT completo generato come esempio:
```json
{
  "iss": "https://unittestdemoapp.fugerit.org",
  "upn": "DEMOUSER",
  "groups": [
    "guest",
    "user"
  ],
  "sub": "DEMOUSER",
  "iat": 1771234632,
  "exp": 1771238232,
  "jti": "ab2addbf-f776-4a7a-8b3d-4c0701f316d1"
}
```

Puoi usare strumenti online come [jwt.io](https://www.jwt.io/) per verificare il contenuto del tuo JWT.

#### Autorizzazione nella Swagger UI

1. Clicca sul pulsante **"Authorize"** nella Swagger UI
2. Inserisci il JWT ottenuto in precedenza nel formato: `Bearer <token>`
3. Clicca su "Authorize"

![autorizzazione con il jwt dimostrativo](./src/docs/image/04-02-jwt-demo-authorize.png)

### Test: Accesso negato (403 Forbidden)

Se tenti di accedere a un endpoint senza i ruoli necessari, riceverai un errore 403.

**Esempio**: Tentativo di accesso a `/doc/example.adoc` senza ruolo `admin`

![ruolo non autorizzato per il formato](./src/docs/image/05-01-document-403.png)

### Test: Accesso consentito (200 OK)

Con i ruoli appropriati, puoi accedere agli endpoint autorizzati.

**Esempio**: Accesso a `/doc/example.md` con ruoli `guest` o `user`

![documento generato](./src/docs/image/05-02-document-200.png)

Vedi la [mappatura di ruoli e path](#mappatura-ruoli--permessi--metodo-http) per maggiori dettagli.

## Lo scenario

Nel nostro scenario, abbiamo una base dati popolata e alcuni path disponibili.

### Base dati

Esiste una base dati di persone (sono entità di dominio, non utenti). La tabella PEOPLE è pre-popolata con 3 soggetti, che hanno 4 proprietà principali:

- Nome, Cognome, Titolo descrivono la persona
- Ruolo minimo: rappresenta il ruolo minimo richiesto per poter accedere a quella persona

| Nome       | Cognome | Titolo      | Ruolo minimo |
|------------|---------|-------------|--------------|
| Richard    | Feynman | Fisico      | admin        |
| Margherita | Hack    | Astrofisica | -            |
| Alan       | Turing  | Matematico  | -            |

> **NOTA**: Nel nostro DB pre-popolato tutti possono vedere i dati di Margherita Hack e Alan Turing, ma per vedere i dati di Richard Feynman (che sta lavorando al progetto Manhattan), serve il ruolo 'admin'.

### Mappatura ruoli / permessi / metodo http

L'applicazione gestisce 3 ruoli ed espone dei path che generare documenti in diversi formati o modificare i dati del sistema. Non tutti i ruoli sono autorizzati a richiamare ogni path. Ecco la mappa dei permessi:

| Path                          | Output      | Ruoli autorizzati  | Metodo http |
|-------------------------------|-------------|--------------------|-------------|
| `/doc/example.md` (*)         | 📝 MarkDown | admin, user, guest | GET         |
| `/doc/example.adoc`           | 📄 AsciiDoc | admin              | GET         |
| `/doc/example.html` (*)       | 🌐 HTML     | admin, user        | GET         |
| `/doc/example.pdf`            | 📑 PDF      | admin              | GET         |
| `/person/list` (*)        | 📋 JSON     | admin, user        | GET         |
| `/person/find/{uuid}` (*) | 📋 JSON     | admin, user        | GET         |
| `/person/add`             | 📋 JSON     | admin              | POST        |
| `/person/edit/{uuid}`     | 📋 JSON     | admin, user (**)   | PUT         |
| `/person/delete/{uuid}`   | 📋 JSON     | admin              | DELETE      |

> (*) Eccetto gli utenti con ruolo 'admin', su questi path potrebbe esserci una limitazione ai dati mostrati in base al ruolo minimo richiesto.

> (**) Il ruolo 'user' può modificare solo i campi anagrafici (nome, cognome, titolo); esiste un ulteriore campo privilegiato `minRole` modificabile solo da 'admin' (autorizzazione a livello di campo).

**Esempio di ruoli e permessi:**

| Ruolo   | Permessi                           | Esempio di utilizzo                         |
|---------|------------------------------------|---------------------------------------------|
| `admin` | Accesso completo a tutti i formati | Vedere Richard Feynman, gestire persone     |
| `user`  | Accesso a MarkDown e HTML          | Vedere Hack e Turing, documenti base        |
| `guest` | Accesso solo a MarkDown            | Visualizzazione read-only limitata          |

Sono implementati **ulteriori esempi di autorizzazione a grana fine** (note personali, documenti di ufficio, appuntamenti) descritti in [GUIDA-OPERATIVA.md](GUIDA-OPERATIVA.md) (endpoint, regole, identità e dati demo).

## Workflow del laboratorio

### Passo 1: Setup iniziale
```bash
git clone https://github.com/fugerit79/lab-broken-access-control-quarkus.git
cd lab-broken-access-control-quarkus
mvn quarkus:dev
```

### Passo 2: Esplora le vulnerabilità

- Apri le risorse REST (es. `DocResource.java`, `PersonResource.java`, `PersonalNoteResource.java`, `OfficeDocumentResource.java`, `AppointmentResource.java`)
- Cerca i commenti `// VULNERABILITY: (n)`
- Analizza il codice vulnerabile
- Identifica il tipo di vulnerabilità (IDOR, BOLA, etc.)

### Passo 3: Esegui i test
```bash
mvn verify -P security
```

Utilizzando il branch `branch-vulnerable` i test falliranno dove ci sono vulnerabilità. Osserva gli errori per capire cosa non funziona.

### Passo 4: Correggi le vulnerabilità

- Implementa le correzioni seguendo le best practices OWASP
- Verifica con i test che le modifiche funzionino
- Confronta con le soluzioni (`// SOLUTION: (n)`)

### Passo 5: Verifica la copertura
```bash
mvn verify -P security
```

Tutti i test devono passare ✅

### Passo 6: Trova la vulnerabilità BONUS

Cerca la vulnerabilità (X) che non è coperta dai test. Suggerimenti:
- Esamina tutti gli endpoint
- Cerca metodi HTTP non documentati
- Controlla le annotation mancanti

## Vulnerabilità dimostrative

Questo laboratorio include **15 vulnerabilità** di tipo Broken Access Control, distribuite su vari scenari:

| #   | Scenario / Vulnerabilità              | Classificazione   | Endpoint                                              |
|-----|---------------------------------------|-------------------|-------------------------------------------------------|
| (1) | Accesso a elenco non autorizzato                 | IDOR              | `GET /person/find/{uuid}`                         |
| (2) | Privilege Escalation (Data filtering)| BOLA              | `GET /doc/example.md`, `/doc/example.html`, `/person/list` |
| (3) | Privilege Escalation (Delete action) | BOLA              | `DELETE /person/delete/{uuid}`                    |
| (4) | Broken Object Level Authorization    | BOLA              | `GET /person/find/{uuid}`                         |
| (5) | Missing Authentication               | Access Control    | `GET /doc/example.md`                                 |
| (6) | Field-Level Authorization + Mass Assignment | Field-level | `PUT /person/edit/{uuid}`                         |
| (7a) | Ownership read: accesso non ristretto | Ownership       | `GET /doc/note/{uuid}`                                |
| (7b) | Ownership write: solo owner           | Ownership         | `PUT /doc/note/{uuid}`                                |
| (7c) | Accesso a note altrui               | IDOR              | `GET /doc/note/{uuid}`                   |
| (8a) | State visibility: Draft visibility    | Access Control    | `GET /doc/officedoc/list`, `GET /doc/officedoc/{uuid}` |
| (8b) | Tenant isolation: Cross-office access | Tenant            | `GET /doc/officedoc/{uuid}` (ufficio diverso)         |
| (8c) | Role hierarchy: Missing role check    | BOLA              | `GET /doc/officedoc/{uuid}` (ruolo < owner minRole)   |
| (8d) | Privilege escalation: Non-owner edit  | BOLA              | `PUT /doc/officedoc/{uuid}` (non owner/non admin)     |
| (8e) | Mass Assignment: Server-managed fields | Field-level     | `POST /doc/officedoc` (client sets owner/office/role) |
| (8f) | Accesso a documenti altrui               | IDOR              | `GET /doc/officedoc/{uuid}`            |
| (9a) | Tenant isolation: Cross-office access | Tenant            | `GET /doc/appointment/{uuid}` (ufficio diverso)       |
| (9b) | Over-broad visibility (same office)   | BOLA              | `GET /doc/appointment/{uuid}` (non correlato)         |
| (9c) | Temporal authorization: Delete window | Temporal          | `DELETE /doc/appointment/{uuid}` (< 24h)              |
| (9d) | Ownership: Non-owner delete/move      | Ownership         | `DELETE /doc/appointment/{uuid}`, `PUT .../move`      |
| (9e) | Mass Assignment: creatorUpn server-managed | Field-level   | `POST /doc/appointment` (client sets creatorUpn)      |
| (9f) | Accesso a appuntamenti altrui               | IDOR              | `GET /doc/appointment/{uuid}`            |
| (X) | **Hidden, no test**     | Function-level    | `PUT /person/add`                                 |

> 💡 **Sfida**: La vulnerabilità (X) non è coperta dai test. Riesci a trovarla?

### Descrizione delle vulnerabilità

#### (1) Insecure Direct Object Reference (IDOR)

Un utente autorizzato accede ai dati di altre persone puntando direttamente all'UUID, senza controllo di autorizzazione. L'API non verifica che l'utente possieda il ruolo minimo richiesto dalla risorsa.

**Endpoint**: `GET /person/find/{uuid}`

**Problema**: L'API permette l'accesso diretto a dati di altre persone senza verificare il ruolo minimo. Chiunque autenticato legge i dati di chiunque altro. Come effetto secondario, risposte diverse (200 vs 404) rivelano anche l'esistenza di UUID altrui.
```
GET /person/uuid-altrui-con-minRole-admin (da utente user) → 200 OK (accesso NON autorizzato!)
GET /person/999-inesistente → 404 Not Found (rivelo anche che 999 non esiste)
```

**Soluzione**: (1) **Core**: Verificare che l'utente possieda il `minRole` della persona richiesta, respingere con 403 se non autorizzato. (2) **Conseguenza**: Risposta uniforme (sempre 403) per UUID inesistente o non autorizzato, eliminando l'enumerazione come effetto collaterale.

#### (2) Privilege Escalation - Visualizzazione dati

L'utente riesce a vedere dati che dovrebbero essere disponibili solo per il profilo 'admin'.

**Endpoint**: `GET /doc/example.md`, `GET /doc/example.html`, `GET /person/list`

**Problema**: Utenti con ruolo 'user' vedono Richard Feynman (minRole=admin)

**Soluzione**: Filtrare i dati in base ai ruoli dell'utente autenticato

#### (3) Privilege Escalation - Cancellazione

L'utente riesce a cancellare una persona anche se non ha il ruolo 'admin'.

**Endpoint**: `DELETE /person/delete/{uuid}`

**Problema**: `@RolesAllowed` include erroneamente "user"

**Soluzione**: Rimuovere "user" da `@RolesAllowed`, lasciando solo "admin"

#### (4) Broken Object Level Authorization

L'utente riesce a vedere dati che non dovrebbero essere disponibili per il suo profilo.

**Endpoint**: `GET /person/find/{uuid}`

**Problema**: Verifica del ruolo minimo mancante

**Soluzione**: Controllare `person.getMinRole()` vs `securityIdentity.getRoles()`

#### (5) Missing Authentication

L'utente riesce ad accedere al documento anche se non è autenticato.

**Endpoint**: `GET /doc/example.md`

**Problema**: `@RolesAllowed` annotation mancante

**Soluzione**: Aggiungere `@RolesAllowed({ "admin", "user", "guest" })`

#### (6) Field-Level Authorization + Mass Assignment

Un utente non-admin riesce a modificare il campo privilegiato `minRole` di una persona.

**Endpoint**: `PUT /person/edit/{uuid}`

**Problema**: Il campo `minRole` è accettato dal DTO e applicato senza controllo di ruolo; il client può inviare qualsiasi valore

**Soluzione**: 
- Verificare il ruolo prima di applicare `minRole`: solo 'admin' può modificarlo
- Alternativa (design): esporre un DTO request diverso per ruoli non-admin, senza il campo `minRole` bindabile

#### (7a) Ownership Read: Accesso non autorizzato

Un utente non-owner riesce a leggere una nota che non gli appartiene (senza essere admin).

**Endpoint**: `GET /doc/note/{uuid}`

**Problema**: Manca la verifica di ownership della nota o se ruolo admin

**Soluzione**: Controllare che l'utente sia owner o admin prima di restituire la nota

#### (7b) Ownership Write: Modifica non autorizzata

Un admin riesce a modificare una nota altrui (un ruolo admin garantisce lettura ma la scrittura e riservata sempre all'owner).

**Endpoint**: `PUT /doc/note/{uuid}`

**Problema**: Manca la verifica di ownership della nota (manca il vincolo "solo owner")

**Soluzione**: Controllare che l'utente sia il solo owner (admin può leggere, ma non modificare)

#### (7c) IDOR: Accesso a note altrui

Un utente accede alle note di altri utenti puntando direttamente all'UUID, senza controllo di ownership. L'API non verifica che l'utente sia il proprietario della nota.

**Endpoint**: `GET /doc/note/{uuid}` (nota di altro utente)

**Problema**: L'API permette l'accesso diretto a note di altri utenti senza verificare l'ownership. Chiunque autenticato legge e modifica le note di chiunque altro. Come effetto secondario, risposte diverse (200 vs 404) rivelano anche l'esistenza di note altrui.

**Soluzione**: (1) **Core**: Verificare che l'utente sia il proprietario della nota, respingere con 403. (2) **Conseguenza**: Risposta uniforme (sempre 403) per nota inesistente o non autorizzata.

#### (8a) Visibilità della bozza: visualizzazione bozza non autorizzata

Un utente dell'ufficio legge una bozza (DRAFT) di un altro, anche se non ne è owner.

**Endpoint**: `GET /doc/officedoc/{uuid}` (DRAFT), `GET /doc/officedoc/list`

**Problema**: Manca il controllo che DRAFT sia visibile solo all'owner

**Soluzione**: Aggiungere vincolo `status == DRAFT && owner == self` oppure `status == PUBLISHED`

#### (8b) Tenant Isolation: Cross-office Access

Un admin di un ufficio diverso accede ai documenti di un altro ufficio.

**Endpoint**: `GET /doc/officedoc/{uuid}` (ufficio CHIMICA), chiamante da FISICA/admin

**Problema**: Manca il controllo dell'isolamento per ufficio anche se  ruolo admin

**Soluzione**: Verificare `Objects.equals(currentOffice(), document.getOwnerOffice())`; un admin di FISICA **non** vede documenti di CHIMICA

#### (8c) Role Hierarchy: Mancato controllo di gerarchia

Un utente con ruolo inferiore al `minRole` dell'owner accede al documento PUBLISHED.

**Endpoint**: `GET /doc/officedoc/{uuid}` (PUBLISHED con minRole=admin), chiamante user

**Problema**: Manca il controllo di gerarchia (ruolo ≥ owner minRole)

**Soluzione**: Verificare `RoleHierarchy.isAtLeast(securityIdentity.getRoles(), document.getOwnerRole())`

#### (8d) Privilege Escalation: Non-owner Edit

Un utente non-owner (né admin dello stesso ufficio) riesce a modificare il documento.

**Endpoint**: `PUT /doc/officedoc/{uuid}`

**Problema**: Manca il controllo che solo owner o admin dello stesso ufficio modifichino

**Soluzione**: Verificare ownership o (admin && stessoUfficio)

#### (8e) Mass Assignment: Server-managed Fields

Il client riesce a impostare `ownerUpn`, `ownerOffice`, `ownerRole`, `status` nel body della POST.

**Endpoint**: `POST /doc/officedoc`

**Problema**: I campi server-managed sono esposti nel DTO request e bindati dal framework

**Soluzione**: Impostare owner/ufficio/ruolo/stato lato server dalle credenziali del token, ignorare il body

#### (8f) IDOR: Accesso a documenti altrui

Un utente accede ai documenti di altri uffici o utenti puntando direttamente all'UUID, senza controllo di isolamento multi-tenant. L'API non verifica che l'utente appartenga all'ufficio del documento.

**Endpoint**: `GET /doc/officedoc/{uuid}` (documento di altro ufficio/utente)

**Problema**: L'API permette l'accesso diretto a documenti di altri uffici/utenti senza verificare l'isolamento multi-tenant. Un admin di un ufficio legge documenti di altri uffici. Come effetto secondario, risposte diverse (200 vs 404) rivelano anche l'esistenza di documenti altrui.

**Soluzione**: (1) **Core**: Verificare che l'utente appartenga all'ufficio del documento o sia admin di quell'ufficio, respingere con 403. (2) **Conseguenza**: Risposta uniforme (sempre 403) per documento inesistente o non autorizzato.

#### (9a) Tenant Isolation: Cross-office Visibility

Un admin di un ufficio diverso vede l'appuntamento di un altro ufficio.

**Endpoint**: `GET /doc/appointment/{uuid}` (ufficio CHIMICA), chiamante da FISICA/admin

**Problema**: Manca il vincolo di isolamento per ufficio

**Soluzione**: Verificare `Objects.equals(creatorOffice, currentOffice())` o `Objects.equals(scientistOffice, currentOffice())`

#### (9b) Missing Authorization: relazione (creatore/destinatario) non verificata

Un utente dello stesso ufficio, non creatore e non destinatario dell'appuntamento, vede l'appuntamento.

**Endpoint**: `GET /doc/appointment/{uuid}` (creato da A, destinato a B), viewer è C dello stesso ufficio

**Problema**: Visibilità concessa a "chiunque dello stesso ufficio" invece di solo relazioni (creatore, destinatario, admin)

**Soluzione**: Restringere a `creatorUpn == self OR scientistUpn == self OR (admin && stessoUfficio)`

#### (9c) Temporal Authorization: Finestra di eliminazione dell'appuntamento

Il creatore riesce a cancellare un appuntamento entro le 24h prima (quando dovrebbe essere vietato).

**Endpoint**: `DELETE /doc/appointment/{uuid}` (cancellazione entro 24h)

**Problema**: Manca il controllo che la cancellazione sia consentita solo se mancano > 24h

**Soluzione**: Verificare `ChronoUnit.HOURS.between(now, appointmentTime) > 24` prima di consentire DELETE

#### (9d) Ownership: Non-owner Delete/Move

Un non-creatore (anche il destinatario) riesce a cancellare o spostare l'appuntamento.

**Endpoint**: `DELETE /doc/appointment/{uuid}`, `PUT /doc/appointment/{uuid}/move`

**Problema**: Manca il controllo che solo il creatore possa eliminare/spostare

**Soluzione**: Verificare `creatorUpn == currentUser()` prima di consentire DELETE e MOVE

#### (9e) Mass Assignment: creatorUpn Server-managed

Il client riesce a impostare `creatorUpn` nel body della POST.

**Endpoint**: `POST /doc/appointment`

**Problema**: `creatorUpn` è server-managed ma presente nel DTO request

**Soluzione**: Impostare `creatorUpn` lato server dal token, ignorare il body

#### (9f) IDOR: Accesso ad appuntamenti non autorizzati

Un utente accede agli appuntamenti di altri utenti puntando direttamente all'UUID, senza controllo di visibilità multi-parte. L'API non verifica che l'utente sia creatore, destinatario o admin dell'ufficio.

**Endpoint**: `GET /doc/appointment/{uuid}` (appuntamento non autorizzato)

**Problema**: L'API permette l'accesso diretto ad appuntamenti senza verificare la visibilità multi-parte. Chiunque autenticato legge appuntamenti di cui non è creatore, destinatario né admin dell'ufficio. Come effetto secondario, risposte diverse (200 vs 404) rivelano anche l'esistenza di appuntamenti altrui.

**Soluzione**: (1) **Core**: Verificare che l'utente sia creatore, destinatario scientifico o admin dell'ufficio, respingere con 403. (2) **Conseguenza**: Risposta uniforme (sempre 403) per appuntamento inesistente o non autorizzato.

#### (X) Verb Tampering (Hidden, non coperto da test)

Una PUT senza controllo di autorizzazione è rimasta abilitata per errore.

**Endpoint**: `PUT /person/add`

**Problema**: Il metodo `addPersonPut()` è utilizzabile senza autenticazione (metodo HTTP non dichiarato)

**Soluzione**: Rimuovere totalmente il metodo `addPersonPut()` o applicare `@RolesAllowed`

```java
@PUT
@Path("/person/add")
@Transactional
public Response addPersonPut(AddPersonRequestDTO request) {
    return this.addPerson(request);
}
```

---

### Struttura del laboratorio

Le **15 vulnerabilità** sono distribuite su 4 risorse REST, ognuna con test di sicurezza associati:

| Risorsa REST | Vulnerabilità | Test |
|---|---|---|
| [DocResource](src/main/java/org/fugerit/java/demo/lab/broken/access/control/DocResource.java) | (2), (5) | [DocResourceSicurezzaTest](src/test/java/org/fugerit/java/demo/lab/broken/access/control/DocResourceSicurezzaTest.java) |
| [PersonResource](src/main/java/org/fugerit/java/demo/lab/broken/access/control/PersonResource.java) | (1), (3), (4), (6), (X) | [PersonResourceSicurezzaTest](src/test/java/org/fugerit/java/demo/lab/broken/access/control/PersonResourceSicurezzaTest.java), [PersonResourceFunctionLevelTest](src/test/java/org/fugerit/java/demo/lab/broken/access/control/PersonResourceFunctionLevelTest.java), [PersonResourceFieldLevelTest](src/test/java/org/fugerit/java/demo/lab/broken/access/control/PersonResourceFieldLevelTest.java) |
| [PersonalNoteResource](src/main/java/org/fugerit/java/demo/lab/broken/access/control/PersonalNoteResource.java) | (7a)–(7c) | [PersonalNoteResourceTest](src/test/java/org/fugerit/java/demo/lab/broken/access/control/PersonalNoteResourceTest.java) |
| [OfficeDocumentResource](src/main/java/org/fugerit/java/demo/lab/broken/access/control/OfficeDocumentResource.java) | (8a)–(8f) | [OfficeDocumentResourceTest](src/test/java/org/fugerit/java/demo/lab/broken/access/control/OfficeDocumentResourceTest.java) |
| [AppointmentResource](src/main/java/org/fugerit/java/demo/lab/broken/access/control/AppointmentResource.java) | (9a)–(9f) | [AppointmentResourceTest](src/test/java/org/fugerit/java/demo/lab/broken/access/control/AppointmentResourceTest.java) |

### Approccio Test-Driven Development

Questo progetto segue il *Test-driven development*: i test sono scritti prima del codice e definiscono il comportamento atteso.

I casi di test dove sono presenti vulnerabilità falliranno, per quelli sarà presente il commento:
```java
// VULNERABILITY: (n) risolvi questa vulnerabilità in modo che il caso di test funzioni.
```

Una volta risolte le vulnerabilità, le soluzioni si trovano cercando il commento:
```java
// SOLUTION: (n) 
```

Dove (n) è l'id della vulnerabilità introdotta, ad esempio (1) o (9c).

### Esecuzione e verifica

**Prima della risoluzione** (su `branch-vulnerable`), eseguendo `mvn verify -P security` vedrai **26 test rossi** (uno per ogni vulnerabilità, con alcuni scenari che coprono più weak):

- DocResource: 2 test falliti
- PersonResource: 5 test falliti
- PersonalNoteResource: 6 test falliti  
- OfficeDocumentResource: 8 test falliti
- AppointmentResource: 5 test falliti

![unit test falliti](./src/docs/image/junit-tofix.png)

**Dopo aver risolto tutte le vulnerabilità** (su `main` o applicando le soluzioni), eseguendo `mvn verify -P security` tutti i test passeranno al verde ✅ (63 test, inclusi i 26 negativi di sicurezza).

Buon lavoro!

### 🎁 Bonus - Conversione degli ID in UUID

Come visto nelle [vulnerabilità dimostrative](#vulnerabilità-dimostrative) (1) e (4), l'uso di **ID sequenziali** espone l'applicazione a vulnerabilità di tipo **IDOR** (Insecure Direct Object Reference), rendendo banale per un attaccante enumerare le risorse.

Una buona pratica è sostituire gli ID sequenziali con **UUID** casuali come identificatori pubblici delle risorse.

#### Modifiche necessarie

**1. `PersonRepository.java`** - aggiungere il metodo di ricerca per UUID:
```java
/**
 * Cerca una persona tramite il campo UUID.
 *
 * @param uuid l'UUID della persona da cercare
 * @return la {@link Person} corrispondente all'UUID fornito, o {@code null} se non trovata
 */
public Person findByUuid(String uuid) {
    return find("uuid", uuid).firstResult();
}
```

**2. `PersonResource.java`** - generare l'UUID alla creazione e usarlo come identificatore nei path:

- Alla creazione della persona, generare un UUID casuale:
```java
person.setUuid(UUID.randomUUID().toString());
```
- Restituire l'UUID invece dell'ID sequenziale nella risposta:
```java
response.setUuid(person.getUuid());
```
- Sostituire `{id}` con `{uuid}` negli endpoint `findPerson` e `deletePerson`:
```
GET    /person/find/{uuid}
DELETE /person/delete/{uuid}
```

#### Perché è importante

| | ID Sequenziale | UUID |
|---|---|---|
| Esempio | `/person/find/42` | `/person/find/a3f1c2d4-...` |
| Enumerabile | ✅ facilmente | ❌ praticamente impossibile |
| Prevedibile | ✅ sì | ❌ no |
| Sicurezza | ⚠️ bassa | ✅ alta |

> 💡 L'UUID non sostituisce i controlli di autorizzazione - è un ulteriore livello di difesa. Le vulnerabilità (1), (4) del laboratorio devono comunque essere corrette indipendentemente dall'uso degli UUID.

## Architettura della sicurezza

L'applicazione implementa un sistema di sicurezza a più livelli:

1. **Autenticazione JWT**: Verifica dell'identità tramite token firmati
2. **RBAC**: Controllo accessi basato su ruoli
3. **Object-Level Authorization**: Verifica permessi su singoli oggetti
4. **Field-Level Authorization**: Campi privilegiati modificabili solo dai ruoli idonei
5. **Test automatizzati**: Garanzia della copertura dei requisiti di sicurezza tramite tag JUnit

### Flusso di autenticazione
```
User → JWT Token → Quarkus Security → Role Check → Object Authorization → Field Authorization → Resource Access
```

## 📚 Riferimenti rapidi

**In esecuzione (dev):**

| Risorsa              | Link                                  |
|----------------------|---------------------------------------|
| Console didattica (GUI) | http://localhost:8080/ui/          |
| Swagger UI           | http://localhost:8080/q/swagger-ui/   |
| Dev UI               | http://localhost:8080/q/dev/          |
| Health Check         | http://localhost:8080/q/health        |
| OWASP Top 10 (2025)  | https://owasp.org/Top10/2025/         |
| OWASP API Security   | https://owasp.org/API-Security/       |
| JWT Debugger         | https://jwt.io/                       |
| Quarkus Security     | https://quarkus.io/guides/security    |

**Documentazione del progetto:**

| Documento | Contenuto |
|-----------|-----------|
| 🚀 [SECURITY-UNIT-TEST-QUICKSTART.md](SECURITY-UNIT-TEST-QUICKSTART.md) | Primo test BAC in 15 minuti |
| 📘 [SECURITY-UNIT-TEST.md](SECURITY-UNIT-TEST.md) | Guida ai security unit test |
| 🧭 [GUIDA-OPERATIVA.md](GUIDA-OPERATIVA.md) | Overview, metodi OpenAPI, catalogo test, dati demo |
| 📖 [JUNIT-TEST.md](JUNIT-TEST.md) | Note sugli unit test (indice delle classi) |
| 🏷️ [JUNIT-TAG.md](JUNIT-TAG.md) | Security JUnit con tagging |
| 🔧 [TROUBLESHOOTING.md](TROUBLESHOOTING.md) | Troubleshooting avanzato |
| 🤝 [CONTRIBUTING.md](CONTRIBUTING.md) | Come contribuire |
| 📋 [TODO.md](TODO.md) | Attività opzionali / roadmap |

## ❓ FAQ / Problemi comuni

<details>
<summary><b>Il token JWT scade troppo velocemente</b></summary>

I token hanno validità di 1 ora. Genera un nuovo token con:
```bash
curl http://localhost:8080/demo/admin,user.txt
```

Oppure usa la Swagger UI per rigenerarlo rapidamente.
</details>

<details>
<summary><b>Errore 403 anche con il token corretto</b></summary>

Verifica:
1. ✅ Token non scaduto (controlla `exp` su jwt.io)
2. ✅ Ruolo appropriato per l'endpoint (vedi tabella permessi)
3. ✅ Header Authorization corretto: `Bearer <token>` (con lo spazio)
4. ✅ Token copiato completamente senza spazi extra
</details>

<details>
<summary><b>I test di sicurezza non vengono eseguiti</b></summary>

Usa il profilo security:
```bash
mvn verify -P security
```

Il profilo `security` attiva il plugin `junit5-tag-check-maven-plugin` che verifica la copertura dei test taggati.
</details>

<details>
<summary><b>Quarkus non si avvia - porta 8080 occupata</b></summary>

Cambia la porta in `application.properties`:
```properties
quarkus.http.port=8081
```

Oppure termina il processo che occupa la porta 8080:
```bash
# Linux/Mac
lsof -ti:8080 | xargs kill -9

# Windows
netstat -ano | findstr :8080
taskkill /PID <PID> /F
```
</details>

<details>
<summary><b>Errore 401 Unauthorized su tutti gli endpoint</b></summary>

Hai dimenticato di autorizzare nella Swagger UI. Clicca sul pulsante "Authorize" in alto a destra e inserisci il token nel formato:
```
Bearer eyJ0eXAiOiJKV1QiLCJhbGc...
```
</details>

<details>
<summary><b>Come faccio a vedere Richard Feynman?</b></summary>

Richard Feynman ha `minRole=admin`, quindi serve un token con ruolo `admin`:
```bash
curl http://localhost:8080/demo/admin.txt
```

Poi usa questo token per chiamare `/person/list` o `/doc/example.md`.
</details>

## Licenza

Questo progetto è rilasciato sotto licenza MIT - vedi il file [LICENSE](LICENSE) per i dettagli.

---

**Sviluppato con ❤️ per la community della sicurezza applicativa**
