// Bootstrap della console: barra identità, binding delle azioni dei 4 domini, pannello esito.
import { call } from './api.js';
import * as auth from './auth.js';

const $ = (id) => document.getElementById(id);
const val = (id) => ($(id).value || '').trim();
const enc = (s) => encodeURIComponent((s || '').trim());

// --- Barra identità ---

function renderIdentity() {
    const p = auth.decode();
    const el = $('identity-info');
    if (!p) {
        el.textContent = 'Nessuna identità (genera un token)';
        el.className = 'id-empty';
        return;
    }
    const exp = p.exp ? new Date(p.exp * 1000).toLocaleTimeString() : '?';
    const roles = (p.groups || []).join(', ') || '—';
    el.innerHTML = `upn: <b>${p.upn || p.sub || '?'}</b> &nbsp;|&nbsp; office: <b>${p.office || '—'}</b> &nbsp;|&nbsp; roles: <b>${roles}</b> &nbsp;|&nbsp; exp: ${exp}`;
    el.className = 'id-ok';
}

function currentRoles() {
    return ['admin', 'user', 'guest'].filter((r) => $('role-' + r).checked);
}

async function genToken() {
    try {
        await auth.generate(val('upn') || 'DEMOUSER', val('office'), currentRoles());
    } catch (e) {
        alert(e.message);
    }
}

function preset(upn, office, roles) {
    $('upn').value = upn;
    $('office').value = office;
    ['admin', 'user', 'guest'].forEach((r) => ($('role-' + r).checked = roles.includes(r)));
    genToken();
}

// --- Pannello esito ---

function escapeHtml(s) {
    return (s || '').replace(/[&<>]/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;' }[c]));
}

window.addEventListener('api-result', (ev) => {
    const d = ev.detail;
    const cls = d.status >= 200 && d.status < 300 ? 's2xx'
        : d.status === 401 ? 's401'
        : d.status === 403 ? 's403'
        : d.status === 405 ? 's405' : 'serr';
    const entry = document.createElement('div');
    entry.className = 'resp ' + cls;
    entry.innerHTML = `<div class="resp-line"><span class="method">${d.method}</span> ${d.path} &rarr; <b>${d.status || 'ERR'}</b></div>`
        + `<div class="resp-exp">${d.explanation}</div>`
        + `<details><summary>corpo della risposta</summary><pre>${escapeHtml(d.text).slice(0, 4000)}</pre></details>`;
    $('response-panel').prepend(entry);
});

// dopo una create, copia l'uuid restituito nel campo uuid del dominio
function maybeUuid(detail, fieldId) {
    try {
        const j = JSON.parse(detail.text);
        if (j && j.uuid) $(fieldId).value = j.uuid;
    } catch (e) { /* corpo non JSON: ignora */ }
}

// --- Binding ---

function bind() {
    // identità
    $('btn-gen').onclick = genToken;
    $('btn-logout').onclick = () => auth.clearToken();
    $('btn-clear').onclick = () => ($('response-panel').innerHTML = '');
    $('preset-einstein').onclick = () => preset('EINSTEIN', 'FISICA', ['user', 'guest']);
    $('preset-bohr').onclick = () => preset('BOHR', 'FISICA', ['admin', 'user', 'guest']);
    $('preset-planck').onclick = () => preset('PLANCK', 'FISICA', ['guest']);
    $('preset-fermi').onclick = () => preset('FERMI', 'FISICA', ['user', 'guest']);
    $('preset-mendeleev').onclick = () => preset('MENDELEEV', 'CHIMICA', ['admin', 'user', 'guest']);
    $('preset-lavoisier').onclick = () => preset('LAVOISIER', 'CHIMICA', ['user', 'guest']);

    // documenti (azioni semplici via data-call="METODO path")
    document.querySelectorAll('button[data-call]').forEach((btn) => {
        const [method, path] = btn.getAttribute('data-call').split(' ');
        btn.onclick = () => call(method, path);
    });

    // persone
    const personBody = () => ({
        firstName: val('p-fn'), lastName: val('p-ln'), title: val('p-title'),
        minRole: val('p-minrole') || undefined,
    });
    $('btn-p-list').onclick = () => call('GET', '/doc/person/list');
    $('btn-p-find').onclick = () => call('GET', '/doc/person/find/' + enc(val('p-uuid')));
    $('btn-p-add').onclick = async () => maybeUuid(await call('POST', '/doc/person/add', personBody()), 'p-uuid');
    $('btn-p-edit').onclick = () => call('PUT', '/doc/person/edit/' + enc(val('p-uuid')), personBody());
    $('btn-p-del').onclick = () => call('DELETE', '/doc/person/delete/' + enc(val('p-uuid')));

    // note
    const noteBody = () => ({ title: val('n-title'), content: val('n-content') });
    $('btn-n-create').onclick = async () => maybeUuid(await call('POST', '/doc/note', noteBody()), 'n-uuid');
    $('btn-n-list').onclick = () => call('GET', '/doc/note/list');
    $('btn-n-read').onclick = () => call('GET', '/doc/note/' + enc(val('n-uuid')));
    $('btn-n-edit').onclick = () => call('PUT', '/doc/note/' + enc(val('n-uuid')), noteBody());
    $('btn-n-del').onclick = () => call('DELETE', '/doc/note/' + enc(val('n-uuid')));

    // documenti di ufficio
    const odBody = () => ({ fileName: val('o-file'), content: val('o-content') });
    $('btn-o-create').onclick = async () => maybeUuid(await call('POST', '/doc/officedoc', odBody()), 'o-uuid');
    $('btn-o-list').onclick = () => call('GET', '/doc/officedoc/list');
    $('btn-o-read').onclick = () => call('GET', '/doc/officedoc/' + enc(val('o-uuid')));
    $('btn-o-edit').onclick = () => call('PUT', '/doc/officedoc/' + enc(val('o-uuid')), odBody());
    $('btn-o-del').onclick = () => call('DELETE', '/doc/officedoc/' + enc(val('o-uuid')));
    $('btn-o-pub').onclick = () => call('PUT', '/doc/officedoc/' + enc(val('o-uuid')) + '/publish');
    $('btn-o-share').onclick = () => call('POST', '/doc/officedoc/' + enc(val('o-uuid')) + '/share', { targetUpn: val('o-target') });

    // appuntamenti
    const apptBody = () => ({
        scientistUpn: val('a-scientist'), office: val('a-office'),
        appointmentAt: val('a-at') || undefined, subject: val('a-subject'),
    });
    $('btn-a-create').onclick = async () => maybeUuid(await call('POST', '/doc/appointment', apptBody()), 'a-uuid');
    $('btn-a-list').onclick = () => call('GET', '/doc/appointment/list');
    $('btn-a-read').onclick = () => call('GET', '/doc/appointment/' + enc(val('a-uuid')));
    $('btn-a-move').onclick = () => call('PUT', '/doc/appointment/' + enc(val('a-uuid')) + '/move', { newAppointmentAt: val('a-newat') || undefined });
    $('btn-a-del').onclick = () => call('DELETE', '/doc/appointment/' + enc(val('a-uuid')));

    window.addEventListener('identity-changed', renderIdentity);
    renderIdentity();
}

bind();
