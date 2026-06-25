## Troubleshooting

### Errore 401 Unauthorized

- Verifica che il token JWT sia stato generato correttamente
- Controlla che il token sia inserito nel formato `Bearer <token>`
- Verifica che il token non sia scaduto
- Assicurati che l'issuer del token corrisponda a quello configurato nell'applicazione

### Errore 403 Forbidden

- Controlla che l'utente abbia i ruoli necessari per l'endpoint richiesto
- Riferisciti alla [tabella dei permessi](README.md#grana-grossa---autorizzazione-per-ruolo-rbac) per verificare quali ruoli sono autorizzati
- Verifica che i ruoli siano stati correttamente inclusi nel JWT generato

### Build fallisce con profilo security

Verifica che tutti i tag richiesti siano coperti dai test:

```shell
mvn test -P security
```

Se mancano test per un tag specifico, il plugin `junit5-tag-check-maven-plugin` segnalerà l'errore con un messaggio chiaro.

### Problemi con la versione di Quarkus

Se riscontri errori di tipo `IllegalAccessError` o problemi con `ConfigMappingContext`, verifica:

1. Di utilizzare una versione stabile di Quarkus (attualmente 3.31.3)
2. Che il file `application.yaml` sia correttamente formattato
3. Di eseguire una pulizia completa: `mvn clean`