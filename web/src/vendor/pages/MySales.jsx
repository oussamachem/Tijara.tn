import { useEffect, useState } from 'react';
import { posApi } from '../../api/endpoints.js';
import { apiError } from '../../api/client.js';
import { ErrorNote, EmptyState, Spinner, Button } from '../../client/components/ui.jsx';
import { money, formatDate } from '../../client/lib/format.js';

const PAY_LABEL = { ESPECES: 'Espèces', CARTE: 'Carte', MIXTE: 'Mixte' };

export default function MySales() {
  const [sales, setSales] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [detail, setDetail] = useState(null);

  useEffect(() => {
    posApi.mySales({ size: 30 })
      .then(({ data }) => setSales(data.content || []))
      .catch((e) => setError(apiError(e)))
      .finally(() => setLoading(false));
  }, []);

  const open = async (id) => {
    setError('');
    try { const { data } = await posApi.saleDetail(id); setDetail(data); }
    catch (e) { setError(apiError(e)); }
  };

  return (
    <div className="mx-auto max-w-2xl p-4 pb-24">
      <ErrorNote message={error} />
      {loading ? <Spinner label="Chargement…" /> : sales.length === 0 ? (
        <EmptyState icon="🧾" title="Aucune vente" sub="Vos ventes du jour apparaîtront ici." />
      ) : (
        <ul className="space-y-2">
          {sales.map((s) => (
            <li key={s.id}>
              <button onClick={() => open(s.id)} className="flex w-full items-center justify-between rounded-xl border border-slate-200 bg-white p-4 text-left hover:bg-slate-50">
                <span>
                  <span className="block font-bold text-slate-800">Vente #{s.id}</span>
                  <span className="text-xs text-slate-400">{formatDate(s.saleDate)} · {s.itemCount} article(s) · {PAY_LABEL[s.paymentMethod] || s.paymentMethod}</span>
                </span>
                <span className="font-extrabold text-brand-700">{money(s.totalAmount)}</span>
              </button>
            </li>
          ))}
        </ul>
      )}

      {detail && (
        <div className="fixed inset-0 z-50 flex items-end justify-center bg-black/40 sm:items-center" onClick={() => setDetail(null)}>
          <div className="max-h-[90vh] w-full max-w-lg overflow-y-auto rounded-t-2xl bg-white p-5 sm:rounded-2xl" onClick={(e) => e.stopPropagation()}>
            <h3 className="text-lg font-extrabold text-slate-800">Vente #{detail.id}</h3>
            <p className="text-sm text-slate-400">{formatDate(detail.saleDate)} · {detail.sellerName}</p>
            <ul className="mt-3 divide-y divide-slate-100">
              {detail.items.map((it) => (
                <li key={it.id} className="flex justify-between py-2 text-sm">
                  <span className="min-w-0 truncate">{it.productName} · {it.colorName}/{it.size} × {it.quantity}</span>
                  <span className="ml-2 shrink-0 font-medium">{money(it.totalPrice)}</span>
                </li>
              ))}
            </ul>
            <div className="mt-2 space-y-1 border-t border-slate-200 pt-2 text-sm">
              {Number(detail.discount) > 0 && <div className="flex justify-between"><span className="text-slate-500">Remise</span><span>- {money(detail.discount)}</span></div>}
              <div className="flex justify-between"><span className="font-semibold text-slate-700">Total</span><span className="text-lg font-extrabold text-slate-800">{money(detail.totalAmount)}</span></div>
            </div>
            <Button className="mt-4 w-full" onClick={() => setDetail(null)}>Fermer</Button>
          </div>
        </div>
      )}
    </div>
  );
}
