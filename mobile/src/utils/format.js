// Formatage monétaire robuste (sans Intl, dont le support varie sous Hermes).
// Devise : Dinar tunisien.
export function formatMoney(value) {
  const n = Number(value ?? 0);
  return `${n.toFixed(2)} DT`;
}

export function formatDate(iso) {
  if (!iso) return '—';
  try {
    const d = new Date(iso);
    const pad = (x) => String(x).padStart(2, '0');
    return `${pad(d.getDate())}/${pad(d.getMonth() + 1)}/${d.getFullYear()} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
  } catch {
    return iso;
  }
}
