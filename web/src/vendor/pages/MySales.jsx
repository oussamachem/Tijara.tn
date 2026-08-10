import { useEffect, useState } from 'react';
import { posApi, salesApi } from '../../api/endpoints.js';
import { apiError } from '../../api/client.js';
import { useShop } from '../../context/ShopContext.jsx';
import { ErrorNote, EmptyState, Spinner, Button } from '../../client/components/ui.jsx';
import { money, formatDate } from '../../client/lib/format.js';
import { printSaleTicket } from '../lib/ticket.js';

const PAY_LABEL = { ESPECES: 'Espèces', CARTE: 'Carte', MIXTE: 'Mixte' };

/**
 * Liste des ventes.
 * - `all=false` (vendeur) : SES ventes (/api/sales/mine).
 * - `all=true` (propriétaire, caisse) : TOUTES les ventes de la boutique (/api/admin/sales),
 *   pour superviser qui a vendu. Chaque ligne affiche le nom du vendeur (déjà fourni par l'API).
 */
export default function MySales({ all = false }) {
  const { activeShop } = useShop();
  const [sales, setSales] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [detail, setDetail] = useState(null);

  useEffect(() => {
    setLoading(true); setError('');
    const req = all ? salesApi.history({ page: 0, size: 30 }) : posApi.mySales({ size: 30 });
    req
      .then(({ data }) => setSales(data.content || []))
      .catch((e) => setError(apiError(e)))
      .finally(() => setLoading(false));
  }, [all]);

  const open = async (id) => {
    setError('');
    try { const { data } = await posApi.saleDetail(id); setDetail(data); }
    catch (e) { setError(apiError(e)); }
  };

  return (
    <div className="mx-auto max-w-2xl p-4 pb-24">
      <ErrorNote message={error} />
      {loading ? <Spinner label="Chargement…" /> : sales.length === 0 ? (
        <EmptyState icon="🧾" title="Aucune vente"
          sub={all ? 'Les ventes de la boutique apparaîtront ici.' : 'Vos ventes du jour apparaîtront ici.'} />
      ) : (
        <ul className="space-y-2">
          {sales.map((s) => (
            <li key={s.id}>
              <button onClick={() => open(s.id)} className="flex w-full items-center justify-between gap-3 rounded-xl border border-slate-200 bg-white p-4 text-left hover:bg-slate-50">
                <span className="min-w-0">
                  <span className="flex items-center gap-2 font-bold text-slate-800">
                    Vente #{s.id}
                    <span className="truncate rounded-full bg-slate-100 px-2 py-0.5 text-[11px] font-semibold text-slate-600">👤 {s.sellerName}</span>
                  </span>
                  <span className="text-xs text-slate-400">{formatDate(s.saleDate)} · {s.itemCount} article(s) · {PAY_LABEL[s.paymentMethod] || s.paymentMethod}</span>
                </span>
                <span className="shrink-0 font-extrabold text-brand-700">{money(s.totalAmount)}</span>
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
            <div className="mt-4 flex gap-2">
              <button onClick={() => printSaleTicket(activeShop?.name, detail)}
                className="flex-1 rounded-xl bg-slate-900 py-2.5 text-sm font-semibold text-white active:scale-[.99]">🖨️ Imprimer le ticket</button>
              <Button className="flex-1" onClick={() => setDetail(null)}>Fermer</Button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
