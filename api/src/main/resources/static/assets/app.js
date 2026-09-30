const $ = id => document.getElementById(id);
const number = new Intl.NumberFormat('es-PE', { maximumFractionDigits: 3 });
let token = sessionStorage.getItem('cacaoselva.token');
let claims, page = 0, catalog = [], editing, deleting, registering = false;
let generation = 0, reader, saving = false, debounce;

function message(text, error = false) {
  $('message').textContent = text; $('message').hidden = !text;
  $('message').classList.toggle('error', error);
}
function formError(id, text = '') { $(id).textContent = text; $(id).hidden = !text; }
function session() {
  try {
    claims = JSON.parse(atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')));
    if (claims.exp * 1000 <= Date.now()) throw new Error();
  } catch { token = null; claims = null; sessionStorage.removeItem('cacaoselva.token'); }
  const active = !!claims;
  $('welcome').hidden = active; $('workspace').hidden = !active;
  $('open-login').hidden = active; $('logout').hidden = !active; $('session-name').hidden = !active;
  $('session-name').textContent = claims?.sub || '';
  $('new-lote').hidden = !canWrite(); $('new-socio').hidden = claims?.rol !== 'ADMIN';
}
function canWrite() { return ['ADMIN', 'OPERADOR'].includes(claims?.rol); }
async function api(path, { method = 'GET', body, signal } = {}) {
  let response;
  try {
    response = await fetch(path, { method, signal: signal ? AbortSignal.any([signal, AbortSignal.timeout(65000)]) : AbortSignal.timeout(65000),
      headers: { ...(token ? { Authorization: `Bearer ${token}` } : {}), ...(body ? { 'Content-Type': 'application/json' } : {}) },
      body: body ? JSON.stringify(body) : undefined });
  } catch (error) {
    if (signal?.aborted) throw error;
    throw new Error(method === 'GET' ? 'No se pudo conectar. El servicio puede estar iniciando; vuelve a actualizar.' : 'No se pudo confirmar el resultado. Consulta los registros antes de repetir la operación.');
  }
  if (response.status === 204) return null;
  const data = await response.json().catch(() => ({}));
  if (!response.ok) {
    if (response.status === 401 && !path.startsWith('/auth/')) { logout(); openAuth(false); }
    throw new Error(response.status === 429 ? 'Demasiados intentos. Espera unos minutos antes de volver a intentarlo.' : data.message || data.mensaje || `No se pudo completar la solicitud (${response.status}).`);
  }
  return data;
}
function logout() {
  reader?.abort(); generation++; token = null; sessionStorage.removeItem('cacaoselva.token');
  $('lotes').replaceChildren(); catalog = []; page = 0; session(); message('');
}
function openAuth(register) {
  registering = register; formError('auth-error');
  $('auth-title').textContent = register ? 'Crea tu cuenta' : 'Inicia sesión';
  $('auth-submit').textContent = register ? 'Crear cuenta' : 'Ingresar';
  $('auth-switch').textContent = register ? 'Ya tengo una cuenta' : '¿Primera vez? Crea una cuenta';
  $('password').minLength = register ? 12 : 1;
  $('password').autocomplete = register ? 'new-password' : 'current-password';
  $('password-hint').hidden = !register; $('registration-note').hidden = !register;
  if (!$('auth-dialog').open) $('auth-dialog').showModal();
}
function cell(row, text, className = '') {
  const element = document.createElement('td'); element.textContent = text; element.className = className; row.append(element); return element;
}
function action(parent, text, callback, danger = false) {
  const button = document.createElement('button'); button.textContent = text;
  button.className = `table-action${danger ? ' delete' : ''}`; button.addEventListener('click', callback); parent.append(button);
}
function render(result, pending) {
  $('lotes').replaceChildren();
  for (const lote of result.content) {
    const row = document.createElement('tr');
    cell(row, `#${String(lote.id).padStart(4, '0')}`, 'lote-id');
    cell(row, catalog.find(s => s.id === lote.socioId)?.nombre || `Socio #${lote.socioId}`);
    cell(row, number.format(lote.pesoKg), 'numeric');
    const badge = document.createElement('span'); badge.className = `badge ${lote.estado === 'PENDIENTE' ? 'pending' : 'paid'}`;
    badge.textContent = lote.estado === 'PENDIENTE' ? 'Pendiente' : 'Liquidado'; cell(row, '').append(badge);
    const actions = cell(row, '', 'actions-col');
    if (canWrite()) action(actions, 'Editar', () => openLote(lote));
    if (claims.rol === 'ADMIN') action(actions, 'Eliminar', () => {
      deleting = lote; formError('delete-error'); $('delete-description').textContent = `Se eliminará el lote #${lote.id}.`;
      $('delete-dialog').showModal();
    }, true);
    if (!canWrite()) actions.textContent = 'Solo lectura';
    $('lotes').append(row);
  }
  $('total').textContent = number.format(result.totalElements); $('pending').textContent = number.format(pending.pendientes);
  $('weight').textContent = number.format(result.content.reduce((sum, lote) => sum + lote.pesoKg, 0));
  $('empty').hidden = result.content.length > 0;
  $('page-info').textContent = `Página ${result.totalPages ? result.page + 1 : 0} de ${result.totalPages}`;
  $('previous').disabled = page === 0; $('next').disabled = page + 1 >= result.totalPages;
  $('last-update').textContent = `Actualizado a las ${new Date().toLocaleTimeString('es-PE', { hour: '2-digit', minute: '2-digit' })}`;
}
async function load() {
  if (!token) return;
  reader?.abort(); reader = new AbortController(); const signal = reader.signal; const current = ++generation;
  $('loading').hidden = false; $('previous').disabled = true; $('next').disabled = true;
  const params = new URLSearchParams({ page, size: $('page-size').value });
  if ($('status').value) params.set('estado', $('status').value);
  const search = $('search').value.trim();
  if (search) {
    if (/^\d+$/.test(search)) {
      if (Number(search) < 1 || Number(search) > 2147483647) { $('loading').hidden = true; message('El ID de socio debe estar entre 1 y 2147483647.', true); return; }
      params.set('socioId', search);
    } else params.set('socio', search);
  }
  try {
    const [result, socios, pending] = await Promise.all([api(`/lotes?${params}`, { signal }), api('/socios/catalogo', { signal }), api('/lotes/pendientes/conteo', { signal })]);
    if (current !== generation) return;
    catalog = socios;
    if (page > 0 && page >= result.totalPages) { page = Math.max(0, result.totalPages - 1); return load(); }
    render(result, pending); $('connection').textContent = 'Conectado';
  } catch (error) { if (current === generation && !signal.aborted) { message(error.message, true); $('connection').textContent = 'Sin actualizar'; } }
  finally { if (current === generation) $('loading').hidden = true; }
}
function openLote(lote) {
  editing = lote; $('lote-form').reset(); formError('lote-error');
  $('lote-title').textContent = lote ? `Editar lote #${lote.id}` : 'Nuevo lote';
  $('lote-socio').replaceChildren(new Option('Selecciona un socio', ''));
  catalog.forEach(s => $('lote-socio').add(new Option(s.nombre, s.id)));
  if (lote) { $('lote-socio').value = lote.socioId; $('lote-weight').value = lote.pesoKg; $('lote-status').value = lote.estado; }
  $('lote-dialog').showModal();
}
async function mutate(formId, errorId, task) {
  if (saving) return;
  saving = true; const fieldset = $(formId)?.querySelector('fieldset'); if (fieldset) fieldset.disabled = true;
  $('delete-confirm').disabled = true; formError(errorId);
  try { await task(); } catch (error) { formError(errorId, error.message); }
  finally { saving = false; if (fieldset) fieldset.disabled = false; $('delete-confirm').disabled = false; }
}
$('auth-form').addEventListener('submit', event => {
  event.preventDefault(); mutate('auth-form', 'auth-error', async () => {
    const body = { usuario: $('username').value.trim(), contrasena: $('password').value };
    if (registering) { await api('/auth/register', { method: 'POST', body }); registering = false; message('Cuenta creada. Ya puedes gestionar los lotes.'); }
    const result = await api('/auth/login', { method: 'POST', body }); token = result.token;
    sessionStorage.setItem('cacaoselva.token', token); $('auth-form').reset(); $('auth-dialog').close(); session(); page = 0; await load();
  });
});
$('lote-form').addEventListener('submit', event => {
  event.preventDefault(); mutate('lote-form', 'lote-error', async () => {
    const body = { socioId: Number($('lote-socio').value), pesoKg: Number($('lote-weight').value), estado: $('lote-status').value, ...(editing ? { version: editing.version } : {}) };
    await api(editing ? `/lotes/${editing.id}` : '/lotes', { method: editing ? 'PUT' : 'POST', body });
    $('lote-dialog').close(); message('Lote guardado en el registro compartido.'); await load();
  });
});
$('socio-form').addEventListener('submit', event => {
  event.preventDefault(); const body = Object.fromEntries(new FormData(event.currentTarget));
  mutate('socio-form', 'socio-error', async () => { await api('/socios', { method: 'POST', body }); $('socio-dialog').close(); message('Socio registrado.'); await load(); });
});
$('delete-confirm').addEventListener('click', () => mutate(null, 'delete-error', async () => {
  await api(`/lotes/${deleting.id}`, { method: 'DELETE' }); $('delete-dialog').close(); message('Lote eliminado.'); await load();
}));
document.querySelectorAll('[data-close]').forEach(button => button.addEventListener('click', () => { if (!saving) $(button.dataset.close).close(); }));
document.querySelectorAll('dialog').forEach(dialog => dialog.addEventListener('cancel', event => { if (saving) event.preventDefault(); }));
$('open-login').onclick = $('welcome-login').onclick = () => openAuth(false);
$('open-register').onclick = () => openAuth(true); $('auth-switch').onclick = () => openAuth(!registering);
$('logout').onclick = logout; $('new-lote').onclick = () => openLote(null);
$('new-socio').onclick = () => { $('socio-form').reset(); formError('socio-error'); $('socio-dialog').showModal(); };
$('refresh').onclick = () => { message(''); load(); };
function filter() { clearTimeout(debounce); page = 0; message(''); load(); }
$('filters').onsubmit = event => { event.preventDefault(); filter(); };
$('status').onchange = $('page-size').onchange = filter;
$('search').oninput = () => { clearTimeout(debounce); debounce = setTimeout(filter, 350); };
$('previous').onclick = () => { page--; load(); }; $('next').onclick = () => { page++; load(); };
session(); if (token) load();
