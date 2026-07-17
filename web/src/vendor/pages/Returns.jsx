import { useEffect, useState } from 'react';
import { posApi } from '../../api/endpoints.js';
import { apiError } from '../../api/client.js';
import { ErrorNote, EmptyState, Spinner, Button, Input } from '../../client/components/ui.jsx';
import { money, formatDate } from '../../client/lib/format.js';

/** Retour produit : choisir une vente récente -> une ligne -> quantité + motif. Stock réintégré. */
export default function Returns() {
  const [sales, setSales] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [sale, setSale] = useState(null);      // vente détaillée choisie
  const [selected, setSelected] = useState(null); // ligne choisie
  const [qty, setQty] = useState(1);
  const [reason, setReason] = useState('');
  const [busy, setBusy] = useState(false);
  const [done, setDone] = useState('');

  useEffect(() => {
    posApi.mySales({ size: 30 })
      .then(({ data }) => setSales(data.content || []))
      .catch((e) => setError(apiError(e)))
      .finally(() => setLoading(false));
  }, []);

  const openSale = async (id) => {
    setError(''); setSelected(null); setDone('');
    try { const { data } = await posApi.saleDetail(id); setSale(data); }
    catch (e) { setError(apiError(e)); }
  };

  const submit = async () => {
    if (!selected) return;
    setBusy(true); setError('');
    try {
      await posApi.createReturn({ saleId: sale.id, variantId: selected.variantId, quantity: qty, reason: reason.trim() });
      setDone(`Retour enregistré : ${selected.productName} × ${qty}. Stock réintégré.`);
      setSale(null); setSelected(null); setReason(''); setQty(1);
    } catch (e) {
      setError(apiError(e)); // 409 = plafond (vendu − déjà retourné)
    } finally { setBusy(false); }
  };

  return (
    <div className="mx-auto max-w-2xl p-4 pb-24">
      {done && <div className="mb-3 rounded-xl bg-emerald-50 px-4 py-3 text-sm font-semibold text-emerald-700">{done}</div>}
      <ErrorNote message={error} />
      <p className="mb-2 text-sm text-slate-500">Choisissez la vente d'origine du produit à retourner.</p>

      {loading ? <Spinner label="Chargement…" /> : sales.length === 0 ? (
        <EmptyState icon="↩️" title="Aucune vente" sub="Impossible de faire un retour sans vente." />
      ) : (
        <ul className="space-y-2">
          {sales.map((s) => (
            <li key={s.id}>
              <button onClick={() => openSale(s.id)} className="flex w-full items-center justify-between rounded-xl border border-slate-200 bg-white p-4 text-left hover:bg-slate-50">
                <span><span className="block font-bold text-slate-800">Vente #{s.id}</span>
                  <span className="text-xs text-slate-400">{formatDate(s.saleDate)} · {s.itemCount} article(s)</span></span>
                <span className="font-extrabold text-brand-700">{money(s.totalAmount)}</span>
              </button>
            </li>
          ))}
        </ul>
      )}

      {sale && (
        <div className="fixed inset-0 z-50 flex items-end justify-center bg-black/40 sm:items-center" onClick={() => setSale(null)}>
          <div className="max-h-[90vh] w-full max-w-lg overflow-y-auto rounded-t-2xl bg-white p-5 sm:rounded-2xl" onClick={(e) => e.stopPropagation()}>
            <h3 className="text-lg font-extrabold text-slate-800">Vente #{sale.id}</h3>
            <p className="text-sm text-slate-400">Choisissez l'article à retourner :</p>
            <ul className="mt-3 space-y-2">
              {sale.items.map((it) => (
                <li key={it.id}>
                  <button onClick={() => { setSelected(it); setQty(1); }}
                    className={`flex w-full items-center justify-between rounded-xl border p-3 text-left ${selected?.id === it.id ? 'border-brand-600 bg-brand-50' : 'border-slate-200 bg-white'}`}>
                    <span className="min-w-0"><span className="block truncate font-semibold text-slate-800">{it.productName} <span className="font-normal text-slate-400">({it.colorName} · {it.size})</span></span>
                      <span className="text-xs text-slate-400">{it.variantReference} · vendu {it.quantity} × {money(it.unitPrice)}</span></span>
                    {selected?.id === it.id && <span className="ml-2 text-brand-600">✓</span>}
                  </button>
                </li>
              ))}
            </ul>

            {selected && (
              <div className="mt-4 space-y-3 rounded-xl border border-slate-200 bg-slate-50 p-3">
                <ErrorNote message={error} />
                <p className="text-sm font-semibold text-slate-600">Quantité à retourner (max {selected.quantity})</p>
                <div className="flex items-center gap-2">
                  <button onClick={() => setQty((q) => Math.max(1, q - 1))} className="h-9 w-9 rounded-lg border border-slate-300 text-lg font-bold">−</button>
                  <span className="w-8 text-center font-semibold">{qty}</span>
                  <button onClick={() => setQty((q) => Math.min(selected.quantity, q + 1))} className="h-9 w-9 rounded-lg border border-slate-300 text-lg font-bold">+</button>
                </div>
                <p className="text-xs italic text-slate-400">Le serveur refuse tout retour dépassant (vendu − déjà retourné).</p>
                <Input value={reason} onChange={(e) => setReason(e.target.value)} placeholder="Motif (ex. taille incorrecte, défaut…)" />
                <Button className="w-full" disabled={busy} onClick={submit}>{busy ? 'Validation…' : 'Valider le retour'}</Button>
              </div>
            )}
            <button onClick={() => setSale(null)} className="mt-3 w-full rounded-xl border border-slate-300 py-2.5 text-sm font-semibold text-slate-600">Fermer</button>
          </div>
        </div>
      )}
    </div>
  );
}
