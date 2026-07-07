import { useEffect, useMemo, useState } from 'react';
import { ordersApi } from '../api/endpoints.js';
import { apiError } from '../api/client.js';
import { Badge, Spinner, Alert, Modal, Button, Input } from '../components/ui.jsx';
import { formatMoney, formatDate } from '../utils/format.js';

const STATUS = {
  EN_ATTENTE: { label: 'En attente', color: 'amber' },
  CONFIRMEE: { label: 'Confirmée', color: 'indigo' },
  PRETE: { label: 'Prête', color: 'green' },
  RECUPEREE: { label: 'Récupérée', color: 'slate' },
  ANNULEE: { label: 'Annulée', color: 'red' },
};

// Actions autorisees par statut (miroir des transitions serveur).
const ACTIONS = {
  EN_ATTENTE: [
    { to: 'CONFIRMEE', label: 'Confirmer', variant: 'primary', warn: 'Le stock sera décrémenté.' },
    { to: 'ANNULEE', label: 'Annuler', variant: 'danger' },
  ],
  CONFIRMEE: [
    { to: 'PRETE', label: 'Marquer prête', variant: 'primary' },
    { to: 'ANNULEE', label: 'Annuler', variant: 'danger', warn: 'Le stock sera restauré.' },
  ],
  PRETE: [
    { to: 'RECUPEREE', label: 'Marquer récupérée', variant: 'primary' },
    { to: 'ANNULEE', label: 'Annuler', variant: 'danger', warn: 'Le stock sera restauré.' },
  ],
  RECUPEREE: [],
  ANNULEE: [],
};

const FILTERS = [['', 'Toutes'], ['EN_ATTENTE', 'En attente'], ['CONFIRMEE', 'Confirmées'], ['PRETE', 'Prêtes'], ['RECUPEREE', 'Récupérées'], ['ANNULEE', 'Annulées']];

export default function Orders() {
  const [filter, setFilter] = useState('');
  const [query, setQuery] = useState('');
  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [openId, setOpenId] = useState(null);

  const load = () => {
    setLoading(true);
    setError('');
    ordersApi
      .list({ status: filter || undefined, query: query || undefined })
      .then(({ data }) => setRows(data))
      .catch((e) => setError(apiError(e)))
      .finally(() => setLoading(false));
  };

  useEffect(load, [filter]);

  // Recherche : debounce leger.
  useEffect(() => {
    const t = setTimeout(load, 300);
    return () => clearTimeout(t);
  }, [query]);

  // Priorite aux commandes EN_ATTENTE, puis par date decroissante.
  const sorted = useMemo(() => {
    return [...rows].sort((a, b) => {
      const pa = a.status === 'EN_ATTENTE' ? 0 : 1;
      const pb = b.status === 'EN_ATTENTE' ? 0 : 1;
      if (pa !== pb) return pa - pb;
      return new Date(b.createdAt) - new Date(a.createdAt);
    });
  }, [rows]);

  const pendingCount = rows.filter((r) => r.status === 'EN_ATTENTE').length;

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-xl font-bold text-slate-800">Commandes en ligne</h1>
          <p className="text-sm text-slate-500">
            {pendingCount > 0 ? `${pendingCount} commande(s) en attente de traitement` : 'Aucune commande en attente'}
          </p>
        </div>
        <div className="w-full max-w-xs">
          <Input placeholder="🔍 Référence ou client…" value={query} onChange={(e) => setQuery(e.target.value)} />
        </div>
      </div>

      <div className="flex flex-wrap gap-2">
        {FILTERS.map(([key, label]) => (
          <button
            key={key || 'all'}
            onClick={() => setFilter(key)}
            className={`rounded-full px-3 py-1.5 text-sm font-medium ${
              filter === key ? 'bg-brand-600 text-white' : 'bg-white text-slate-600 border border-slate-200'
            }`}
          >
            {label}
          </button>
        ))}
      </div>

      {error && <Alert type="error">{error}</Alert>}

      {loading ? (
        <Spinner className="mt-10" />
      ) : sorted.length === 0 ? (
        <div className="rounded-xl border border-dashed border-slate-200 bg-white py-16 text-center text-slate-400">
          Aucune commande.
        </div>
      ) : (
        <div className="overflow-hidden rounded-xl border border-slate-200 bg-white">
          <table className="w-full text-sm">
            <thead className="bg-slate-50 text-left text-xs uppercase tracking-wide text-slate-500">
              <tr>
                <th className="px-4 py-3">Référence</th>
                <th className="px-4 py-3">Client</th>
                <th className="px-4 py-3">Date</th>
                <th className="px-4 py-3 text-center">Articles</th>
                <th className="px-4 py-3 text-right">Total</th>
                <th className="px-4 py-3">Statut</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {sorted.map((o) => (
                <tr key={o.id} onClick={() => setOpenId(o.id)} className="cursor-pointer hover:bg-slate-50">
                  <td className="px-4 py-3 font-semibold text-slate-800">{o.reference}</td>
                  <td className="px-4 py-3">
                    <div className="text-slate-700">{o.clientName || '—'}</div>
                    <div className="text-xs text-slate-400">{o.clientEmail}</div>
                  </td>
                  <td className="px-4 py-3 text-slate-500">{formatDate(o.createdAt)}</td>
                  <td className="px-4 py-3 text-center text-slate-600">{o.itemCount}</td>
                  <td className="px-4 py-3 text-right font-bold text-slate-800">{formatMoney(o.total)}</td>
                  <td className="px-4 py-3"><Badge color={STATUS[o.status]?.color}>{STATUS[o.status]?.label || o.status}</Badge></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <OrderDetailModal id={openId} onClose={() => setOpenId(null)} onChanged={load} />
    </div>
  );
}

function OrderDetailModal({ id, onClose, onChanged }) {
  const [detail, setDetail] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [pending, setPending] = useState(null); // action en cours de confirmation
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    if (!id) { setDetail(null); return; }
    setLoading(true);
    setError('');
    ordersApi.get(id).then(({ data }) => setDetail(data)).catch((e) => setError(apiError(e))).finally(() => setLoading(false));
  }, [id]);

  const apply = async () => {
    setBusy(true);
    setError('');
    try {
      const { data } = await ordersApi.changeStatus(id, pending.to);
      setDetail(data);
      setPending(null);
      onChanged?.();
    } catch (e) {
      setError(apiError(e)); // 409 = stock insuffisant / transition invalide
      setPending(null);
    } finally {
      setBusy(false);
    }
  };

  const actions = detail ? ACTIONS[detail.status] || [] : [];

  return (
    <Modal open={!!id} onClose={onClose} title={detail ? `Commande ${detail.reference}` : 'Commande'} wide>
      {loading || !detail ? (
        <Spinner className="my-8" />
      ) : (
        <div className="space-y-5">
          {error && <Alert type="error" onClose={() => setError('')}>{error}</Alert>}

          <div className="flex items-center justify-between">
            <Badge color={STATUS[detail.status]?.color}>{STATUS[detail.status]?.label || detail.status}</Badge>
            <span className="text-sm text-slate-400">{formatDate(detail.createdAt)}</span>
          </div>

          <div className="rounded-lg bg-slate-50 p-3">
            <div className="text-xs font-semibold uppercase tracking-wide text-slate-400">Client</div>
            <div className="mt-1 font-medium text-slate-800">{detail.clientName || '—'}</div>
            <div className="text-sm text-slate-500">{detail.clientEmail}</div>
          </div>

          <div>
            <div className="mb-2 text-xs font-semibold uppercase tracking-wide text-slate-400">Articles</div>
            <ul className="divide-y divide-slate-100 rounded-lg border border-slate-100">
              {detail.items.map((it, i) => (
                <li key={i} className="flex items-center justify-between px-3 py-2 text-sm">
                  <span className="text-slate-700">{it.productName} · {it.color}/{it.size} × {it.quantity}</span>
                  <span className="font-medium text-slate-700">{formatMoney(it.unitPrice * it.quantity)}</span>
                </li>
              ))}
            </ul>
            <div className="mt-2 flex justify-between px-1 text-sm">
              <span className="text-slate-500">Total</span>
              <span className="font-bold text-slate-800">{formatMoney(detail.total)}</span>
            </div>
          </div>

          <div>
            <div className="mb-2 text-xs font-semibold uppercase tracking-wide text-slate-400">Historique</div>
            <ol className="space-y-2">
              {detail.history.map((h, i) => (
                <li key={i} className="flex items-start gap-3 text-sm">
                  <span className="mt-1 h-2 w-2 shrink-0 rounded-full bg-brand-500" />
                  <div>
                    <span className="font-medium text-slate-700">
                      {h.fromStatus ? `${STATUS[h.fromStatus]?.label} → ` : 'Création · '}
                      {STATUS[h.toStatus]?.label || h.toStatus}
                    </span>
                    <div className="text-xs text-slate-400">{formatDate(h.at)} · {h.changedBy || '—'}</div>
                  </div>
                </li>
              ))}
            </ol>
          </div>

          {actions.length > 0 ? (
            <div className="flex flex-wrap gap-2 border-t border-slate-100 pt-4">
              {actions.map((a) => (
                <Button key={a.to} variant={a.variant} onClick={() => setPending(a)}>{a.label}</Button>
              ))}
            </div>
          ) : (
            <p className="border-t border-slate-100 pt-4 text-sm text-slate-400">Commande clôturée — aucune action.</p>
          )}
        </div>
      )}

      {pending && (
        <Modal
          open
          onClose={() => setPending(null)}
          title={`${pending.label} ?`}
          footer={
            <>
              <Button variant="secondary" onClick={() => setPending(null)} disabled={busy}>Annuler</Button>
              <Button variant={pending.variant === 'danger' ? 'danger' : 'primary'} onClick={apply} disabled={busy}>
                {busy ? '…' : pending.label}
              </Button>
            </>
          }
        >
          <p className="text-sm text-slate-600">
            Passer la commande <b>{detail?.reference}</b> à « <b>{STATUS[pending.to]?.label}</b> ».
            {pending.warn && <span className="mt-2 block text-slate-500">{pending.warn}</span>}
          </p>
        </Modal>
      )}
    </Modal>
  );
}
