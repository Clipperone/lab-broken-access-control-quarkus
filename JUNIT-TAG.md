## Security JUnit con tagging

### Strategia di testing

Il progetto utilizza un approccio basato su **tag JUnit** per garantire la copertura completa dei requisiti di sicurezza.

### Definizione dei tag di sicurezza

Definiamo i gruppi di test con cui vogliamo classificare i nostri test.

**Tag per esito HTTP** (autenticazione vs autorizzazione):

| Tag            | Descrizione                                                 | Status Code atteso |
|----------------|-------------------------------------------------------------|--------------------|
| `authorized`   | Test per accessi autorizzati                                | 200, 201           |
| `unauthorized` | Test per utenti **non autenticati** (JWT mancante o non valido) — *autenticazione* | 401   |
| `forbidden`    | Test per utenti autenticati **senza i permessi** necessari — *autorizzazione* | 403         |
| `security`     | Tag generico per qualsiasi altro controllo di sicurezza     | vari               |

> 🔑 **401 vs 403**: `401 Unauthorized` risponde a *"chi sei?"* (autenticazione mancante); `403 Forbidden` risponde a *"cosa puoi fare?"* (sei autenticato ma non autorizzato). Questo laboratorio è incentrato sull'**autorizzazione** (403 e classi sotto), non sull'autenticazione.

**Tag per classe di autorizzazione** (OWASP A01 — verificati dal gate, vedi `pom.xml`):

| Tag              | Classe di vulnerabilità                                              |
|------------------|---------------------------------------------------------------------|
| `object-level`   | Broken Object Level Authorization (BOLA) / IDOR su singolo oggetto   |
| `function-level` | Missing Function Level Access Control / verb tampering              |
| `field-level`    | Mass assignment / autorizzazione a livello di singolo campo          |
| `ownership`      | Accesso basato sulla proprietà del dato (owner / admin)              |
| `tenant`         | Isolamento multi-tenant (per ufficio) + gerarchia di ruoli          |
| `temporal`       | Autorizzazione basata sul tempo (es. finestra di cancellazione)     |

### Esempio di test

Ecco un esempio di test con tag `forbidden`:

```java
@Test
@Tag("security")
@Tag("forbidden")
void testAsciiDoc403NoAdminRole() {
    // l'utente con ruoli {user, guest} NON ha 'admin', richiesto da /doc/example.adoc -> 403
    String token = DemoJwtGeneratorRest.generateUserToken();
    given()
        .header("Authorization", "Bearer " + token)
        .when().get("/doc/example.adoc")
        .then().statusCode(Response.Status.FORBIDDEN.getStatusCode());
}
```

### Verifica presenza test

Ci sono vari modi per verificare la presenza di test sui tag definiti.

Per questa demo usiamo il più semplice, ovvero andremo a verificare con il [maven-surefire-plugin](https://maven.apache.org/surefire/maven-surefire-plugin/) che sia presente almeno un test per ogni tag.

Questo può essere fatto con una execution del plugin per ogni tag, es:

```xml
<execution>
    <id>verify-security-tests</id>
    <phase>test</phase>
    <goals>
        <goal>test</goal>
    </goals>
    <configuration>
        <groups>security</groups>
        <failIfNoTests>true</failIfNoTests>
    </configuration>
</execution>
```

Nel nostro caso attiviamo questo controllo con il profilo `security`:

```shell
mvn verify -P security
```

> **Nota**: È possibile usare questo meccanismo per verificare anche altri tag custom definiti dallo sviluppatore.

### Note su test e coverage

Un effetto collaterale dell'utilizzo del profilo `security` è che vengono eseguiti solo i test con i tag definiti.

Nella nostra CI per ovviare a questa situazione, abbiamo separato lo step di verifica da quello per il calcolo del quality gate e coverage:

```yaml
- name: Check security unit test tags
  run: mvn verify -P security
  
- name: Build and analyze
  env:
    SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
  run: mvn -B clean install org.sonarsource.scanner.maven:sonar-maven-plugin:sonar -Dsonar.organization=fugerit79 -Dsonar.projectKey=fugerit79_lab-broken-access-control-quarkus
```

> **Nota**: In futuro potremmo rendere più robusto il meccanismo, ad esempio con un sistema più personalizzabile di verifica (es. custom maven plugin).