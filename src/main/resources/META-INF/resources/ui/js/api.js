// Wrapper unico per le chiamate REST: aggiunge il Bearer token, esegue la fetch e pubblica l'esito
// (con spiegazione) tramite l'evento 'api-result', a cui il pannello in app.js è in ascolto.

import { getToken } from './auth.js';
import { explain } from './explain.js';

export async function call(method, path, body) {
    const headers = {};
    const token = getToken();
    if (token) headers['Authorization'] = 'Bearer ' + token;

    const opts = { method, headers };
    if (body !== undefined) {
        headers['Content-Type'] = 'application/json';
        headers['Accept'] = 'application/json';
        opts.body = JSON.stringify(body);
    }

    let status = 0;
    let text = '';
    try {
        const res = await fetch(path, opts);
        status = res.status;
        text = await res.text();
    } catch (e) {
        status = 0;
        text = String(e);
    }

    const detail = { method, path, status, text, explanation: explain(status, path, method) };
    window.dispatchEvent(new CustomEvent('api-result', { detail }));
    return detail;
}
