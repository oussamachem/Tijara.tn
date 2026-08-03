// Devise : Dinar tunisien — 3 décimales (millimes), ex. 49.000 DT.
export function formatMoney(value) {
  const n = Number(value ?? 0);
  return `${n.toFixed(3)} DT`;
}

export function formatDate(iso) {
  if (!iso) return '—';
  try {
    return new Date(iso).toLocaleString('fr-FR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  } catch {
    return iso;
  }
}
