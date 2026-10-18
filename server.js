#!/usr/bin/env node
/**
 * CompartilhaLocal v2 — servidor Node ZERO dependências.
 * Upload GitHub: só este arquivo + package.json (sem pastas).
 * PWA embutida em memória; dados em ./data (criado se faltar).
 */
'use strict';
const http = require('http');
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const { URL } = require('url');

const PORT = Number(process.env.PORT || 8080);
const SECRET = (process.env.SECRET || '').trim();
const DATA = path.join(__dirname, 'data');
const MAX_PHOTO_B64 = 900_000;
const ONLINE_MS = 3 * 60 * 1000;
const HISTORY_MS = 24 * 60 * 60 * 1000;

/** Assets embutidos (texto + ícones PNG em base64) */
const ASSETS = {
  '/': { type: 'text/html; charset=utf-8', body: null },
  '/index.html': { type: 'text/html; charset=utf-8', body: null },
  '/css/app.css': { type: 'text/css; charset=utf-8', body: null },
  '/js/app.js': { type: 'application/javascript; charset=utf-8', body: null },
  '/manifest.webmanifest': { type: 'application/manifest+json', body: null },
  '/icons/icon-192.png': { type: 'image/png', body: null },
  '/icons/icon-512.png': { type: 'image/png', body: null },
};

(function loadEmbedded() {
  ASSETS['/'].body = Buffer.from(`<!DOCTYPE html>
<html lang="pt-BR">
<head>
<meta charset="utf-8"/>
<meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no,viewport-fit=cover"/>
<meta name="theme-color" content="#0B1F33"/>
<meta name="apple-mobile-web-app-capable" content="yes"/>
<meta name="apple-mobile-web-app-title" content="CompartilhaLocal"/>
<link rel="manifest" href="/manifest.webmanifest"/>
<link rel="icon" href="/icons/icon-192.png"/>
<link rel="apple-touch-icon" href="/icons/icon-192.png"/>
<link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css"/>
<link rel="stylesheet" href="/css/app.css"/>
<title>CompartilhaLocal</title>
</head>
<body>
<div id="app">
  <!-- Setup -->
  <section id="screen-setup" class="screen">
    <div class="setup-card">
      <div class="logo-mark">📍</div>
      <h1>CompartilhaLocal</h1>
      <p class="muted">Compartilhe a localização com quem importa. Interface inspirada em apps de família — marca e artes próprias.</p>
      <label>Seu nome</label>
      <input id="setup-nome" maxlength="40" placeholder="Como você aparece"/>
      <label>Senha do casal</label>
      <div class="row">
        <input id="setup-senha" type="password" minlength="4" placeholder="Mín. 4 caracteres" style="flex:1"/>
        <button type="button" class="btn ghost" id="btn-toggle-senha">Mostrar</button>
      </div>
      <label>URL do servidor</label>
      <input id="setup-url" placeholder="https://seu-app.onrender.com"/>
      <p class="hint">No emulador use http://10.0.2.2:8080. No celular use a URL do Render.</p>
      <button class="btn primary block" id="btn-setup">Continuar</button>
    </div>
  </section>

  <!-- Map (main) -->
  <section id="screen-map" class="screen hidden">
    <div id="map"></div>
    <div class="top-bar">
      <button class="chip" id="btn-places">Lugares</button>
      <div class="brand-pill">CompartilhaLocal</div>
      <button class="chip" id="btn-settings">⚙️</button>
    </div>
    <button class="sos-btn" id="btn-sos" title="SOS">SOS</button>
    <button class="fab-me" id="btn-me" title="Meu local">◎</button>
    <div class="sheet" id="sheet">
      <div class="handle"></div>
      <div class="sheet-head">
        <div>
          <div class="sheet-title" id="sheet-title">Círculo</div>
          <div class="sheet-sub" id="sheet-sub">Atualizando…</div>
        </div>
        <button class="btn ghost sm" id="btn-refresh">Atualizar</button>
      </div>
      <div id="member-list" class="member-list"></div>
      <div class="sheet-actions">
        <button class="btn secondary" id="btn-history">Histórico 24h</button>
        <button class="btn secondary" id="btn-profile">Minha foto</button>
      </div>
      <input type="file" id="photo-input" accept="image/*" capture="user" hidden/>
    </div>
  </section>

  <!-- Places -->
  <section id="screen-places" class="screen sheet-screen hidden">
    <header class="page-header">
      <button class="btn ghost" id="btn-places-back">←</button>
      <h2>Lugares</h2>
      <button class="btn ghost" id="btn-place-add">+</button>
    </header>
    <div id="places-list" class="list"></div>
    <p class="hint pad">Casa / Trabalho com raio. Você recebe aviso de chegada/saída do outro.</p>
  </section>

  <!-- History -->
  <section id="screen-history" class="screen sheet-screen hidden">
    <header class="page-header">
      <button class="btn ghost" id="btn-hist-back">←</button>
      <h2>Histórico 24h</h2>
      <span></span>
    </header>
    <div id="hist-map" style="height:40vh"></div>
    <div id="hist-list" class="list"></div>
  </section>

  <!-- Settings -->
  <section id="screen-settings" class="screen sheet-screen hidden">
    <header class="page-header">
      <button class="btn ghost" id="btn-set-back">←</button>
      <h2>Ajustes</h2>
      <span></span>
    </header>
    <div class="pad form">
      <label>Nome</label>
      <input id="set-nome"/>
      <label>Nova senha do casal (vazio = manter)</label>
      <input id="set-senha" type="password" placeholder="••••"/>
      <label>URL do servidor</label>
      <input id="set-url"/>
      <button class="btn primary block" id="btn-save-set">Salvar</button>
      <p class="hint">O SOS só avisa o outro no app — não liga nem manda SMS sozinho.</p>
      <p class="hint">Mapas: © OpenStreetMap. Uso intenso pode exigir outro provedor de tiles.</p>
    </div>
  </section>

  <!-- Toast / SOS modal -->
  <div id="toast" class="toast hidden"></div>
  <div id="modal" class="modal hidden">
    <div class="modal-card">
      <h3 id="modal-title">SOS</h3>
      <p id="modal-body"></p>
      <button class="btn primary block" id="modal-ok">OK</button>
    </div>
  </div>
</div>
<script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
<script src="/js/app.js"></script>
</body>
</html>
`, 'utf8');
  ASSETS['/index.html'].body = ASSETS['/'].body;
  ASSETS['/css/app.css'].body = Buffer.from(`:root {
  --bg: #0B1F33; --card: #12283F; --sheet: #FFFFFF; --text: #0F172A;
  --muted: #64748B; --green: #16A34A; --blue: #2563EB; --red: #DC2626;
  --line: #E2E8F0; --safe-bottom: env(safe-area-inset-bottom, 0px);
}
* { box-sizing: border-box; -webkit-tap-highlight-color: transparent; }
html, body { margin: 0; height: 100%; font-family: system-ui, -apple-system, Segoe UI, Roboto, sans-serif; background: var(--bg); color: var(--text); }
.hidden { display: none !important; }
.screen { position: fixed; inset: 0; }
#map, #hist-map { width: 100%; height: 100%; background: #cfd8e3; }
.leaflet-control-attribution { font-size: 10px !important; }
.setup-card { max-width: 420px; margin: 8vh auto; background: var(--sheet); border-radius: 24px; padding: 28px 22px; box-shadow: 0 20px 50px rgba(0,0,0,.35); }
.logo-mark { font-size: 42px; text-align: center; }
.setup-card h1 { text-align: center; margin: 8px 0 4px; color: var(--bg); }
.muted { color: var(--muted); text-align: center; font-size: 14px; }
label { display: block; margin: 14px 0 6px; font-size: 13px; font-weight: 600; color: var(--muted); }
input { width: 100%; padding: 12px 14px; border: 1px solid var(--line); border-radius: 12px; font-size: 16px; background: #F8FAFC; }
.row { display: flex; gap: 8px; align-items: center; }
.hint { font-size: 12px; color: var(--muted); margin-top: 10px; }
.pad { padding: 16px; }
.btn { border: 0; border-radius: 999px; padding: 12px 18px; font-weight: 700; font-size: 15px; cursor: pointer; background: #E2E8F0; color: var(--text); }
.btn.primary { background: var(--blue); color: #fff; }
.btn.secondary { background: #EFF6FF; color: var(--blue); flex: 1; }
.btn.ghost { background: transparent; color: var(--blue); }
.btn.block { width: 100%; margin-top: 18px; }
.btn.sm { padding: 8px 12px; font-size: 13px; }
.top-bar { position: absolute; top: calc(10px + env(safe-area-inset-top, 0px)); left: 12px; right: 12px; display: flex; justify-content: space-between; align-items: center; z-index: 500; pointer-events: none; }
.top-bar > * { pointer-events: auto; }
.brand-pill { background: rgba(255,255,255,.92); backdrop-filter: blur(8px); padding: 8px 14px; border-radius: 999px; font-weight: 800; font-size: 13px; color: var(--bg); box-shadow: 0 4px 16px rgba(0,0,0,.12); }
.chip { background: #fff; border: 0; border-radius: 999px; padding: 10px 14px; font-weight: 700; box-shadow: 0 4px 16px rgba(0,0,0,.12); font-size: 13px; }
.sos-btn { position: absolute; right: 16px; bottom: calc(290px + var(--safe-bottom)); z-index: 500; width: 64px; height: 64px; border-radius: 50%; border: 0; background: var(--red); color: #fff; font-weight: 900; font-size: 16px; box-shadow: 0 8px 24px rgba(220,38,38,.45); }
.fab-me { position: absolute; right: 22px; bottom: calc(220px + var(--safe-bottom)); z-index: 500; width: 44px; height: 44px; border-radius: 50%; border: 0; background: #fff; font-size: 18px; box-shadow: 0 4px 14px rgba(0,0,0,.18); }
.sheet { position: absolute; left: 0; right: 0; bottom: 0; z-index: 600; background: var(--sheet); border-radius: 24px 24px 0 0; padding: 8px 16px calc(16px + var(--safe-bottom)); box-shadow: 0 -8px 30px rgba(0,0,0,.18); max-height: 48vh; overflow: auto; }
.handle { width: 40px; height: 4px; background: #CBD5E1; border-radius: 99px; margin: 6px auto 12px; }
.sheet-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.sheet-title { font-size: 20px; font-weight: 800; }
.sheet-sub { font-size: 12px; color: var(--muted); }
.member-list { display: flex; flex-direction: column; gap: 8px; }
.member { display: flex; align-items: center; gap: 12px; padding: 10px; border-radius: 16px; background: #F8FAFC; border: 1px solid var(--line); cursor: pointer; }
.avatar { width: 48px; height: 48px; border-radius: 50%; object-fit: cover; background: #CBD5E1; border: 3px solid var(--blue); flex-shrink: 0; }
.avatar.self { border-color: var(--green); }
.avatar.offline { filter: grayscale(.4); opacity: .75; }
.member .meta { flex: 1; min-width: 0; }
.member .name { font-weight: 700; }
.member .status { font-size: 12px; color: var(--muted); }
.dot { width: 8px; height: 8px; border-radius: 50%; display: inline-block; margin-right: 4px; }
.dot.on { background: var(--green); } .dot.off { background: #94A3B8; }
.sheet-actions { display: flex; gap: 8px; margin-top: 12px; }
.sheet-screen { background: #F1F5F9; overflow: auto; }
.page-header { display: flex; align-items: center; justify-content: space-between; padding: calc(12px + env(safe-area-inset-top, 0px)) 12px 12px; background: #fff; border-bottom: 1px solid var(--line); position: sticky; top: 0; z-index: 10; }
.page-header h2 { margin: 0; font-size: 18px; }
.list { padding: 12px; display: flex; flex-direction: column; gap: 8px; }
.list-item { background: #fff; border-radius: 14px; padding: 14px; border: 1px solid var(--line); display: flex; justify-content: space-between; align-items: center; gap: 8px; }
.toast { position: fixed; left: 50%; bottom: 24px; transform: translateX(-50%); background: #0F172A; color: #fff; padding: 12px 18px; border-radius: 999px; z-index: 999; font-size: 14px; max-width: 90vw; }
.modal { position: fixed; inset: 0; background: rgba(15,23,42,.55); z-index: 1000; display: flex; align-items: center; justify-content: center; padding: 24px; }
.modal-card { background: #fff; border-radius: 20px; padding: 22px; width: 100%; max-width: 360px; }
.modal-card h3 { margin: 0 0 8px; color: var(--red); }
.pin-wrap { background: transparent; border: 0; }
.pin-avatar { width: 44px; height: 44px; border-radius: 50%; border: 3px solid var(--blue); object-fit: cover; box-shadow: 0 2px 8px rgba(0,0,0,.3); background: #fff; }
.pin-avatar.self { border-color: var(--green); }
`, 'utf8');
  ASSETS['/js/app.js'].body = Buffer.from(`/* CompartilhaLocal v2 — PWA (Leaflet + OSM). Marca própria. */
(function () {
  const APP_SALT = 'CompartilhaLocal.v1.casal.salt.9f3a';
  const DEFAULT_TOKEN = 'zpb4PDuTiEH_4W2vus5aKaRhlfRXH1YZ'; // mesmo build-secret; sobrescrevível via Android bridge
  const LS = {
    get(k, d) { try { const v = localStorage.getItem(k); return v == null ? d : JSON.parse(v); } catch { return d; } },
    set(k, v) { localStorage.setItem(k, JSON.stringify(v)); },
  };

  const state = {
    url: LS.get('url', 'http://10.0.2.2:8080'),
    nome: LS.get('nome', ''),
    senhaHash: LS.get('grupo', ''),
    id: LS.get('id', null),
    token: (window.CL_BRIDGE && typeof window.CL_BRIDGE.token === 'function') ? window.CL_BRIDGE.token() : DEFAULT_TOKEN,
    map: null, histMap: null,
    markers: {}, circles: {},
    me: null, members: [],
    places: [], lastEventTs: 0,
  };
  if (!state.id) { state.id = crypto.randomUUID(); LS.set('id', state.id); }

  async function sha256(text) {
    const buf = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(text));
    return [...new Uint8Array(buf)].map(b => b.toString(16).padStart(2, '0')).join('');
  }

  function $(id) { return document.getElementById(id); }
  function show(id) {
    document.querySelectorAll('.screen').forEach(s => s.classList.add('hidden'));
    $(id).classList.remove('hidden');
  }
  function toast(msg) {
    const t = $('toast'); t.textContent = msg; t.classList.remove('hidden');
    setTimeout(() => t.classList.add('hidden'), 2800);
  }
  function modal(title, body) {
    $('modal-title').textContent = title;
    $('modal-body').textContent = body;
    $('modal').classList.remove('hidden');
  }
  $('modal-ok').onclick = () => $('modal').classList.add('hidden');

  function apiUrl(path, qs) {
    const u = new URL(path, state.url.replace(/\\/$/, '') + '/');
    if (qs) Object.entries(qs).forEach(([k, v]) => u.searchParams.set(k, v));
    return u.toString();
  }
  async function api(path, opts = {}) {
    const headers = Object.assign({ 'X-CL-Token': state.token }, opts.headers || {});
    if (opts.body && !headers['Content-Type']) headers['Content-Type'] = 'application/json';
    const res = await fetch(apiUrl(path, opts.qs), { method: opts.method || 'GET', headers, body: opts.body ? JSON.stringify(opts.body) : undefined });
    const data = await res.json().catch(() => ({}));
    if (!res.ok) throw new Error(data.erro || ('HTTP ' + res.status));
    return data;
  }

  function idadeTxt(s) {
    if (s < 60) return 'agora';
    if (s < 3600) return 'há ' + Math.floor(s / 60) + ' min';
    return 'há ' + Math.floor(s / 3600) + ' h';
  }

  function avatarHtml(m, self) {
    const cls = 'avatar' + (self ? ' self' : '') + (m.online ? '' : ' offline');
    if (m.photoUrl) return \`<img class="\${cls}" src="\${state.url.replace(/\\/$/, '')}\${m.photoUrl}" alt=""/>\`;
    const initials = (m.nome || '?').slice(0, 1).toUpperCase();
    return \`<div class="\${cls}" style="display:flex;align-items:center;justify-content:center;font-weight:800;color:#fff;background:\${self ? '#16A34A' : '#2563EB'}">\${initials}</div>\`;
  }

  function pinIcon(m, self) {
    const border = self ? '#16A34A' : '#2563EB';
    const src = m.photoUrl
      ? (state.url.replace(/\\/$/, '') + m.photoUrl)
      : \`data:image/svg+xml,\${encodeURIComponent(\`<svg xmlns="http://www.w3.org/2000/svg" width="88" height="88"><circle cx="44" cy="44" r="40" fill="\${border}"/><text x="44" y="52" text-anchor="middle" fill="white" font-size="36" font-family="sans-serif">\${(m.nome||'?')[0]}</text></svg>\`)}\`;
    return L.divIcon({
      className: 'pin-wrap',
      html: \`<img class="pin-avatar\${self ? ' self' : ''}" style="border-color:\${border}" src="\${src}"/>\`,
      iconSize: [44, 44], iconAnchor: [22, 22],
    });
  }

  function ensureMap() {
    if (state.map) { state.map.invalidateSize(); return; }
    state.map = L.map('map', { zoomControl: false, attributionControl: true })
      .setView([-23.5505, -46.6333], 14);
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
    }).addTo(state.map);
  }

  function upsertMember(m) {
    const self = m.id === state.id;
    const key = m.id;
    const icon = pinIcon(m, self);
    if (state.markers[key]) {
      state.markers[key].setLatLng([m.lat, m.lon]).setIcon(icon);
    } else {
      state.markers[key] = L.marker([m.lat, m.lon], { icon }).addTo(state.map)
        .bindPopup(\`<b>\${m.nome}</b><br>\${idadeTxt(m.idade_s)}\`);
    }
    const acc = Math.max(20, Number(m.precisao) || 40);
    if (state.circles[key]) {
      state.circles[key].setLatLng([m.lat, m.lon]).setRadius(acc);
    } else {
      state.circles[key] = L.circle([m.lat, m.lon], {
        radius: acc, color: self ? '#16A34A' : '#2563EB',
        fillColor: self ? '#16A34A' : '#2563EB', fillOpacity: 0.12, weight: 1,
      }).addTo(state.map);
    }
  }

  function renderList(members) {
    const box = $('member-list');
    box.innerHTML = '';
    if (!members.length) {
      box.innerHTML = '<p class="hint">Ninguém online ainda. Peça para a outra pessoa usar a mesma senha.</p>';
      return;
    }
    members.forEach(m => {
      const self = m.id === state.id;
      const el = document.createElement('div');
      el.className = 'member';
      el.innerHTML = \`
        \${avatarHtml(m, self)}
        <div class="meta">
          <div class="name">\${m.nome}\${self ? ' (você)' : ''}</div>
          <div class="status"><span class="dot \${m.online ? 'on' : 'off'}"></span>\${m.online ? 'Online' : 'Offline'} · \${idadeTxt(m.idade_s)}\${m.battery != null ? ' · 🔋 ' + Math.round(m.battery) + '%' : ''}</div>
        </div>\`;
      el.onclick = () => { state.map.setView([m.lat, m.lon], 16); state.markers[m.id]?.openPopup(); };
      box.appendChild(el);
    });
  }

  async function refreshMembers() {
    if (!state.senhaHash) return;
    try {
      const data = await api('/api/locais', { qs: { grupo: state.senhaHash } });
      state.members = data.locais || [];
      const alive = new Set(state.members.map(m => m.id));
      Object.keys(state.markers).forEach(id => {
        if (!alive.has(id)) {
          state.map.removeLayer(state.markers[id]); delete state.markers[id];
          if (state.circles[id]) { state.map.removeLayer(state.circles[id]); delete state.circles[id]; }
        }
      });
      state.members.forEach(upsertMember);
      renderList(state.members);
      $('sheet-sub').textContent = state.members.length + ' pessoa(s) · atualizado agora';
      // notify Android bridge of config for background GPS
      if (window.CL_BRIDGE && window.CL_BRIDGE.onConfig) {
        window.CL_BRIDGE.onConfig(JSON.stringify({
          url: state.url, nome: state.nome, grupo: state.senhaHash, id: state.id, token: state.token,
        }));
      }
    } catch (e) {
      $('sheet-sub').textContent = 'Falha: ' + e.message;
    }
  }

  async function postLocation(lat, lon, precisao, battery) {
    if (!state.senhaHash) return;
    try {
      await api('/api/local', {
        method: 'POST',
        body: {
          nome: state.nome, id: state.id, grupo: state.senhaHash,
          lat, lon, precisao, battery, hora: new Date().toISOString(),
        },
      });
    } catch (e) { console.warn(e); }
  }

  async function pollEvents() {
    if (!state.senhaHash) return;
    try {
      const data = await api('/api/eventos', { qs: { grupo: state.senhaHash, id: state.id, since: String(state.lastEventTs) } });
      (data.eventos || []).forEach(ev => {
        state.lastEventTs = Math.max(state.lastEventTs, ev.ts);
        if (ev.tipo === 'sos') {
          modal('🚨 SOS de ' + ev.fromNome, ev.msg || 'Precisa de ajuda!');
          if (window.CL_BRIDGE && window.CL_BRIDGE.notify) window.CL_BRIDGE.notify('SOS', ev.fromNome + ': ' + (ev.msg || ''));
        } else if (ev.tipo === 'chegada' || ev.tipo === 'saida') {
          const t = ev.tipo === 'chegada' ? 'chegou em' : 'saiu de';
          toast(ev.fromNome + ' ' + t + ' ' + ev.placeName);
          if (window.CL_BRIDGE && window.CL_BRIDGE.notify) window.CL_BRIDGE.notify('Lugar', ev.fromNome + ' ' + t + ' ' + ev.placeName);
        }
        api('/api/eventos/lido', { method: 'POST', body: { eventoId: ev.id, id: state.id } }).catch(() => {});
      });
    } catch (_) {}
  }

  // Geolocation watch (browser / PWA)
  let watchId = null;
  function startGeo() {
    if (!navigator.geolocation) return;
    if (watchId != null) return;
    watchId = navigator.geolocation.watchPosition(pos => {
      const { latitude: lat, longitude: lon, accuracy } = pos.coords;
      state.me = { lat, lon, precisao: accuracy };
      postLocation(lat, lon, accuracy, null);
      upsertMember({
        id: state.id, nome: state.nome, lat, lon, precisao: accuracy,
        idade_s: 0, online: true, photoUrl: null,
      });
    }, err => console.warn(err), { enableHighAccuracy: true, maximumAge: 30000, timeout: 20000 });
  }

  // Called by Android WebView with GPS fixes
  window.CL_onLocation = function (lat, lon, acc, battery) {
    postLocation(lat, lon, acc, battery);
    upsertMember({
      id: state.id, nome: state.nome, lat, lon, precisao: acc,
      idade_s: 0, online: true, battery, photoUrl: null,
    });
  };

  function enterApp() {
    show('screen-map');
    ensureMap();
    setTimeout(() => state.map.invalidateSize(), 200);
    refreshMembers();
    startGeo();
    setInterval(refreshMembers, 60000);
    setInterval(pollEvents, 20000);
    $('sheet-title').textContent = state.nome || 'Círculo';
  }

  // Setup
  $('setup-url').value = state.url;
  $('setup-nome').value = state.nome;
  if (state.nome && state.senhaHash && state.url) {
    // already configured
  }
  $('btn-toggle-senha').onclick = () => {
    const i = $('setup-senha');
    i.type = i.type === 'password' ? 'text' : 'password';
    $('btn-toggle-senha').textContent = i.type === 'password' ? 'Mostrar' : 'Ocultar';
  };
  $('btn-setup').onclick = async () => {
    const nome = $('setup-nome').value.trim();
    const senha = $('setup-senha').value;
    const url = $('setup-url').value.trim().replace(/\\/$/, '');
    if (!nome) return toast('Digite seu nome');
    if (senha.length < 4) return toast('Senha com pelo menos 4 caracteres');
    if (!url) return toast('Informe a URL do servidor');
    state.nome = nome;
    state.url = url;
    state.senhaHash = await sha256(senha + APP_SALT);
    LS.set('nome', nome); LS.set('url', url); LS.set('grupo', state.senhaHash);
    try {
      await api('/api/health');
    } catch (e) {
      toast('Não conectou: ' + e.message + ' — salvei mesmo assim');
    }
    enterApp();
  };

  if (state.nome && state.senhaHash && state.url) {
    enterApp();
  } else {
    show('screen-setup');
  }

  $('btn-refresh').onclick = () => refreshMembers();
  $('btn-me').onclick = () => {
    if (state.me) state.map.setView([state.me.lat, state.me.lon], 16);
    else if (state.markers[state.id]) state.map.setView(state.markers[state.id].getLatLng(), 16);
  };
  $('btn-sos').onclick = async () => {
    if (!confirm('Enviar SOS para o outro aparelho? (não liga / não manda SMS)')) return;
    try {
      const lat = state.me?.lat, lon = state.me?.lon;
      await api('/api/sos', { method: 'POST', body: { grupo: state.senhaHash, id: state.id, nome: state.nome, lat, lon, msg: 'Preciso de ajuda!' } });
      toast('SOS enviado');
    } catch (e) { toast(e.message); }
  };

  // Places
  $('btn-places').onclick = async () => {
    show('screen-places');
    await loadPlaces();
  };
  $('btn-places-back').onclick = () => { show('screen-map'); setTimeout(() => state.map.invalidateSize(), 100); };
  async function loadPlaces() {
    try {
      const data = await api('/api/places', { qs: { grupo: state.senhaHash } });
      state.places = data.places || [];
      const box = $('places-list');
      box.innerHTML = '';
      if (!state.places.length) box.innerHTML = '<p class="hint pad">Nenhum lugar ainda.</p>';
      state.places.forEach(pl => {
        const el = document.createElement('div');
        el.className = 'list-item';
        el.innerHTML = \`<div><b>\${pl.name}</b><div class="hint">Raio \${pl.radius} m</div></div><button class="btn ghost sm">Apagar</button>\`;
        el.querySelector('button').onclick = async () => {
          await api('/api/places', { method: 'DELETE', body: { grupo: state.senhaHash, id: pl.id } });
          loadPlaces();
        };
        box.appendChild(el);
      });
    } catch (e) { toast(e.message); }
  }
  $('btn-place-add').onclick = async () => {
    const name = prompt('Nome do lugar (ex: Casa, Trabalho)');
    if (!name) return;
    const radius = Number(prompt('Raio em metros', '100') || 100);
    let lat = state.me?.lat, lon = state.me?.lon;
    if (lat == null && state.markers[state.id]) {
      const ll = state.markers[state.id].getLatLng(); lat = ll.lat; lon = ll.lng;
    }
    if (lat == null) return toast('Sem localização atual');
    try {
      await api('/api/places', { method: 'POST', body: { grupo: state.senhaHash, name, lat, lon, radius, ownerId: state.id } });
      toast('Lugar salvo');
      loadPlaces();
    } catch (e) { toast(e.message); }
  };

  // History
  $('btn-history').onclick = async () => {
    show('screen-history');
    if (!state.histMap) {
      state.histMap = L.map('hist-map', { zoomControl: false }).setView([-23.55, -46.63], 13);
      L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '&copy; OpenStreetMap',
      }).addTo(state.histMap);
    }
    setTimeout(() => state.histMap.invalidateSize(), 200);
    try {
      const data = await api('/api/historico', { qs: { grupo: state.senhaHash } });
      const pts = data.pontos || [];
      const box = $('hist-list');
      box.innerHTML = '';
      const byId = {};
      pts.forEach(p => { (byId[p.id] = byId[p.id] || []).push(p); });
      Object.values(state.histMap._layers).forEach(l => {
        if (l instanceof L.Polyline || l instanceof L.CircleMarker) state.histMap.removeLayer(l);
      });
      Object.entries(byId).forEach(([id, arr]) => {
        arr.sort((a, b) => a.ts - b.ts);
        const latlngs = arr.map(p => [p.lat, p.lon]);
        if (latlngs.length) {
          L.polyline(latlngs, { color: id === state.id ? '#16A34A' : '#2563EB', weight: 3 }).addTo(state.histMap);
          state.histMap.fitBounds(L.latLngBounds(latlngs), { padding: [20, 20] });
        }
        const last = arr[arr.length - 1];
        const el = document.createElement('div');
        el.className = 'list-item';
        el.innerHTML = \`<div><b>\${last.nome}</b><div class="hint">\${arr.length} pontos · último \${new Date(last.ts).toLocaleString()}</div></div>\`;
        box.appendChild(el);
      });
      if (!pts.length) box.innerHTML = '<p class="hint pad">Sem histórico nas últimas 24h.</p>';
    } catch (e) { toast(e.message); }
  };
  $('btn-hist-back').onclick = () => { show('screen-map'); setTimeout(() => state.map.invalidateSize(), 100); };

  // Profile photo
  $('btn-profile').onclick = () => $('photo-input').click();
  $('photo-input').onchange = async (ev) => {
    const file = ev.target.files?.[0];
    if (!file) return;
    if (file.size > 700000) return toast('Foto grande demais (máx ~600KB). Tire outra mais leve.');
    const reader = new FileReader();
    reader.onload = async () => {
      try {
        const data = await api('/api/foto', {
          method: 'POST',
          body: { id: state.id, nome: state.nome, photoBase64: reader.result },
        });
        toast('Foto atualizada');
        refreshMembers();
      } catch (e) { toast(e.message); }
    };
    reader.readAsDataURL(file);
  };

  // Settings
  $('btn-settings').onclick = () => {
    $('set-nome').value = state.nome;
    $('set-url').value = state.url;
    $('set-senha').value = '';
    show('screen-settings');
  };
  $('btn-set-back').onclick = () => { show('screen-map'); setTimeout(() => state.map.invalidateSize(), 100); };
  $('btn-save-set').onclick = async () => {
    state.nome = $('set-nome').value.trim() || state.nome;
    state.url = $('set-url').value.trim().replace(/\\/$/, '') || state.url;
    const s = $('set-senha').value;
    if (s) {
      if (s.length < 4) return toast('Senha curta');
      state.senhaHash = await sha256(s + APP_SALT);
      LS.set('grupo', state.senhaHash);
    }
    LS.set('nome', state.nome); LS.set('url', state.url);
    toast('Salvo');
    show('screen-map');
    setTimeout(() => state.map.invalidateSize(), 100);
    refreshMembers();
  };
})();
`, 'utf8');
  ASSETS['/manifest.webmanifest'].body = Buffer.from(`{
  "name": "CompartilhaLocal",
  "short_name": "CompartilhaLocal",
  "start_url": "/",
  "display": "standalone",
  "background_color": "#0B1F33",
  "theme_color": "#0B1F33",
  "icons": [
    { "src": "/icons/icon-192.png", "sizes": "192x192", "type": "image/png" },
    { "src": "/icons/icon-512.png", "sizes": "512x512", "type": "image/png" }
  ]
}
`, 'utf8');
  ASSETS['/icons/icon-192.png'].body = Buffer.from("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", 'base64');
  ASSETS['/icons/icon-512.png'].body = Buffer.from("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", 'base64');
})();

for (const d of [DATA, path.join(DATA, 'photos')]) {
  if (!fs.existsSync(d)) fs.mkdirSync(d, { recursive: true });
}

function load(name, fallback) {
  const f = path.join(DATA, name);
  try { return JSON.parse(fs.readFileSync(f, 'utf8')); } catch { return fallback; }
}
function save(name, obj) {
  fs.writeFileSync(path.join(DATA, name), JSON.stringify(obj), 'utf8');
}

let latest = load('latest.json', {});
let history = load('history.json', []);
let places = load('places.json', {});
let events = load('events.json', []);
let profiles = load('profiles.json', {});

function now() { return Date.now(); }
function okAuth(req, url) {
  if (!SECRET) return true;
  const h = req.headers['x-cl-token'] || '';
  const q = url.searchParams.get('token') || '';
  const tok = h || q;
  try {
    return tok.length === SECRET.length &&
      crypto.timingSafeEqual(Buffer.from(tok), Buffer.from(SECRET));
  } catch { return false; }
}
function readBody(req) {
  return new Promise((resolve, reject) => {
    const chunks = [];
    let size = 0;
    req.on('data', c => {
      size += c.length;
      if (size > 2_000_000) { reject(new Error('body too large')); req.destroy(); return; }
      chunks.push(c);
    });
    req.on('end', () => resolve(Buffer.concat(chunks).toString('utf8')));
    req.on('error', reject);
  });
}
function send(res, code, obj, extraHeaders) {
  const body = typeof obj === 'string' ? obj : JSON.stringify(obj);
  const headers = Object.assign({
    'Content-Type': typeof obj === 'string' ? 'text/plain; charset=utf-8' : 'application/json; charset=utf-8',
    'Access-Control-Allow-Origin': '*',
    'Access-Control-Allow-Headers': 'Content-Type, X-CL-Token',
    'Access-Control-Allow-Methods': 'GET, POST, OPTIONS, PUT, DELETE',
    'Content-Length': Buffer.byteLength(body),
  }, extraHeaders || {});
  res.writeHead(code, headers);
  res.end(body);
}
function serveAsset(res, pathname) {
  const a = ASSETS[pathname] || (pathname.endsWith('/') ? ASSETS['/'] : null);
  if (!a || !a.body) return false;
  res.writeHead(200, {
    'Content-Type': a.type,
    'Content-Length': a.body.length,
    'Cache-Control': pathname === '/' || pathname === '/index.html' ? 'no-cache' : 'public, max-age=3600',
  });
  res.end(a.body);
  return true;
}
function haversineM(aLat, aLon, bLat, bLon) {
  const R = 6371000, toR = d => d * Math.PI / 180;
  const dLat = toR(bLat - aLat), dLon = toR(bLon - aLon);
  const x = Math.sin(dLat / 2) ** 2 +
    Math.cos(toR(aLat)) * Math.cos(toR(bLat)) * Math.sin(dLon / 2) ** 2;
  return 2 * R * Math.asin(Math.sqrt(x));
}
function checkGeofences(grupo, deviceId, nome, lat, lon) {
  const list = places[grupo] || [];
  if (!list.length) return;
  const stateKey = `_geo_${grupo}|${deviceId}`;
  if (!globalThis.__geoState) globalThis.__geoState = {};
  const prev = globalThis.__geoState[stateKey] || {};
  const next = {};
  for (const pl of list) {
    const dist = haversineM(lat, lon, pl.lat, pl.lon);
    const inside = dist <= (pl.radius || 100);
    next[pl.id] = inside;
    const was = !!prev[pl.id];
    if (inside && !was) {
      events.push({
        id: crypto.randomUUID(), tipo: 'chegada', grupo, fromId: deviceId, fromNome: nome,
        placeId: pl.id, placeName: pl.name, ts: now(), lidoPor: [],
      });
    } else if (!inside && was) {
      events.push({
        id: crypto.randomUUID(), tipo: 'saida', grupo, fromId: deviceId, fromNome: nome,
        placeId: pl.id, placeName: pl.name, ts: now(), lidoPor: [],
      });
    }
  }
  globalThis.__geoState[stateKey] = next;
  if (events.length > 500) events = events.slice(-400);
  save('events.json', events);
}

const server = http.createServer(async (req, res) => {
  try {
    const url = new URL(req.url, `http://${req.headers.host || 'localhost'}`);
    const p = url.pathname;

    if (req.method === 'OPTIONS') return send(res, 204, '');

    // Static PWA from memory (no auth)
    if (req.method === 'GET' && !p.startsWith('/api/')) {
      if (p.startsWith('/photos/')) {
        const f = path.join(DATA, 'photos', path.basename(p));
        if (fs.existsSync(f)) {
          const data = fs.readFileSync(f);
          res.writeHead(200, { 'Content-Type': 'image/jpeg', 'Content-Length': data.length });
          return res.end(data);
        }
        return send(res, 404, { ok: false });
      }
      if (serveAsset(res, p)) return;
      return send(res, 404, { ok: false, erro: 'não encontrado' });
    }

    if (!okAuth(req, url)) return send(res, 401, { ok: false, erro: 'não autorizado' });

    if (p === '/api/health' || p === '/api/') {
      return send(res, 200, { ok: true, msg: 'CompartilhaLocal v2', aparelhos: Object.keys(latest).length, auth: !!SECRET });
    }

    if (p === '/api/local' && req.method === 'POST') {
      const raw = await readBody(req);
      const data = JSON.parse(raw || '{}');
      const lat = Number(data.lat), lon = Number(data.lon);
      if (!Number.isFinite(lat) || !Number.isFinite(lon)) return send(res, 400, { ok: false, erro: 'lat/lon' });
      const nome = String(data.nome || 'Sem nome').slice(0, 40);
      const id = String(data.id || nome).slice(0, 64);
      const grupo = String(data.grupo || '').trim();
      if (!grupo) return send(res, 400, { ok: false, erro: 'grupo obrigatório' });
      const battery = data.battery != null ? Number(data.battery) : null;
      const precisao = data.precisao != null ? Number(data.precisao) : null;
      const ts = now();
      const entry = { id, nome, grupo, lat, lon, precisao, battery, hora: data.hora || null, recebido_ms: ts };
      latest[`${grupo}|${id}`] = entry;
      history.push({ id, nome, grupo, lat, lon, precisao, ts });
      const cut = ts - HISTORY_MS;
      history = history.filter(h => h.ts >= cut);
      if (!profiles[id]) profiles[id] = {};
      profiles[id].nome = nome;
      if (battery != null) profiles[id].battery = battery;
      profiles[id].updated = ts;
      save('latest.json', latest);
      save('history.json', history);
      save('profiles.json', profiles);
      try { checkGeofences(grupo, id, nome, lat, lon); } catch (e) { console.error('geo', e); }
      return send(res, 200, { ok: true, salvo: true });
    }

    if (p === '/api/locais' && req.method === 'GET') {
      const grupo = (url.searchParams.get('grupo') || '').trim();
      if (!grupo) return send(res, 400, { ok: false, erro: 'informe ?grupo=' });
      const t = now();
      const itens = Object.values(latest)
        .filter(v => v.grupo === grupo)
        .map(v => {
          const prof = profiles[v.id] || {};
          return {
            id: v.id, nome: v.nome, lat: v.lat, lon: v.lon,
            precisao: v.precisao, battery: v.battery != null ? v.battery : prof.battery,
            hora: v.hora, recebido_ms: v.recebido_ms,
            idade_s: Math.max(0, Math.floor((t - v.recebido_ms) / 1000)),
            online: (t - v.recebido_ms) < ONLINE_MS,
            photoUrl: prof.photo ? `/photos/${path.basename(prof.photo)}` : null,
          };
        })
        .sort((a, b) => a.nome.localeCompare(b.nome));
      return send(res, 200, { ok: true, agora_ms: t, locais: itens });
    }

    if (p === '/api/historico' && req.method === 'GET') {
      const grupo = (url.searchParams.get('grupo') || '').trim();
      const id = url.searchParams.get('id') || '';
      if (!grupo) return send(res, 400, { ok: false, erro: 'grupo' });
      const cut = now() - HISTORY_MS;
      let pts = history.filter(h => h.grupo === grupo && h.ts >= cut);
      if (id) pts = pts.filter(h => h.id === id);
      pts = pts.map(({ id, nome, lat, lon, precisao, ts }) => ({ id, nome, lat, lon, precisao, ts }));
      return send(res, 200, { ok: true, pontos: pts });
    }

    if (p === '/api/sos' && req.method === 'POST') {
      const data = JSON.parse(await readBody(req) || '{}');
      const grupo = String(data.grupo || '').trim();
      const fromId = String(data.id || '');
      const fromNome = String(data.nome || 'Alguém');
      if (!grupo) return send(res, 400, { ok: false, erro: 'grupo' });
      const ev = {
        id: crypto.randomUUID(), tipo: 'sos', grupo, fromId, fromNome,
        lat: data.lat, lon: data.lon, msg: String(data.msg || 'SOS!').slice(0, 200),
        ts: now(), lidoPor: [],
      };
      events.push(ev);
      save('events.json', events);
      return send(res, 200, { ok: true, evento: ev });
    }

    if (p === '/api/eventos' && req.method === 'GET') {
      const grupo = (url.searchParams.get('grupo') || '').trim();
      const me = url.searchParams.get('id') || '';
      if (!grupo) return send(res, 400, { ok: false, erro: 'grupo' });
      const since = Number(url.searchParams.get('since') || 0);
      const list = events.filter(e => e.grupo === grupo && e.ts > since && e.fromId !== me);
      return send(res, 200, { ok: true, eventos: list });
    }

    if (p === '/api/eventos/lido' && req.method === 'POST') {
      const data = JSON.parse(await readBody(req) || '{}');
      const ev = events.find(e => e.id === data.eventoId);
      if (ev && data.id && !ev.lidoPor.includes(data.id)) {
        ev.lidoPor.push(data.id);
        save('events.json', events);
      }
      return send(res, 200, { ok: true });
    }

    if (p === '/api/places' && req.method === 'GET') {
      const grupo = (url.searchParams.get('grupo') || '').trim();
      return send(res, 200, { ok: true, places: places[grupo] || [] });
    }

    if (p === '/api/places' && req.method === 'POST') {
      const data = JSON.parse(await readBody(req) || '{}');
      const grupo = String(data.grupo || '').trim();
      if (!grupo) return send(res, 400, { ok: false, erro: 'grupo' });
      if (!places[grupo]) places[grupo] = [];
      const pl = {
        id: data.id || crypto.randomUUID(),
        name: String(data.name || 'Lugar').slice(0, 40),
        lat: Number(data.lat), lon: Number(data.lon),
        radius: Math.max(30, Number(data.radius) || 100),
        ownerId: String(data.ownerId || ''),
      };
      const i = places[grupo].findIndex(x => x.id === pl.id);
      if (i >= 0) places[grupo][i] = pl; else places[grupo].push(pl);
      save('places.json', places);
      return send(res, 200, { ok: true, place: pl });
    }

    if (p === '/api/places' && req.method === 'DELETE') {
      const data = JSON.parse(await readBody(req) || '{}');
      const grupo = String(data.grupo || '').trim();
      places[grupo] = (places[grupo] || []).filter(x => x.id !== data.id);
      save('places.json', places);
      return send(res, 200, { ok: true });
    }

    if (p === '/api/foto' && req.method === 'POST') {
      const data = JSON.parse(await readBody(req) || '{}');
      const id = String(data.id || '').slice(0, 64);
      let b64 = String(data.photoBase64 || '');
      if (!id || !b64) return send(res, 400, { ok: false, erro: 'id/photo' });
      b64 = b64.replace(/^data:image\/\w+;base64,/, '');
      if (b64.length > MAX_PHOTO_B64) return send(res, 400, { ok: false, erro: 'foto muito grande (máx ~600KB)' });
      const buf = Buffer.from(b64, 'base64');
      const fname = `${id.replace(/[^a-zA-Z0-9_-]/g, '')}.jpg`;
      const fpath = path.join(DATA, 'photos', fname);
      fs.writeFileSync(fpath, buf);
      if (!profiles[id]) profiles[id] = {};
      profiles[id].photo = fpath;
      if (data.nome) profiles[id].nome = String(data.nome).slice(0, 40);
      save('profiles.json', profiles);
      return send(res, 200, { ok: true, photoUrl: `/photos/${fname}` });
    }

    send(res, 404, { ok: false, erro: 'não encontrado' });
  } catch (e) {
    console.error(e);
    send(res, 500, { ok: false, erro: String(e.message || e) });
  }
});

server.listen(PORT, '0.0.0.0', () => {
  console.log(`CompartilhaLocal v2 (embedded) http://0.0.0.0:${PORT} SECRET=${SECRET ? 'sim' : 'NÃO'}`);
});
