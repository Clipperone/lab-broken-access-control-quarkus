// Gestione dell'identità demo: generazione token via endpoint /demo, decodifica e persistenza.
// Nota didattica: il token è tenuto in sessionStorage; in un'app reale sarebbe esposto a XSS
// (valutare cookie httpOnly). L'ufficio è un claim del token (identità), non un dato di input.

const KEY = 'lab-bac-token';

export function getToken() {
    return sessionStorage.getItem(KEY) || '';
}

export function setToken(token) {
    sessionStorage.setItem(KEY, token);
    window.dispatchEvent(new CustomEvent('identity-changed'));
}

export function clearToken() {
    sessionStorage.removeItem(KEY);
    window.dispatchEvent(new CustomEvent('identity-changed'));
}

/**
 * Genera un JWT tramite gli endpoint demo:
 *  - con ufficio: GET /demo/office/{office}/{upn}/{roles}.txt
 *  - senza ufficio: GET /demo/{roles}.txt
 */
export async function generate(upn, office, roles) {
    const rolesPart = roles.join(',') || 'guest';
    const url = (office && office.trim())
        ? `/demo/office/${office.trim()}/${upn || 'DEMOUSER'}/${rolesPart}.txt`
        : `/demo/${rolesPart}.txt`;
    const res = await fetch(url);
    if (!res.ok) {
        throw new Error(`Generazione token fallita (${res.status}). In 'prod' gli endpoint /demo sono disattivati.`);
    }
    const token = (await res.text()).trim();
    setToken(token);
    return token;
}

/** Decodifica il payload del JWT corrente (senza verifica della firma: solo per visualizzazione). */
export function decode() {
    const t = getToken();
    if (!t) return null;
    try {
        const part = t.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
        return JSON.parse(decodeURIComponent(escape(atob(part))));
    } catch (e) {
        return null;
    }
}
