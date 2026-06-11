# Broken Access Control Lab

Un laboratorio educativo completo per testare e comprendere le vulnerabilità [Broken Access Control](https://owasp.org/Top10/2025/A01_2025-Broken_Access_Control/) nelle applicazioni Java.

> ⚠️ **ATTENZIONE**: Questo progetto contiene **intenzionalmente vulnerabilità di sicurezza** a scopo educativo. **NON utilizzare in produzione** e **NON esporre pubblicamente** senza aver rimosso tutte le vulnerabilità dimostrative.

> 🟢 Questo branch contiene la **versione sanata** (vulnerabilità corrette, test verdi). Un branch con la versione vulnerabile, da usare come punto di partenza degli esercizi TDD, è previsto in roadmap (vedi [TODO.md](TODO.md)).

Le vulnerabilità di tipo [Broken Access Control](https://owasp.org/Top10/2025/A01_2025-Broken_Access_Control/) sono attualmente le più diffuse secondo il progetto [OWASP](https://owasp.org/). Sono al primo posto sia nella [OWASP Top 10](https://owasp.org/Top10/) del [2021](https://owasp.org/Top10/2021/) che [2025](https://owasp.org/Top10/2025/).

[![Keep a Changelog v1.1.0 badge](https://img.shields.io/badge/changelog-Keep%20a%20Changelog%20v1.1.0-%23E05735)](CHANGELOG.md)
[![License: MIT](https://img.shields.io/badge/License-MIT-teal.svg)](https://opensource.org/licenses/MIT)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=lab-sca_lab-broken-access-control-quarkus&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=lab-sca_lab-broken-access-control-quarkus)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=lab-sca_lab-broken-access-control-quarkus&metric=coverage)](https://sonarcloud.io/summary/new_code?id=lab-sca_lab-broken-access-control-quarkus)

## Indice

- [Due modi di usare il progetto](#due-modi-di-usare-il-progetto)
- [Il progetto](#il-progetto)
- [Quickstart](#quickstart)
- [Lo scenario](#lo-scenario)
- [Workflow del laboratorio](#workflow-del-laboratorio)
- [Vulnerabilità dimostrative](#vulnerabilità-dimostrative)
  - [🎁 Bonus — Conversione degli ID in UUID](#-bonus--conversione-degli-id-in-uuid)
- [Architettura della sicurezza](#architettura-della-sicurezza)
- [📚 Riferimenti rapidi](#-riferimenti-rapidi)
- [❓ FAQ / Problemi comuni](#-faq--problemi-comuni)
- [Licenza](#licenza)

## Due modi di usare il progetto

### 🧪 Percorso 1 — Il laboratorio

Parti da una versione vulnerabile, fai fallire i test e correggi le vulnerabilità (1)–(5) + la BONUS (X)
su [DocResource](src/main/java/org/fugerit/java/demo/lab/broken/access/control/DocResource.java).
È il percorso descritto in questo README: [Quickstart](#quickstart) → [Lo scenario](#lo-scenario) →
[Workflow del laboratorio](#workflow-del-laboratorio) → [Vulnerabilità dimostrative](#vulnerabilità-dimostrative).

### 🏢 Percorso 2 — Riferimento formativo aziendale

Il progetto è anche un **riferimento per scrivere unit test di sicurezza sui controlli di
autorizzazione** (OWASP A01), a complemento degli strumenti SAST/DAST. Scenari di riferimento
(esempi già corretti, con test esemplari):

- **Function Level Access Control & verb tampering** → [DocResourceFunctionLevelTest](src/test/java/org/fugerit/java/demo/lab/broken/access/control/DocResourceFunctionLevelTest.java)
- **Mass assignment & Field-Level Authorization** → [DocResourceFieldLevelTest](src/test/java/org/fugerit/java/demo/lab/broken/access/control/DocResourceFieldLevelTest.java)
- **Ownership-based access** (dati personali: visibili a owner o admin) → endpoint `/doc/note`, [PersonalNoteResourceTest](src/test/java/org/fugerit/java/demo/lab/broken/access/control/PersonalNoteResourceTest.java)
- **Multi-tenant per ufficio + gerarchia ruoli** (admin di un altro ufficio escluso; draft/published; sharing) → endpoint `/doc/officedoc`, [OfficeDocumentResourceTest](src/test/java/org/fugerit/java/demo/lab/broken/access/control/OfficeDocumentResourceTest.java)
- **Appuntamenti: visibilità multi-parte + autorizzazione temporale** (delete solo dal creatore e solo > 24h prima) → endpoint `/doc/appointment`, [AppointmentResourceTest](src/test/java/org/fugerit/java/demo/lab/broken/access/control/AppointmentResourceTest.java)

Documenti dedicati:

- 📘 **[SECURITY-TESTING-GUIDE.md](SECURITY-TESTING-GUIDE.md)** — *come progettare* i test: anatomia di un test di autorizzazione, ponte SAST/DAST → unit test, pattern/anti-pattern, percorso a difficoltà incrementale, matrice di copertura.
- 🧭 **[GUIDA-OPERATIVA.md](GUIDA-OPERATIVA.md)** — onboarding step-by-step, descrizione di tutti i metodi OpenAPI, catalogo dei test dal più basilare al più avanzato, **identità e dati demo** per provare gli scenari a grana fine.
- 🖥️ **Console didattica** su <http://localhost:8080/ui/> (in dev): prova gli scenari dal browser cambiando identità e osservando esito + spiegazione.

## Il progetto

Questo progetto dimostra come implementare una strategia di testing basata su tag JUnit per garantire la copertura dei requisiti di sicurezza in un'applicazione Quarkus con autenticazione JWT e RBAC (Role-Based Access Control).

Completando il laboratorio acquisirai competenze pratiche su:

- 🔐 **Autenticazione JWT**: implementazione e configurazione in Quarkus
- 🛡️ **RBAC**: design e implementazione di Role-Based Access Control
- 🐛 **Vulnerability Detection**: identificazione di BOLA, IDOR e privilege escalation
- ✅ **Security Testing**: strategia di test con JUnit tags e coverage
- 📊 **Security Metrics**: misurazione della copertura dei requisiti di sicurezza
- 🔒 **Defense in Depth**: approccio a più livelli per la sicurezza applicativa

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

Qui c'è un payload del JWT completo generato come esempio:
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

L'applicazione è configurata per gestire 3 ruoli e 4 path, che generano lo stesso documento in formati diversi. Non tutti i ruoli sono autorizzati a generare ogni path. Ecco la mappa dei permessi:

| Path                          | Output      | Ruoli autorizzati  | Metodo http |
|-------------------------------|-------------|--------------------|-------------|
| `/doc/example.md` (*)         | 📝 MarkDown | admin, user, guest | GET         |
| `/doc/example.adoc`           | 📄 AsciiDoc | admin              | GET         |
| `/doc/example.html` (*)       | 🌐 HTML     | admin, user        | GET         |
| `/doc/example.pdf`            | 📑 PDF      | admin              | GET         |
| `/doc/person/list` (*)        | 📋 JSON     | admin, user        | GET         |
| `/doc/person/find/{uuid}` (*) | 📋 JSON     | admin, user        | GET         |
| `/doc/person/add`             | 📋 JSON     | admin              | POST        |
| `/doc/person/edit/{uuid}`     | 📋 JSON     | admin, user (**)   | PUT         |
| `/doc/person/delete/{uuid}`   | 📋 JSON     | admin              | DELETE      |

> (*) Eccetto gli utenti con ruolo 'admin', su questi path potrebbe esserci una limitazione ai dati mostrati in base al ruolo minimo richiesto.

> (**) Il ruolo 'user' può modificare solo i campi anagrafici (nome, cognome, titolo); il campo privilegiato `minRole` è modificabile solo da 'admin' (autorizzazione a livello di campo).

**Ruoli e permessi dettagliati:**

| Ruolo   | Permessi                           | Esempio di utilizzo                         |
|---------|------------------------------------|---------------------------------------------|
| `admin` | Accesso completo a tutti i formati | Vedere Richard Feynman, gestire persone     |
| `user`  | Accesso a MarkDown e HTML          | Vedere Hack e Turing, documenti base        |
| `guest` | Accesso solo a MarkDown            | Visualizzazione read-only limitata          |

Gli scenari di autorizzazione a grana fine (note personali, documenti di ufficio, appuntamenti) sono
descritti in [GUIDA-OPERATIVA.md](GUIDA-OPERATIVA.md) (endpoint, regole, identità e dati demo).

## Workflow del laboratorio

### Passo 1: Setup iniziale
```bash
git clone https://github.com/fugerit79/lab-broken-access-control-quarkus.git
cd lab-broken-access-control-quarkus
mvn quarkus:dev
```

### Passo 2: Esplora le vulnerabilità

- Apri `DocResource.java`
- Cerca i commenti `// VULNERABILITY: (n)`
- Analizza il codice vulnerabile
- Identifica il tipo di vulnerabilità (IDOR, BOLA, etc.)

### Passo 3: Esegui i test
```bash
mvn verify -P security
```

I test falliranno dove ci sono vulnerabilità. Osserva gli errori per capire cosa non funziona.

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

Questo laboratorio include 6 vulnerabilità reali di tipo Broken Access Control:

| #   | Vulnerabilità                 | Classificazione | Endpoint                                                   | Status   |
|-----|-------------------------------|-----------------|------------------------------------------------------------|----------|
| (1) | ID Enumeration                | IDOR            | `/person/find/{id}`                                        | 🟢 Fixed |
| (2) | Privilege Escalation (Data)   | BOLA            | `/doc/example.md`, `/doc/example.html`, `/doc/person/list` | 🟢 Fixed |
| (3) | Privilege Escalation (Action) | BOLA            | `/doc/person/delete/{id}`                                  | 🟢 Fixed |
| (4) | Broken Object Authorization   | BOLA            | `/doc/person/find/{id}`                                    | 🟢 Fixed |
| (5) | Missing Authentication        | Access Control  | `/doc/example.md`                                          | 🟢 Fixed |
| (X) | Hidden Vulnerability (BONUS)  | Access Control  | `/person/add` (PUT)                                        | 🟢 Fixed |

> 💡 **Sfida**: La vulnerabilità (X) non è coperta dai test. Riesci a trovarla?

### Descrizione delle vulnerabilità

#### (1) ID Enumeration (IDOR)

È possibile individuare gli identificativi delle persone esistenti distinguendo tra risposte 404 (non esiste) e 403 (non autorizzato).

**Endpoint**: `/person/find/{id}`

**Problema**: Risposta diversa per ID esistenti vs non esistenti
```
GET /person/999  → 404 Not Found (non esiste)
GET /person/10002 → 403 Forbidden (esiste ma non autorizzato)
```

**Soluzione**: Risposta uniforme (sempre 403) per evitare enumerazione

#### (2) Privilege Escalation - Visualizzazione dati

L'utente riesce a vedere dati che dovrebbero essere disponibili solo per il profilo 'admin'.

**Endpoint**: `/doc/example.md`, `/doc/example.html`, `/doc/person/list`

**Problema**: Utenti con ruolo 'user' vedono Richard Feynman (minRole=admin)

**Soluzione**: Filtrare i dati in base ai ruoli dell'utente autenticato

#### (3) Privilege Escalation - Cancellazione

L'utente riesce a cancellare una persona anche se non ha il ruolo 'admin'.

**Endpoint**: `/doc/person/delete/{id}`

**Problema**: `@RolesAllowed` include erroneamente "user"

**Soluzione**: Rimuovere "user" da `@RolesAllowed`, lasciando solo "admin"

#### (4) Broken Object Level Authorization

L'utente riesce a vedere dati che non dovrebbero essere disponibili per il suo profilo.

**Endpoint**: `/doc/person/find/{id}`

**Problema**: Verifica del ruolo minimo mancante

**Soluzione**: Controllare `person.getMinRole()` vs `securityIdentity.getRoles()`

#### (5) Missing Authentication

L'utente riesce ad accedere al documento anche se non è autenticato.

**Endpoint**: `/doc/example.md`

**Problema**: `@RolesAllowed` annotation mancante

**Soluzione**: Aggiungere `@RolesAllowed({ "admin", "user", "guest" })`

#### (x) Hidden Vulnerability (BONUS)

una put senza controllo di autorizzazione è rimasta abilitata per errore.

**Endpoint**: `/person/add` (PUT)

**Problema**: Il metodo è utilizzabile senza autenticazione

**Soluzione**: rimuoviamo totalmente il metodo addPersonPut()

```java
    @APIResponse(responseCode = "201", description = "La persona è stata creata", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AddPersonResponseDTO.class)))
    @APIResponse(responseCode = "401", description = "Se l'autenticazione non è presente")
    @APIResponse(responseCode = "403", description = "Se l'utente non è autorizzato per la risorsa")
    @APIResponse(responseCode = "500", description = "In caso di errori non gestiti")
    @Tag(name = "person")
    @Operation(operationId = "addPersonPut", summary = "Aggiunge una persona al database (ruoli: admin)", description = "Vanno forniti i parametri, nome, cognome, titolo e ruolo minimo.")
    @PUT
    @Path("/person/add")
    @Transactional
    public Response addPersonPut(AddPersonRequestDTO request) {
        return this.addPerson(request);
    }
```

---

Le vulnerabilità da risolvere saranno presenti a partire dal servizio REST:

- [DocResource](src/main/java/org/fugerit/java/demo/lab/broken/access/control/DocResource.java)

Visto che questo progetto segue l'approccio del *Test-driven development* abbiamo scritto prima i test della nostra applicazione, ovvero:

- [DocResourceSicurezzaTest](src/test/java/org/fugerit/java/demo/lab/broken/access/control/DocResourceSicurezzaTest.java) - Test di sicurezza, in particolare gli accessi non autorizzati

I casi di test dove sono presenti vulnerabilità falliranno, per quelli sarà presente il commento:
```java
// VULNERABILITY: (n) risolvi questa vulnerabilità in modo che il caso di test funzioni.
```

Una volta pubblicate le soluzioni, le potrai trovare cercando il commento:
```java
// SOLUTION: (n) 
```

Dove (n) è l'id del comportamento vulnerabile introdotto, ad esempio (1).

In totale saranno presenti 5 vulnerabilità. Ognuna farà fallire uno dei casi di test. Solo la numero (2) farà fallire 2 casi di test.

> **BONUS**: C'è un path che contiene una vulnerabilità non censita negli unit test, nella soluzione sarà censita come SOLUTION: (X)

Da notare che prima della risoluzione, l'esecuzione della suite di test *DocResourceSicurezzaTest* porterà a questo risultato (6 casi di test falliti)

![unit test falliti](./src/docs/image/junit-tofix.png)

Mentre dopo aver applicato le patch il risultato dovrebbe essere un questo

![unit test riusciti](./src/docs/image/junit-fixed.png)

Buon lavoro!

### 🎁 Bonus — Conversione degli ID in UUID

Come visto nelle [vulnerabilità dimostrative](#vulnerabilità-dimostrative) (1) e (4), l'uso di **ID sequenziali** espone l'applicazione a vulnerabilità di tipo **IDOR** (Insecure Direct Object Reference), rendendo banale per un attaccante enumerare le risorse.

Una buona pratica è sostituire gli ID sequenziali con **UUID** casuali come identificatori pubblici delle risorse.

#### Modifiche necessarie

**1. `PersonRepository.java`** — aggiungere il metodo di ricerca per UUID:
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

**2. `DocResource.java`** — generare l'UUID alla creazione e usarlo come identificatore nei path:

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
GET    /doc/person/find/{uuid}
DELETE /doc/person/delete/{uuid}
```

#### Perché è importante

| | ID Sequenziale | UUID |
|---|---|---|
| Esempio | `/person/find/42` | `/person/find/a3f1c2d4-...` |
| Enumerabile | ✅ facilmente | ❌ praticamente impossibile |
| Prevedibile | ✅ sì | ❌ no |
| Sicurezza | ⚠️ bassa | ✅ alta |

> 💡 L'UUID non sostituisce i controlli di autorizzazione — è un ulteriore livello di difesa. Le vulnerabilità (1), (4) del laboratorio devono comunque essere corrette indipendentemente dall'uso degli UUID.

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
| 📘 [SECURITY-TESTING-GUIDE.md](SECURITY-TESTING-GUIDE.md) | Guida ai security unit test (formazione) |
| 🧭 [GUIDA-OPERATIVA.md](GUIDA-OPERATIVA.md) | Onboarding, metodi OpenAPI, catalogo test, dati demo |
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

Poi usa questo token per chiamare `/doc/person/list` o `/doc/example.md`.
</details>

<details>
<summary><b>I test passano ma la vulnerabilità è ancora presente</b></summary>

Ricorda che ci sono 6 vulnerabilità:
- 5 coperte dai test (che devono passare)
- 1 BONUS non coperta dai test (devi trovarla manualmente)

Cerca `// SOLUTION: (X)` nel codice per vedere la vulnerabilità nascosta.
</details>

## Licenza

Questo progetto è rilasciato sotto licenza MIT - vedi il file [LICENSE](LICENSE) per i dettagli.

---

**Sviluppato con ❤️ per la community della sicurezza applicativa**
