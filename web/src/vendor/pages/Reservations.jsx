import { useEffect, useState } from 'react';
import { reservationsApi } from '../../api/endpoints.js';
import { apiError } from '../../api/client.js';
import { Button, Input, ErrorNote, EmptyState, Spinner, Badge } from '../../client/components/ui.jsx';
import { money, formatDate } from '../../client/lib/format.js';

const STATUS_STYLE = {
  ACTIVE: 'bg-blue-100 text-blue-700',
  CLOTUREE: 'bg-emerald-100 text-emerald-700',
  ANNULEE: 'bg-slate-200 text-slate-600',
  EXPIREE: 'bg-amber-100 text-amber-700',
};
const FILTERS = [['', 'Toutes'], ['ACTIVE', 'Actives'], ['CLOTUREE', 'Clôturées']];

export default function Reservations() {
  const [filter, setFilter] = useState('');
  const [list, setList] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [detail, setDetail] = useState(null);
  const [pay, setPay] = useState({ amount: '', method: 'ESPECES' });
  const [busy, setBusy] = useState(false);

  const load = () => {
    setLoading(true); setError('');
    reservationsApi.list(filter || undefined)
      .then(({ data }) => setList(data))
      .catch((e) => setError(apiError(e)))
      .finally(() => setLoading(false));
  };
  useEffect(load, [filter]);

  const open = async (id) => {
    setError('');
    try { const { data } = await reservationsApi.detail(id); setDetail(data); setPay({ amount: '', method: 'ESPECES' }); }
    catch (e) { setError(apiError(e)); }
  };

  const addPayment = async () => {
    const amt = parseFloat(String(pay.amount).replace(',', '.'));
    if (Number.isNaN(amt) || amt <= 0) { setError('Montant du versement invalide.'); return; }
    setBusy(true); setError('');
    try {
      const { data } = await reservationsApi.pay(detail.id, { amount: amt, method: pay.method });
      setDetail(data); setPay({ amount: '', method: 'ESPECES' }); load();
    } catch (e) { setError(apiError(e)); } finally { setBusy(false); }
  };

  const cancel = async () => {
    setBusy(true); setError('');
    try { const { data } = await reservationsApi.cancel(detail.id); setDetail(data); load(); }
    catch (e) { setError(apiError(e)); } finally { setBusy(false); }
  };

  return (
    <div className="mx-auto max-w-2xl p-4 pb-24">
      <div className="mb-3 flex gap-2">
        {FILTERS.map(([k, label]) => (
          <button key={k} onClick={() => setFilter(k)}
            className={`rounded-full px-4 py-1.5 text-sm font-semibold ${filter === k ? 'bg-brand-600 text-white' : 'border border-slate-200 bg-white text-slate-600'}`}>
            {label}
          </button>
        ))}
      </div>
      <ErrorNote message={error} onRetry={load} />

      {loading ? <Spinner label="Chargement…" /> : list.length === 0 ? (
        <EmptyState icon="📅" title="Aucune réservation" sub="Les acomptes créés en caisse apparaissent ici." />
      ) : (
        <ul className="space-y-2">
          {list.map((r) => (
            <li key={r.id}>
              <button onClick={() => open(r.id)} className="w-full rounded-xl border border-slate-200 bg-white p-4 text-left hover:bg-slate-50">
                <div className="flex items-center justify-between">
                  <span className="font-bold text-slate-800">{r.reference}</span>
                  <Badge className={STATUS_STYLE[r.status] || 'bg-slate-100 text-slate-600'}>{r.status}{r.dueSoon ? ' · à prévenir' : ''}</Badge>
                </div>
                <div className="mt-0.5 text-sm text-slate-500">{r.customerName}{r.customerPhone ? ` · ${r.customerPhone}` : ''}</div>
                <div className="mt-1 flex justify-between text-sm">
                  <span className="text-slate-500">Reste <b className="text-slate-800">{money(r.remaining)}</b> / {money(r.total)}</span>
                  {r.status === 'ACTIVE' && <span className="text-slate-400">{r.daysRemaining} j restants</span>}
                </div>
              </button>
            </li>
          ))}
        </ul>
      )}

      {detail && (
        <div className="fixed inset-0 z-50 flex items-end justify-center bg-black/40 sm:items-center" onClick={() => setDetail(null)}>
          <div className="max-h-[90vh] w-full max-w-lg overflow-y-auto rounded-t-2xl bg-white p-5 sm:rounded-2xl" onClick={(e) => e.stopPropagation()}>
            <div className="mb-2 flex items-center justify-between">
              <h3 className="text-lg font-extrabold text-slate-800">{detail.reference}</h3>
              <Badge className={STATUS_STYLE[detail.status] || 'bg-slate-100 text-slate-600'}>{detail.status}</Badge>
            </div>
            <p className="text-sm text-slate-500">{detail.customerName} · créée {formatDate(detail.createdAt)}</p>

            <ul className="mt-3 divide-y divide-slate-100">
              {detail.items?.map((it, i) => (
                <li key={i} className="flex justify-between py-2 text-sm">
                  <span className="min-w-0 truncate">{it.productName} · {it.colorName}/{it.size} × {it.quantity}</span>
                  <span className="ml-2 shrink-0 font-medium">{money(it.totalPrice ?? it.unitPrice)}</span>
                </li>
              ))}
            </ul>
            <div className="mt-2 space-y-1 border-t border-slate-200 pt-2 text-sm">
              <div className="flex justify-between"><span className="text-slate-500">Total</span><b>{money(detail.total)}</b></div>
              <div className="flex justify-between"><span className="text-slate-500">Payé</span><b className="text-emerald-600">{money(detail.paid)}</b></div>
              <div className="flex justify-between"><span className="text-slate-500">Reste</span><b className="text-rose-600">{money(detail.remaining)}</b></div>
            </div>

            <ErrorNote message={error} />

            {detail.status === 'ACTIVE' && (
              <div className="mt-4 space-y-3 rounded-xl border border-slate-200 bg-slate-50 p-3">
                <p className="text-sm font-semibold text-slate-600">Ajouter un versement</p>
                <div className="flex gap-2">
                  <Input value={pay.amount} onChange={(e) => setPay((p) => ({ ...p, amount: e.target.value }))} placeholder="Montant" inputMode="decimal" />
                  {['ESPECES', 'CARTE'].map((m) => (
                    <button key={m} onClick={() => setPay((p) => ({ ...p, method: m }))}
                      className={`shrink-0 rounded-lg border px-3 text-sm font-semibold ${pay.method === m ? 'border-brand-600 bg-brand-600 text-white' : 'border-slate-300 bg-white text-slate-700'}`}>
                      {m === 'ESPECES' ? 'Esp.' : 'Carte'}
                    </button>
                  ))}
                </div>
                <div className="flex gap-2">
                  <Button className="flex-1" disabled={busy} onClick={addPayment}>{busy ? '…' : 'Encaisser'}</Button>
                  <Button variant="danger" disabled={busy} onClick={cancel}>Annuler la résa</Button>
                </div>
              </div>
            )}
            <button onClick={() => setDetail(null)} className="mt-3 w-full rounded-xl border border-slate-300 py-2.5 text-sm font-semibold text-slate-600">Fermer</button>
          </div>
        </div>
      )}
    </div>
  );
}
