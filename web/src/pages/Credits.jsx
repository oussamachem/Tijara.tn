import { useEffect, useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { creditsApi, customersApi, productsApi } from '../api/endpoints.js';
import { apiError } from '../api/client.js';
import { Button, Field, Input, Select, Card, Badge, Modal, Alert, Spinner, Pagination } from '../components/ui.jsx';
import { formatMoney, formatDate } from '../utils/format.js';

const PAGE_SIZE = 10;
const STATUS = {
  PAID: { label: 'Payé', color: 'green' },
  PARTIAL: { label: 'Partiel', color: 'amber' },
  UNPAID: { label: 'Non payé', color: 'red' },
  CANCELLED: { label: 'Annulé', color: 'slate' },
};

function StatusBadge({ status }) {
  const s = STATUS[status] || { label: status, color: 'slate' };
  return <Badge color={s.color}>{s.label}</Badge>;
}

function StatCard({ label, value, accent = 'text-slate-800' }) {
  return (
    <div className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
      <div className="text-sm text-slate-500">{label}</div>
      <div className={`mt-1 text-2xl font-bold ${accent}`}>{value}</div>
    </div>
  );
}

export default function Credits() {
  const navigate = useNavigate();
  const [dash, setDash] = useState(null);
  const [filters, setFilters] = useState({ customerId: '', status: '', from: '', to: '' });
  const [page, setPage] = useState(0);
  const [data, setData] = useState({ content: [], totalPages: 0, totalElements: 0 });
  const [customers, setCustomers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [createOpen, setCreateOpen] = useState(false);

  const loadDash = () => creditsApi.dashboard().then((r) => setDash(r.data)).catch(() => {});
  useEffect(() => {
    customersApi.list().then((r) => setCustomers(r.data)).catch(() => {});
    loadDash();
  }, []);

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const params = { page, size: PAGE_SIZE };
      if (filters.customerId) params.customerId = filters.customerId;
      if (filters.status) params.status = filters.status;
      if (filters.from) params.from = filters.from;
      if (filters.to) params.to = filters.to;
      const { data: d } = await creditsApi.list(params);
      setData(d);
    } catch (err) {
      setError(apiError(err));
    } finally {
      setLoading(false);
    }
  }, [page, filters]);

  useEffect(() => { load(); }, [load]);

  const submitFilters = (e) => { e.preventDefault(); setPage(0); load(); };
  const resetFilters = () => { setFilters({ customerId: '', status: '', from: '', to: '' }); setPage(0); };

  const onCreated = () => { setCreateOpen(false); setNotice('Crédit créé.'); load(); loadDash(); };

  return (
    <div className="space-y-5">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <h2 className="text-xl font-bold text-slate-800">Crédits</h2>
        <Button onClick={() => setCreateOpen(true)} className="w-full sm:w-auto">+ Nouveau crédit</Button>
      </div>

      <Alert type="success" onClose={() => setNotice('')}>{notice}</Alert>
      <Alert type="error" onClose={() => setError('')}>{error}</Alert>

      {/* Cartes trésorerie */}
      {dash && (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <StatCard label="Crédits actifs" value={dash.creditsCount} />
          <StatCard label="Total vendu à crédit" value={formatMoney(dash.totalAmount)} />
          <StatCard label="Déjà encaissé" value={formatMoney(dash.collected)} accent="text-green-700" />
          <StatCard label="Restant à encaisser" value={formatMoney(dash.outstanding)}
            accent={Number(dash.outstanding) > 0 ? 'text-amber-600' : 'text-slate-800'} />
        </div>
      )}

      {/* Filtres */}
      <Card>
        <form onSubmit={submitFilters} className="grid grid-cols-1 gap-3 md:grid-cols-5">
          <Field label="Client">
            <Select value={filters.customerId} onChange={(e) => setFilters({ ...filters, customerId: e.target.value })}>
              <option value="">Tous</option>
              {customers.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
            </Select>
          </Field>
          <Field label="Statut">
            <Select value={filters.status} onChange={(e) => setFilters({ ...filters, status: e.target.value })}>
              <option value="">Tous</option>
              <option value="UNPAID">Non payé</option>
              <option value="PARTIAL">Partiel</option>
              <option value="PAID">Payé</option>
              <option value="CANCELLED">Annulé</option>
            </Select>
          </Field>
          <Field label="Du"><Input type="date" value={filters.from} onChange={(e) => setFilters({ ...filters, from: e.target.value })} /></Field>
          <Field label="Au"><Input type="date" value={filters.to} onChange={(e) => setFilters({ ...filters, to: e.target.value })} /></Field>
          <div className="flex items-end gap-2">
            <Button type="submit">Filtrer</Button>
            <Button type="button" variant="secondary" onClick={resetFilters}>Réinitialiser</Button>
          </div>
        </form>
      </Card>

      {/* Liste */}
      <Card>
        {loading ? (
          <div className="flex justify-center py-10"><Spinner className="h-7 w-7" /></div>
        ) : data.content.length === 0 ? (
          <p className="py-6 text-center text-sm text-slate-400">Aucun crédit.</p>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="text-left text-slate-500">
                  <th className="pb-2">Client</th>
                  <th className="pb-2 text-right">Total</th>
                  <th className="pb-2 text-right">Payé</th>
                  <th className="pb-2 text-right">Reste</th>
                  <th className="pb-2">Statut</th>
                  <th className="pb-2">Échéance</th>
                  <th className="pb-2"></th>
                </tr>
              </thead>
              <tbody>
                {data.content.map((c) => (
                  <tr key={c.id} className="cursor-pointer border-t border-slate-100 hover:bg-slate-50"
                    onClick={() => navigate(`/credits/${c.id}`)}>
                    <td className="py-2 font-medium">{c.customerName}</td>
                    <td className="py-2 text-right">{formatMoney(c.total)}</td>
                    <td className="py-2 text-right text-green-700">{formatMoney(c.paid)}</td>
                    <td className="py-2 text-right font-medium">{formatMoney(c.remaining)}</td>
                    <td className="py-2"><StatusBadge status={c.status} /></td>
                    <td className="py-2 text-slate-500">{c.dueDate ? formatDate(c.dueDate) : '—'}</td>
                    <td className="py-2 text-right text-brand-600">Détail →</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
        <Pagination page={page} totalPages={data.totalPages} onChange={setPage} />
      </Card>

      {createOpen && (
        <CreateCreditModal customers={customers} onClose={() => setCreateOpen(false)} onCreated={onCreated}
          onCustomersChanged={() => customersApi.list().then((r) => setCustomers(r.data)).catch(() => {})} />
      )}
    </div>
  );
}

// --------------------------- Modale de création ---------------------------
function CreateCreditModal({ customers, onClose, onCreated }) {
  const [products, setProducts] = useState([]);
  const [custMode, setCustMode] = useState('existing');
  const [customerId, setCustomerId] = useState('');
  const [newCustomer, setNewCustomer] = useState({ name: '', phone: '', address: '' });
  const [lines, setLines] = useState([]); // {variantId, label, unitPrice, quantity}
  const [pick, setPick] = useState({ productId: '', variantId: '', quantity: 1 });
  const [downPayment, setDownPayment] = useState(0);
  const [dueDate, setDueDate] = useState('');
  const [paymentMethod, setPaymentMethod] = useState('ESPECES');
  const [saving, setSaving] = useState(false);
  const [err, setErr] = useState('');

  useEffect(() => {
    productsApi.search({ size: 200, sort: 'name,asc' }).then((r) => setProducts(r.data.content)).catch(() => {});
  }, []);

  const product = products.find((p) => String(p.id) === String(pick.productId));
  const total = lines.reduce((s, l) => s + l.unitPrice * l.quantity, 0);

  const addLine = () => {
    if (!product || !pick.variantId) return;
    const v = product.variants.find((x) => String(x.id) === String(pick.variantId));
    if (!v) return;
    setLines([...lines, {
      variantId: v.id,
      label: `${product.name} — ${v.colorName} · ${v.size} (${v.reference})`,
      unitPrice: Number(product.salePrice),
      quantity: Number(pick.quantity) || 1,
    }]);
    setPick({ productId: '', variantId: '', quantity: 1 });
  };
  const removeLine = (i) => setLines(lines.filter((_, idx) => idx !== i));

  const submit = async () => {
    if (lines.length === 0) { setErr('Ajoutez au moins un article.'); return; }
    if (custMode === 'existing' && !customerId) { setErr('Choisissez un client.'); return; }
    if (custMode === 'new' && !newCustomer.name.trim()) { setErr('Nom du client requis.'); return; }
    setSaving(true); setErr('');
    try {
      const payload = {
        items: lines.map((l) => ({ variantId: l.variantId, quantity: l.quantity })),
        paymentMethod,
        downPayment: Number(downPayment) || 0,
        dueDate: dueDate || null,
        ...(custMode === 'existing' ? { customerId: Number(customerId) } : { newCustomer }),
      };
      await creditsApi.create(payload);
      onCreated();
    } catch (e) {
      setErr(apiError(e));
    } finally {
      setSaving(false);
    }
  };

  return (
    <Modal open onClose={onClose} title="Nouveau crédit" wide
      footer={<>
        <Button variant="secondary" onClick={onClose}>Annuler</Button>
        <Button onClick={submit} disabled={saving}>{saving ? 'Création…' : 'Créer le crédit'}</Button>
      </>}>
      <div className="space-y-4">
        <Alert type="error">{err}</Alert>

        {/* Client */}
        <div>
          <div className="mb-1 text-sm font-medium text-slate-700">Client</div>
          <div className="mb-2 flex gap-2 text-sm">
            <label className="flex items-center gap-1"><input type="radio" checked={custMode === 'existing'} onChange={() => setCustMode('existing')} /> Existant</label>
            <label className="flex items-center gap-1"><input type="radio" checked={custMode === 'new'} onChange={() => setCustMode('new')} /> Nouveau</label>
          </div>
          {custMode === 'existing' ? (
            <Select value={customerId} onChange={(e) => setCustomerId(e.target.value)}>
              <option value="">— Choisir —</option>
              {customers.map((c) => <option key={c.id} value={c.id}>{c.name} {c.phone ? `(${c.phone})` : ''}</option>)}
            </Select>
          ) : (
            <div className="grid grid-cols-1 gap-2 md:grid-cols-3">
              <Input placeholder="Nom *" value={newCustomer.name} onChange={(e) => setNewCustomer({ ...newCustomer, name: e.target.value })} />
              <Input placeholder="Téléphone" value={newCustomer.phone} onChange={(e) => setNewCustomer({ ...newCustomer, phone: e.target.value })} />
              <Input placeholder="Adresse" value={newCustomer.address} onChange={(e) => setNewCustomer({ ...newCustomer, address: e.target.value })} />
            </div>
          )}
        </div>

        {/* Articles */}
        <div>
          <div className="mb-1 text-sm font-medium text-slate-700">Articles vendus</div>
          <div className="flex flex-wrap items-end gap-2 rounded-lg border border-dashed border-slate-300 p-2">
            <Field label="Produit">
              <Select className="w-44" value={pick.productId} onChange={(e) => setPick({ productId: e.target.value, variantId: '', quantity: 1 })}>
                <option value="">—</option>
                {products.map((p) => <option key={p.id} value={p.id}>{p.name}</option>)}
              </Select>
            </Field>
            <Field label="Variante">
              <Select className="w-44" value={pick.variantId} onChange={(e) => setPick({ ...pick, variantId: e.target.value })} disabled={!product}>
                <option value="">—</option>
                {product?.variants.map((v) => (
                  <option key={v.id} value={v.id} disabled={v.quantity <= 0}>
                    {v.colorName} · {v.size} (stock {v.quantity})
                  </option>
                ))}
              </Select>
            </Field>
            <Field label="Qté"><Input type="number" min="1" className="w-20" value={pick.quantity} onChange={(e) => setPick({ ...pick, quantity: e.target.value })} /></Field>
            <Button type="button" variant="secondary" disabled={!pick.variantId} onClick={addLine}>+ Ajouter</Button>
          </div>
          {lines.length > 0 && (
            <table className="mt-2 w-full text-sm">
              <tbody>
                {lines.map((l, i) => (
                  <tr key={i} className="border-t border-slate-100">
                    <td className="py-1">{l.label}</td>
                    <td className="py-1 text-right text-slate-500">{l.quantity} × {formatMoney(l.unitPrice)}</td>
                    <td className="py-1 text-right font-medium">{formatMoney(l.unitPrice * l.quantity)}</td>
                    <td className="py-1 text-right"><button type="button" className="text-red-500" onClick={() => removeLine(i)}>🗑️</button></td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
          <div className="mt-1 text-right text-sm font-semibold text-slate-700">Total (dérivé) : {formatMoney(total)}</div>
        </div>

        {/* Acompte / échéance */}
        <div className="grid grid-cols-1 gap-3 md:grid-cols-3">
          <Field label="Acompte initial"><Input type="number" step="0.01" min="0" value={downPayment} onChange={(e) => setDownPayment(e.target.value)} /></Field>
          <Field label="Mode (acompte)">
            <Select value={paymentMethod} onChange={(e) => setPaymentMethod(e.target.value)}>
              <option value="ESPECES">Espèces</option>
              <option value="CARTE">Carte</option>
              <option value="MIXTE">Mixte</option>
            </Select>
          </Field>
          <Field label="Échéance (optionnelle)"><Input type="date" value={dueDate} onChange={(e) => setDueDate(e.target.value)} /></Field>
        </div>
      </div>
    </Modal>
  );
}
