import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { posApi, reservationsApi } from '../../api/endpoints.js';
import { apiError } from '../../api/client.js';
import { usePosCart } from '../PosCartContext.jsx';
import QrScanner from '../QrScanner.jsx';
import { Button, Input, ErrorNote, EmptyState, Spinner } from '../../client/components/ui.jsx';
import { money } from '../../client/lib/format.js';

const PAYMENTS = [
  ['ESPECES', 'Espèces'],
  ['CARTE', 'Carte'],
  ['MIXTE', 'Mixte'],
  ['TICKET_CADEAU', 'Ticket cadeau'],
];
const ISSUERS = ['PLUXEE', 'JOKER', 'AUTRE'];
const DENOMINATIONS = [10, 20, 50];
const maskCode = (c) => (c && c.length > 4 ? `••••${c.slice(-4)}` : c);
const isExpired = (d) => d && !Number.isNaN(Date.parse(d)) && new Date(d) < new Date(new Date().toDateString());

// Produit+variante (recherche admin) -> ligne compatible avec le panier (forme VariantScanResponse).
const toLine = (p, v) => ({
  variantId: v.id, variantReference: v.reference, productName: p.name, salePrice: p.salePrice,
  colorName: v.colorName, colorHex: v.colorHex, size: v.size, quantity: v.quantity,
});

function Stepper({ value, onChange, max }) {
  return (
    <div className="flex items-center gap-2">
      <button onClick={() => onChange(Math.max(1, value - 1))} className="h-9 w-9 rounded-lg border border-slate-300 text-lg font-bold text-slate-600">−</button>
      <span className="w-8 text-center font-semibold">{value}</span>
      <button onClick={() => onChange(max ? Math.min(max, value + 1) : value + 1)} className="h-9 w-9 rounded-lg border border-slate-300 text-lg font-bold text-slate-600">+</button>
    </div>
  );
}

export default function Pos() {
  const navigate = useNavigate();
  const { items, add, setQuantity, remove, clear, subtotal, count } = usePosCart();

  const [scanOpen, setScanOpen] = useState(false);
  const [query, setQuery] = useState('');
  const [results, setResults] = useState([]);
  const [searching, setSearching] = useState(false);
  const [searched, setSearched] = useState(false);
  const [picker, setPicker] = useState(null);   // produit dont on choisit la variante
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

  const [payment, setPayment] = useState('ESPECES');
  const [discount, setDiscount] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [receipt, setReceipt] = useState(null);

  // Ticket cadeau
  const [tickets, setTickets] = useState([]);
  const [ticketForm, setTicketForm] = useState(null);
  const [remainderMethod, setRemainderMethod] = useState('ESPECES');
  const [ticketScan, setTicketScan] = useState(false);

  // Réservation
  const [reserve, setReserve] = useState(null);

  const d = parseFloat(String(discount).replace(',', '.'));
  const disc = !Number.isNaN(d) && d > 0 ? d : 0;
  const total = Math.max(0, subtotal - disc);
  const ticketsTotal = tickets.reduce((s, t) => s + Number(t.value), 0);
  const reste = Math.max(0, total - ticketsTotal);
  const surplus = ticketsTotal > total;

  // --------------------------------- Scan / recherche ---------------------------------
  const onScanned = async (code) => {
    setScanOpen(false);
    setError(''); setNotice('');
    try {
      const { data } = await posApi.byQr(code);
      add(data, 1);
      setNotice(`Ajouté : ${data.productName} (${data.colorName} · ${data.size})`);
    } catch (err) {
      setError(err?.response?.status === 404 ? `Aucune variante pour « ${code} »` : apiError(err));
    }
  };

  const runSearch = async () => {
    const q = query.trim();
    if (!q) return;
    setSearching(true); setError(''); setSearched(true);
    try {
      let { data } = await posApi.productSearch({ name: q, size: 20 });
      if (!data.content?.length) data = (await posApi.productSearch({ reference: q, size: 20 })).data;
      setResults(data.content || []);
    } catch (err) { setError(apiError(err)); }
    finally { setSearching(false); }
  };

  const pickVariant = (v) => {
    add(toLine(picker, v), 1);
    setNotice(`Ajouté : ${picker.name} (${v.colorName} · ${v.size})`);
    setPicker(null);
  };

  // ------------------------------------- Ticket cadeau -------------------------------------
  const onTicketScanned = (code) => {
    setTicketScan(false);
    if (!code) return;
    if (tickets.some((t) => t.code === code)) { setError('Ce ticket est déjà dans le panier.'); return; }
    setTicketForm({ code, issuer: 'PLUXEE', value: 20, expiry: '' });
  };
  const addTicket = () => {
    if (isExpired(ticketForm.expiry)) { setError('Ticket expiré — le serveur le refusera.'); return; }
    setTickets((ts) => [...ts, { ...ticketForm }]);
    setTicketForm(null);
  };
  const removeTicket = (code) => setTickets((ts) => ts.filter((t) => t.code !== code));

  // ---------------------------------------- Vente ----------------------------------------
  const validate = async () => {
    if (submitting || items.length === 0) return;
    if (payment === 'TICKET_CADEAU' && tickets.length === 0) {
      setError('Ajoutez au moins un ticket cadeau (ou changez de mode de paiement).');
      return;
    }
    setSubmitting(true); setError('');
    try {
      const payload = { items: items.map((i) => ({ variantId: i.variant.variantId, quantity: i.quantity })) };
      if (disc > 0) payload.discount = disc;
      if (payment === 'TICKET_CADEAU') {
        const payments = tickets.map((t) => ({
          method: 'TICKET_CADEAU', amount: Number(t.value), issuer: t.issuer,
          ticketCode: t.code, ticketExpiry: t.expiry || null,
        }));
        if (reste > 0) payments.push({ method: remainderMethod, amount: reste });
        payload.payments = payments;
      } else {
        payload.paymentMethod = payment;
      }
      const { data } = await posApi.createSale(payload);
      clear(); setTickets([]); setDiscount(''); setPayment('ESPECES');
      setReceipt(data);
    } catch (err) {
      setError(apiError(err)); // 409 = stock/ticket ; 400 = ticket expiré/dénomination
    } finally {
      setSubmitting(false);
    }
  };

  const submitReserve = async () => {
    if (submitting) return;
    if (!reserve.name.trim()) { setError('Le nom du client est obligatoire pour une réservation.'); return; }
    setSubmitting(true); setError('');
    try {
      const dep = parseFloat(String(reserve.deposit).replace(',', '.'));
      const dur = parseInt(reserve.duration, 10);
      const payload = {
        customerName: reserve.name.trim(),
        customerPhone: reserve.phone.trim() || null,
        items: items.map((i) => ({ variantId: i.variant.variantId, quantity: i.quantity })),
        downPayment: !Number.isNaN(dep) && dep > 0 ? dep : 0,
        downPaymentMethod: reserve.method,
      };
      if (!Number.isNaN(dur) && dur > 0) payload.durationDays = dur;
      const { data } = await reservationsApi.create(payload);
      clear(); setReserve(null);
      setNotice(`Réservation ${data.reference} créée · reste ${money(data.remaining)}.`);
      navigate('/reservations');
    } catch (err) {
      setError(apiError(err));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="mx-auto max-w-2xl p-4 pb-40">
      {/* Actions scan + recherche */}
      <div className="flex flex-col gap-3 sm:flex-row">
        <Button className="sm:w-48" onClick={() => setScanOpen(true)}>📷 Scanner un produit</Button>
        <div className="flex flex-1 gap-2">
          <Input value={query} onChange={(e) => setQuery(e.target.value)} placeholder="Nom ou référence…"
            onKeyDown={(e) => e.key === 'Enter' && runSearch()} />
          <Button variant="secondary" className="shrink-0" onClick={runSearch}>Chercher</Button>
        </div>
      </div>

      {notice && (
        <div className="mt-3 flex items-center justify-between rounded-xl bg-emerald-50 px-4 py-2.5 text-sm font-medium text-emerald-700">
          <span>{notice}</span><button onClick={() => setNotice('')}>✕</button>
        </div>
      )}
      <div className="mt-3"><ErrorNote message={error} /></div>

      {/* Résultats de recherche */}
      {searching ? (
        <Spinner label="Recherche…" />
      ) : results.length > 0 ? (
        <ul className="mt-3 space-y-2">
          {results.map((p) => (
            <li key={p.id}>
              <button onClick={() => setPicker(p)} className="flex w-full items-center justify-between rounded-xl border border-slate-200 bg-white px-4 py-3 text-left hover:bg-slate-50">
                <span className="min-w-0"><span className="block truncate font-semibold text-slate-800">{p.name}</span>
                  <span className="text-xs text-slate-400">{p.reference} · {p.variants.length} variante(s)</span></span>
                <span className="ml-3 shrink-0 font-bold text-brand-700">{money(p.salePrice)}</span>
              </button>
            </li>
          ))}
        </ul>
      ) : searched && !searching ? (
        <p className="mt-4 text-center text-sm text-slate-400">Aucun produit trouvé.</p>
      ) : null}

      {/* Panier */}
      <h2 className="mt-6 mb-2 text-sm font-bold uppercase tracking-wide text-slate-400">Panier ({count})</h2>
      {items.length === 0 ? (
        <EmptyState icon="🛒" title="Panier vide" sub="Scannez un produit ou utilisez la recherche." />
      ) : (
        <ul className="space-y-2">
          {items.map((it) => (
            <li key={it.variant.variantId} className="flex items-center gap-3 rounded-xl border border-slate-200 bg-white p-3">
              <div className="min-w-0 flex-1">
                <div className="truncate font-semibold text-slate-800">{it.variant.productName}</div>
                <div className="text-xs text-slate-500">{it.variant.colorName} · {it.variant.size} — {money(it.variant.salePrice)} × {it.quantity} = {money(Number(it.variant.salePrice) * it.quantity)}</div>
                <button onClick={() => remove(it.variant.variantId)} className="mt-0.5 text-xs font-semibold text-rose-600">Retirer</button>
              </div>
              <Stepper value={it.quantity} max={it.variant.quantity > 0 ? it.variant.quantity : undefined}
                onChange={(q) => setQuantity(it.variant.variantId, q)} />
            </li>
          ))}
        </ul>
      )}

      {/* Barre de validation fixe */}
      {items.length > 0 && (
        <div className="fixed inset-x-0 bottom-14 z-20 border-t border-slate-200 bg-white/95 p-4 backdrop-blur">
          <div className="mx-auto max-w-2xl space-y-3">
            <div className="flex flex-wrap gap-2">
              {PAYMENTS.map(([k, label]) => (
                <button key={k} onClick={() => setPayment(k)}
                  className={`flex-1 rounded-lg border px-3 py-2 text-sm font-semibold ${payment === k ? 'border-brand-600 bg-brand-600 text-white' : 'border-slate-300 bg-white text-slate-700'}`}>
                  {label}
                </button>
              ))}
            </div>

            {payment === 'TICKET_CADEAU' && (
              <div className="space-y-2 rounded-xl border border-slate-200 bg-slate-50 p-3">
                <Button variant="secondary" className="w-full" onClick={() => setTicketScan(true)}>＋ Scanner un ticket</Button>
                {tickets.map((t) => (
                  <div key={t.code} className="flex items-center justify-between rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm">
                    <span>{t.issuer} · <b>{money(t.value)}</b> <span className="text-slate-400">{maskCode(t.code)}</span> {isExpired(t.expiry) && '⚠️'}</span>
                    <button onClick={() => removeTicket(t.code)} className="text-rose-600">Retirer</button>
                  </div>
                ))}
                <div className="flex justify-between text-sm">
                  <span>Total tickets : <b>{money(ticketsTotal)}</b></span>
                  <span>Reste : <b className={reste > 0 ? 'text-rose-600' : 'text-emerald-600'}>{money(reste)}</b></span>
                </div>
                {surplus && <p className="text-xs font-medium text-rose-600">⚠️ Le surplus des tickets n'est pas remboursé.</p>}
                {reste > 0 && (
                  <div className="flex items-center gap-2 text-sm">
                    <span className="text-slate-500">Reste en :</span>
                    {['ESPECES', 'CARTE'].map((m) => (
                      <button key={m} onClick={() => setRemainderMethod(m)}
                        className={`rounded-lg border px-3 py-1.5 font-semibold ${remainderMethod === m ? 'border-brand-600 bg-brand-600 text-white' : 'border-slate-300 bg-white text-slate-700'}`}>
                        {m === 'ESPECES' ? 'Espèces' : 'Carte'}
                      </button>
                    ))}
                  </div>
                )}
              </div>
            )}

            <div className="flex items-center gap-3">
              <Input value={discount} onChange={(e) => setDiscount(e.target.value)} placeholder="Remise (montant)" inputMode="decimal" className="max-w-[160px]" />
              <div className="ml-auto text-right">
                <div className="text-xs text-slate-400">Sous-total indicatif</div>
                <div className="text-xl font-extrabold text-slate-800">{money(total)}</div>
              </div>
            </div>

            <div className="flex gap-2">
              <Button className="flex-1" disabled={submitting} onClick={validate}>{submitting ? 'Validation…' : 'Valider la vente'}</Button>
              <Button variant="secondary" disabled={submitting}
                onClick={() => setReserve({ name: '', phone: '', deposit: '', method: 'ESPECES', duration: '' })}>📅 Réserver</Button>
            </div>
            <p className="text-center text-xs italic text-slate-400">Le total définitif est calculé par le serveur.</p>
          </div>
        </div>
      )}

      {/* Scanners */}
      <QrScanner open={scanOpen} onClose={() => setScanOpen(false)} onDetected={onScanned} title="Scanner un produit" />
      <QrScanner open={ticketScan} onClose={() => setTicketScan(false)} onDetected={onTicketScanned} title="Scanner un ticket cadeau" />

      {/* Sélecteur de variante */}
      {picker && (
        <Sheet onClose={() => setPicker(null)} title={picker.name} subtitle="Choisissez la déclinaison">
          <div className="max-h-80 space-y-2 overflow-y-auto">
            {picker.variants.map((v) => (
              <button key={v.id} disabled={v.quantity <= 0} onClick={() => pickVariant(v)}
                className="flex w-full items-center gap-3 rounded-lg border border-slate-200 px-3 py-2.5 text-left disabled:opacity-40">
                <span className="h-4 w-4 rounded-full border border-slate-300" style={{ backgroundColor: v.colorHex || '#fff' }} />
                <span className="flex-1 font-medium text-slate-700">{v.colorName} · {v.size}</span>
                <span className={`text-xs font-semibold ${v.quantity <= 0 ? 'text-rose-600' : 'text-emerald-600'}`}>{v.quantity <= 0 ? 'Rupture' : `Stock ${v.quantity}`}</span>
              </button>
            ))}
          </div>
        </Sheet>
      )}

      {/* Détail ticket cadeau scanné */}
      {ticketForm && (
        <Sheet onClose={() => setTicketForm(null)} title="Ticket cadeau" subtitle={maskCode(ticketForm.code)}>
          <Label>Émetteur</Label>
          <Chips options={ISSUERS.map((i) => [i, i])} value={ticketForm.issuer} onChange={(issuer) => setTicketForm((f) => ({ ...f, issuer }))} />
          <Label>Valeur (DT)</Label>
          <Chips options={DENOMINATIONS.map((v) => [v, String(v)])} value={ticketForm.value} onChange={(value) => setTicketForm((f) => ({ ...f, value }))} />
          <Label>Expiration (AAAA-MM-JJ, optionnel)</Label>
          <Input value={ticketForm.expiry} onChange={(e) => setTicketForm((f) => ({ ...f, expiry: e.target.value }))} placeholder="2026-12-31" />
          {isExpired(ticketForm.expiry) && <p className="text-xs font-medium text-rose-600">⚠️ Date dépassée — ticket refusé.</p>}
          <Button className="mt-3 w-full" onClick={addTicket}>Ajouter le ticket</Button>
        </Sheet>
      )}

      {/* Réservation (acompte) */}
      {reserve && (
        <Sheet onClose={() => setReserve(null)} title="Réserver (acompte)" subtitle="Le produit est retenu ; total calculé par le serveur.">
          <Label>Nom du client *</Label>
          <Input value={reserve.name} onChange={(e) => setReserve((r) => ({ ...r, name: e.target.value }))} placeholder="Nom et prénom" />
          <Label>Téléphone (optionnel)</Label>
          <Input value={reserve.phone} onChange={(e) => setReserve((r) => ({ ...r, phone: e.target.value }))} placeholder="20 123 456" inputMode="tel" />
          <Label>Acompte maintenant (optionnel)</Label>
          <Input value={reserve.deposit} onChange={(e) => setReserve((r) => ({ ...r, deposit: e.target.value }))} placeholder="0.00" inputMode="decimal" />
          <Label>Acompte en</Label>
          <Chips options={[['ESPECES', 'Espèces'], ['CARTE', 'Carte']]} value={reserve.method} onChange={(method) => setReserve((r) => ({ ...r, method }))} />
          <Label>Durée en jours (optionnel, défaut 30)</Label>
          <Input value={reserve.duration} onChange={(e) => setReserve((r) => ({ ...r, duration: e.target.value }))} placeholder="30" inputMode="numeric" />
          <Button className="mt-3 w-full" disabled={submitting} onClick={submitReserve}>{submitting ? 'Création…' : 'Créer la réservation'}</Button>
        </Sheet>
      )}

      {/* Reçu */}
      {receipt && (
        <Sheet onClose={() => setReceipt(null)} title={`Vente #${receipt.id}`} subtitle="Vente enregistrée ✓">
          <ul className="divide-y divide-slate-100">
            {receipt.items.map((it) => (
              <li key={it.id} className="flex justify-between py-2 text-sm">
                <span className="min-w-0 truncate">{it.productName} · {it.colorName}/{it.size} × {it.quantity}</span>
                <span className="ml-2 shrink-0 font-medium">{money(it.totalPrice)}</span>
              </li>
            ))}
          </ul>
          <div className="mt-2 space-y-1 border-t border-slate-200 pt-2 text-sm">
            {Number(receipt.discount) > 0 && <Row label="Remise" value={`- ${money(receipt.discount)}`} />}
            <Row label="Total" value={money(receipt.totalAmount)} strong />
            {Number(receipt.change) > 0 && <Row label="Rendu monnaie" value={money(receipt.change)} />}
          </div>
          <Button className="mt-4 w-full" onClick={() => setReceipt(null)}>Nouvelle vente</Button>
        </Sheet>
      )}
    </div>
  );
}

// --------------------------------- petits helpers UI ---------------------------------
function Sheet({ title, subtitle, onClose, children }) {
  return (
    <div className="fixed inset-0 z-50 flex items-end justify-center bg-black/40 sm:items-center" onClick={onClose}>
      <div className="w-full max-w-lg rounded-t-2xl bg-white p-5 sm:rounded-2xl" onClick={(e) => e.stopPropagation()}>
        <div className="mb-3">
          <h3 className="text-lg font-extrabold text-slate-800">{title}</h3>
          {subtitle && <p className="text-sm text-slate-400">{subtitle}</p>}
        </div>
        {children}
        <button onClick={onClose} className="mt-3 w-full rounded-xl border border-slate-300 py-2.5 text-sm font-semibold text-slate-600">Fermer</button>
      </div>
    </div>
  );
}
function Label({ children }) { return <p className="mb-1 mt-3 text-sm font-semibold text-slate-500">{children}</p>; }
function Chips({ options, value, onChange }) {
  return (
    <div className="flex flex-wrap gap-2">
      {options.map(([val, label]) => (
        <button key={val} onClick={() => onChange(val)}
          className={`rounded-lg border px-4 py-1.5 text-sm font-semibold ${value === val ? 'border-brand-600 bg-brand-600 text-white' : 'border-slate-300 bg-white text-slate-700'}`}>
          {label}
        </button>
      ))}
    </div>
  );
}
function Row({ label, value, strong }) {
  return (
    <div className="flex justify-between">
      <span className={strong ? 'font-semibold text-slate-700' : 'text-slate-500'}>{label}</span>
      <span className={strong ? 'text-lg font-extrabold text-slate-800' : 'font-medium text-slate-700'}>{value}</span>
    </div>
  );
}
