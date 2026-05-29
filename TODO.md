# TODO — Operazioni opzionali (da valutare)

Elenco di interventi **opzionali**, emersi durante la trasformazione del progetto in riferimento
formativo per i security unit test sull'autorizzazione. Non sono necessari al funzionamento attuale
(build verde: `mvn verify -P security`); sono migliorie da valutare quando opportuno.

> Branch di lavoro corrente: `feature/security-testing-training`.
> Legenda effort/rischio: 🟢 basso · 🟡 medio · 🔴 alto.

---

## 1. `DocResourceTest`: allineare alla linea guida "positivo + negativo per metodo" — 🟡

**Contesto.** Oggi [DocResourceTest](src/test/java/org/fugerit/java/demo/lab/broken/access/control/DocResourceTest.java)
contiene solo 3 casi positivi (`testMarkdownOk`, `testHtmlOk`, `testAsciiDocOk`, tutti 200), taggati
`business`/`success`. Non ha casi negativi (401/403) e non copre tutti i metodi. È una classe di smoke
test funzionale, distinta dalla suite di sicurezza.

**Opzioni:**
- **(a)** Per ogni metodo aggiungere il gemello negativo (401/403) e taggare `security` + esito
  (`forbidden`/`unauthorized`). Effetto collaterale positivo: con il tag `security` i test rientrano
  anche nell'esecuzione (vedi punto 2/W1).
- **(b)** Consolidare i casi positivi dentro
  [DocResourceSicurezzaTest](src/test/java/org/fugerit/java/demo/lab/broken/access/control/DocResourceSicurezzaTest.java)
  ed eliminare la duplicazione (i 401/403 sono già lì).

**Dipendenze.** Intrecciato con il punto 2 (filtro `<groups>`): valutarli insieme.

---

## 2. W1 — Filtro `<groups>` di surefire (test esclusi dall'esecuzione) — 🟡

**Contesto.** In [pom.xml](pom.xml) `maven-surefire-plugin` ha
`<groups>security,unauthorized,forbidden,authorized</groups>`: vengono eseguiti solo i test con almeno
uno di quei tag. Restano quindi **mai eseguiti** in `mvn verify` (e nello step Sonar, che gira senza
`-P security`):
- `DocResourceTest` → `testMarkdownOk`, `testHtmlOk`, `testAsciiDocOk` (tag `business`/`success`)
- `DocHelperTest` → `testDocProcess` (nessun tag)
- `DemoJwtGeneratorRestTest` → `testDemoAdminToken` (tag `demo`)

`DocResourceIT` **non** è coinvolto (è failsafe/`@QuarkusIntegrationTest`, governato da `skipITs`).

**Opzioni:**
- **(a)** Rimuovere il filtro `<groups>` di default → `mvn verify` esegue tutti i test; il gate sui tag
  resta nel profilo `security`. Coverage Sonar più rappresentativa.
- **(b)** Taggare i 5 metodi con un tag già incluso (es. `authorized`/`business`+aggiungere `business` ai groups).

**Ripercussioni di (a):** anche `mvn verify -P security` eseguirebbe tutti i test; va aggiornata la nota
in [JUNIT-TAG.md](JUNIT-TAG.md) ("Note su test e coverage") e rivista la separazione dei due step in
[.github/workflows/ci.yml](.github/workflows/ci.yml). **Verificare** che i 5 test attivati passino.

---

## 3. Esercizi TDD su `branch-vulnerable` (varianti vulnerabili + test rossi) — 🔴

**Contesto.** Gli scenari nuovi sono presenti solo come **esempi di riferimento** (già corretti su
`main`). Mancano gli esercizi TDD red→green, perché il branch `branch-vulnerable` (citato dal README)
**non esiste in questo clone**.

**Da fare quando `branch-vulnerable` sarà disponibile:**
- **Field-level (editPerson):** introdurre la variante vulnerabile in `DocResource.editPerson` che fa
  binding di `minRole` anche per un `user` (privilege escalation di campo), con commento `// VULNERABILITY`.
  Test rosso: `testEditPersonUserCannotChangeMinRole` (un `user` imposta `minRole=admin` → atteso 403).
- **Function-level / verb tampering (vuln X):** reintrodurre un metodo `PUT /doc/person/add` **senza**
  `@RolesAllowed` (`// VULNERABILITY: (X)`). Test rosso che invoca la PUT e attende 401/403.
  Fix: rimuovere il metodo (`// SOLUTION: (X)`) e/o `deny-unannotated-endpoints` (già attivo su `main`).

**Coordinamento branch:** la variante vulnerabile + test rosso su `branch-vulnerable`; la `// SOLUTION`
+ test verde su `main`. Confermare la convenzione esatta dei due rami.

---

## 4. Estendere la matrice di copertura alle celle mancanti — 🟢

**Contesto.** La matrice in [SECURITY-TESTING-GUIDE.md](SECURITY-TESTING-GUIDE.md) ha celle senza test
(buoni esercizi di estensione). Esempi:
- `guest` su `GET /doc/person/list` e `GET /doc/person/find/{uuid}` → atteso 403 (function-level).
- `guest` su `GET /doc/example.html` → atteso 403.
- `anonimo` (senza JWT) su `POST /doc/person/add`, `PUT /doc/person/edit/{uuid}`, `DELETE …` → 401.
- `guest` su `PUT /doc/person/edit/{uuid}` con oggetto non accessibile → 403 (object-level).

Aggiungere i test mancanti, taggati coerentemente, e spuntare le celle nella matrice.

---

## 5. Nuance "senior": modello dei ruoli — gerarchia vs set-membership (W7) — 🟡

**Contesto.** Il filtro `minRole in ?1` in
[PersonRepository.findByRolesOrderedByName](src/main/java/org/fugerit/java/demo/lab/broken/access/control/persistence/PersonRepository.java)
è **set-membership**, non una gerarchia di ruoli: un token `{admin}` privo di `user` non vedrebbe un
oggetto con `minRole=user`. Regge solo perché i token demo sono cumulativi.

**Da fare:** esercizio/esempio dedicato che evidenzia il rischio (token non cumulativo) e mostra
l'alternativa (gerarchia esplicita dei ruoli o normalizzazione dei ruoli a monte).

---

## 6. Gate di coverage JaCoCo con soglia minima — 🟢

**Contesto.** È presente la dipendenza `quarkus-jacoco` ma nessuna soglia di copertura imposta
localmente (solo SonarCloud lato CI). Valutare un'esecuzione `jacoco:check` con soglia minima nel
`pom.xml` per un gate locale.

---

## 7. `.github/codeql-config.yml` con query `security-and-quality` — 🟢

**Contesto.** CodeQL (SAST) gira con le query di default (solo security). Aggiungere una config con il
set `security-and-quality` allinea il SAST alla narrativa formativa (più finding didattici).

---

## 8. Doc: arricchire la descrizione della vuln (X) con il deny-by-default — 🟢

**Contesto.** Nel [README.md](README.md) la soluzione della (X) è descritta come "rimuovere il metodo
`addPersonPut()`". Aggiungere che il **deny-by-default**
(`quarkus.security.jaxrs.deny-unannotated-endpoints=true`, già attivo) è la mitigazione *strutturale*
della stessa classe (un endpoint senza annotazione è negato anche se ci si dimentica di rimuoverlo).

---

## 9. Test di integrazione nativi per la suite di sicurezza — 🟡

**Contesto.** L'unico `*IT` presente è
[DocResourceIT](src/test/java/org/fugerit/java/demo/lab/broken/access/control/DocResourceIT.java)
(`@QuarkusIntegrationTest extends DocResourceTest`): in modalità *packaged*/nativa riesegue solo i 3
casi positivi di generazione documento. Gli scenari di **autorizzazione** (401/403, object/function/
field-level) **non vengono verificati in nativo**, perché non esiste un `*IT` per le suite di sicurezza.

**Da fare (se serve garantire l'authz anche sull'artefatto nativo):** aggiungere
`DocResourceSicurezzaIT extends DocResourceSicurezzaTest` (ed eventualmente i corrispettivi per
`DocResourceFunctionLevelTest` / `DocResourceFieldLevelTest`), così gli stessi test girano anche con
`mvn verify -Dnative`. **Verificare** che i JWT reali (RS256) e `@TestSecurity` funzionino in nativo
(possibili aggiustamenti di reflection/risorse).
