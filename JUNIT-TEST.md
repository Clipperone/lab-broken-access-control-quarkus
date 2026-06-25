## Note sugli unit test

Questo progetto usa **JUnit 5**, **RestAssured** e le utility di test di Quarkus
(`@QuarkusTest`, `@TestSecurity`). I test sono classificati con tag (vedi [JUNIT-TAG.md](JUNIT-TAG.md))
e la copertura delle classi di sicurezza è verificata dal gate `junit5-tag-check` con `mvn verify -P security`.

### Indice delle classi di test

| Classe | Scopo | Pattern dimostrato |
|--------|-------|--------------------|
| [DocResourceSicurezzaTest](src/test/java/org/fugerit/java/demo/lab/broken/access/control/DocResourceSicurezzaTest.java) | Suite di sicurezza principale: accessi autorizzati/negati/non autenticati su tutti gli endpoint | `@TestSecurity` + JWT Bearer; 401 vs 403; data filtering; object-level (BOLA/IDOR) |
| [PersonResourceFunctionLevelTest](src/test/java/org/fugerit/java/demo/lab/broken/access/control/PersonResourceFunctionLevelTest.java) | **Function Level Access Control & verb tampering** | escalation verticale sulla creazione (403); verbo HTTP non dichiarato (405) |
| [PersonResourceFieldLevelTest](src/test/java/org/fugerit/java/demo/lab/broken/access/control/PersonResourceFieldLevelTest.java) | **Mass assignment & Field-Level Authorization** | campi server-controlled ignorati; campo privilegiato `minRole` modificabile solo da 'admin' |
| [PersonalNoteResourceTest](src/test/java/org/fugerit/java/demo/lab/broken/access/control/PersonalNoteResourceTest.java) | **Ownership-based access control** | dato visibile solo a owner/admin; modificabile solo dall'owner |
| [OfficeDocumentResourceTest](src/test/java/org/fugerit/java/demo/lab/broken/access/control/OfficeDocumentResourceTest.java) | **Multi-tenant (ufficio) + gerarchia ruoli** | isolamento per ufficio (admin altrui escluso), ruolo ≥, draft/published, sharing, mass-assignment |
| [AppointmentResourceTest](src/test/java/org/fugerit/java/demo/lab/broken/access/control/AppointmentResourceTest.java) | **Appuntamenti: multi-parte + business-logic** | visibilità creatore/destinatario/admin-ufficio; delete solo creatore e > 24h; niente doppia prenotazione (409); orizzonte massimo (422); move solo creatore; mass-assignment |
| [DocResourceTest](src/test/java/org/fugerit/java/demo/lab/broken/access/control/DocResourceTest.java) | Casi positivi "di business" sulla generazione documenti | smoke test funzionale con identità iniettata |
| [DocResourceIT](src/test/java/org/fugerit/java/demo/lab/broken/access/control/DocResourceIT.java) | Stessi test in modalità *packaged*/native | `@QuarkusIntegrationTest` (attivo con `-Dnative` o `skipITs=false`) |
| [DocHelperTest](src/test/java/org/fugerit/java/demo/lab/broken/access/control/DocHelperTest.java) | Rendering del template documentale (Fugerit Venus Doc) | unit test "puro" non di sicurezza |
| [DemoJwtGeneratorRestTest](src/test/java/org/fugerit/java/demo/lab/broken/access/control/DemoJwtGeneratorRestTest.java) | Endpoint dimostrativo di generazione JWT | test funzionale dell'endpoint demo |

### Due approcci di test di sicurezza

`DocResourceSicurezzaTest` mostra entrambi i pattern, complementari:

1. **`@TestSecurity`** - inietta utente e ruoli nel contesto di sicurezza, senza JWT reale. Veloce e leggibile, ideale per fissare *una* identità per test.
2. **JWT reale via RestAssured** - `.header("Authorization", "Bearer " + DemoJwtGeneratorRest.generateUserToken())`. Necessario per scenari **multi-identità** nello stesso test (es. *admin crea, user modifica*) e per testare il ciclo completo di verifica del token.

> Per la guida completa su *come progettare* questi test (anatomia, ponte SAST/DAST, pattern e anti-pattern, percorso a difficoltà incrementale), vedi **[SECURITY-UNIT-TEST.md](SECURITY-UNIT-TEST.md)**.
