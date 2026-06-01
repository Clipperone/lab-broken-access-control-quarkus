// Mappa (status HTTP, path, metodo) -> spiegazione testuale didattica.
// Distinzione chiave: 401 = autenticazione (chi sei?) · 403 = autorizzazione (cosa puoi fare?).

export function explain(status, path, method) {
    if (status === 0) return "Errore di rete o nessuna risposta dal server.";
    if (status === 401) {
        return "401 Unauthorized — AUTENTICAZIONE mancante o non valida: serve un token. Genera un'identità in alto.";
    }
    if (status === 403) {
        if (path.includes("/appointment")) {
            if (path.includes("/move")) return "403 Forbidden — solo il creatore può spostare l'appuntamento.";
            if (method === "DELETE") return "403 Forbidden — l'eliminazione è consentita solo al creatore e solo se mancano più di 24h all'appuntamento (regola temporale).";
            return "403 Forbidden — l'appuntamento è visibile solo a creatore, scienziato destinatario o admin dello stesso ufficio; un ufficio diverso è escluso.";
        }
        if (path.includes("/officedoc")) {
            return "403 Forbidden — non sei l'owner, il documento non è condiviso con te, oppure (se pubblicato) non sei dello stesso ufficio con ruolo sufficiente. Un ufficio diverso non accede nemmeno da admin (isolamento di tenant).";
        }
        if (path.includes("/note")) {
            return "403 Forbidden — una nota è visibile solo all'owner (o a un admin in lettura) e modificabile solo dall'owner.";
        }
        if (path.includes("/person/")) {
            return "403 Forbidden — ruolo insufficiente per l'azione, oppure oggetto non accessibile per il suo ruolo minimo (object-level).";
        }
        return "403 Forbidden — AUTORIZZAZIONE negata: sei autenticato ma il tuo ruolo non basta per questa azione.";
    }
    if (status === 405) {
        return "405 Method Not Allowed — il metodo HTTP non è dichiarato per questo path: verb tampering bloccato dal framework.";
    }
    if (status === 400) {
        return "400 Bad Request — richiesta non valida (es. validazione dei campi: nome con caratteri non ammessi, minRole fuori whitelist...).";
    }
    if (status === 201) return "201 Created — risorsa creata (owner/ufficio impostati dal server, non dal client).";
    if (status === 200) return "200 OK — operazione consentita.";
    return `${status} — esito generico.`;
}
