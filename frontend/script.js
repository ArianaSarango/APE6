/**
 * Fleet Monitor — script.js
 * Consume la API REST de Spring Boot y actualiza el DOM cada 5 segundos.
 * API base: http://localhost:8080/api/fleet
 */

const API_BASE = 'http://localhost:8080/api/fleet';
const VEHICLES = ['VH-001', 'VH-002', 'VH-003'];
const REFRESH_MS = 5000;

// ── DOM refs ──────────────────────────────────────
const elTotalVehicles = document.getElementById('total-vehicles');
const elTotalGps      = document.getElementById('total-gps');
const elActiveAlerts  = document.getElementById('active-alerts');
const elLastUpdated   = document.getElementById('last-updated');
const elConnectionBadge = document.getElementById('connection-badge');
const elVehicleGrid   = document.getElementById('vehicle-grid');
const elAlertsContainer = document.getElementById('alerts-container');

// ── Helpers ───────────────────────────────────────

function setOnline(ok) {
  if (ok) {
    elConnectionBadge.textContent = '● En línea';
    elConnectionBadge.className = 'badge badge--online';
  } else {
    elConnectionBadge.textContent = '● Sin conexión';
    elConnectionBadge.className = 'badge badge--offline';
  }
}

function formatTime(isoStr) {
  if (!isoStr) return '—';
  const d = new Date(isoStr);
  return isNaN(d) ? isoStr : d.toLocaleTimeString('es-EC');
}

function formatCoord(val) {
  return val != null ? Number(val).toFixed(6) : '—';
}

function formatSpeed(val) {
  return val != null ? `${Number(val).toFixed(1)} km/h` : '—';
}

async function fetchJSON(url) {
  const res = await fetch(url);
  if (!res.ok) throw new Error(`HTTP ${res.status}`);
  return res.json();
}

// ── 1. Estado General ─────────────────────────────
async function updateStatus() {
  try {
    const data = await fetchJSON(`${API_BASE}/status`);
    elTotalVehicles.textContent = data.totalVehicles ?? '—';
    elTotalGps.textContent      = data.totalGpsRecords ?? '—';
    elActiveAlerts.textContent  = data.activeAlerts ?? '—';

    // Resaltar si hay alertas
    const alertCard = document.getElementById('stat-alerts');
    alertCard.style.borderColor = data.activeAlerts > 0 ? 'var(--danger)' : '';
    setOnline(true);
  } catch (e) {
    console.error('[Status]', e);
    setOnline(false);
  }
}

// ── 2. Telemetría GPS por vehículo ────────────────
async function updateVehicles() {
  const cards = await Promise.all(VEHICLES.map(async (id) => {
    try {
      const records = await fetchJSON(`${API_BASE}/vehicle/${id}/telemetria`);
      const last = records[0] ?? null;
      return buildVehicleCard(id, last);
    } catch {
      return buildVehicleCard(id, null, true);
    }
  }));

  elVehicleGrid.innerHTML = cards.join('');
}

function buildVehicleCard(id, data, error = false) {
  if (error || !data) {
    return `
      <div class="vehicle-card">
        <div class="vehicle-card__header">
          <span class="vehicle-card__id">🚗 ${id}</span>
          <span class="vehicle-card__dot vehicle-card__dot--no-data"></span>
        </div>
        <p class="vehicle-card__no-data">${error ? '❌ Sin conexión con API' : 'Sin datos aún'}</p>
      </div>`;
  }

  return `
    <div class="vehicle-card">
      <div class="vehicle-card__header">
        <span class="vehicle-card__id">🚗 ${id}</span>
        <span class="vehicle-card__dot" title="Datos recibidos"></span>
      </div>
      <table class="vehicle-card__table">
        <tr>
          <td>📍 Latitud</td>
          <td>${formatCoord(data.lat)}</td>
        </tr>
        <tr>
          <td>📍 Longitud</td>
          <td>${formatCoord(data.lng)}</td>
        </tr>
        <tr>
          <td>⚡ Velocidad</td>
          <td>${formatSpeed(data.speed)}</td>
        </tr>
        <tr>
          <td>🕐 Timestamp</td>
          <td>${formatTime(data.receivedAt ?? data.timestamp)}</td>
        </tr>
      </table>
    </div>`;
}

// ── 3. Alertas activas ────────────────────────────
async function updateAlerts() {
  try {
    const alerts = await fetchJSON(`${API_BASE}/alerts`);

    if (!alerts || alerts.length === 0) {
      elAlertsContainer.innerHTML =
        '<p class="empty-msg empty-msg--ok">✅ Sin alertas activas en este momento</p>';
      return;
    }

    const items = alerts.map(a => {
      const icon = a.type === 'TEMPERATURA' ? '🌡️' : '⛽';
      return `
        <div class="alert-item">
          <span class="alert-item__icon">${icon}</span>
          <div class="alert-item__body">
            <p class="alert-item__title">[${a.type}] Vehículo ${a.vehicleId}</p>
            <p class="alert-item__meta">${a.rawPayload ?? ''}</p>
          </div>
          <span class="alert-item__time">${formatTime(a.receivedAt ?? a.timestamp)}</span>
        </div>`;
    });

    elAlertsContainer.innerHTML = `<div class="alert-list">${items.join('')}</div>`;
  } catch (e) {
    console.error('[Alerts]', e);
    elAlertsContainer.innerHTML =
      '<p class="empty-msg">❌ No se pudo cargar la lista de alertas</p>';
  }
}

// ── Ciclo principal ───────────────────────────────
async function refresh() {
  await Promise.all([updateStatus(), updateVehicles(), updateAlerts()]);
  elLastUpdated.textContent = `Última actualización: ${new Date().toLocaleTimeString('es-EC')}`;
}

// Arrancar inmediatamente y luego cada 5 segundos
refresh();
setInterval(refresh, REFRESH_MS);
