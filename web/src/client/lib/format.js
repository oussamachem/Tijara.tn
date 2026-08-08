// Dinar tunisien — 3 décimales (millimes), ex. 49.000 DT.
export function money(value) {
  const n = Number(value ?? 0);
  return `${n.toFixed(3)} DT`;
}

// Nombre compact façon réseaux sociaux : 1234 -> 1.2k, 2000000 -> 2M.
export function compact(value) {
  const n = Number(value ?? 0);
  if (n >= 1_000_000) return `${(n / 1_000_000).toFixed(n % 1_000_000 ? 1 : 0)}M`;
  if (n >= 1_000) return `${(n / 1_000).toFixed(n % 1_000 ? 1 : 0)}k`;
  return String(n);
}

export function formatDate(iso) {
  if (!iso) return '—';
  try {
    const d = new Date(iso);
    const p = (x) => String(x).padStart(2, '0');
    return `${p(d.getDate())}/${p(d.getMonth() + 1)}/${d.getFullYear()} ${p(d.getHours())}:${p(d.getMinutes())}`;
  } catch {
    return iso;
  }
}

export const STATUS_LABEL = {
  EN_ATTENTE: 'En attente',
  CONFIRMEE: 'Confirmée',
  PRETE: 'Prête',
  RECUPEREE: 'Récupérée',
  ANNULEE: 'Annulée',
};

export const STATUS_STYLE = {
  EN_ATTENTE: 'bg-amber-100 text-amber-700',
  CONFIRMEE: 'bg-blue-100 text-blue-700',
  PRETE: 'bg-indigo-100 text-indigo-700',
  RECUPEREE: 'bg-emerald-100 text-emerald-700',
  ANNULEE: 'bg-slate-200 text-slate-600',
};
