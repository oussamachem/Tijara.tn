import { useEffect, useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { debtsApi, suppliersApi, productsApi } from '../api/endpoints.js';
import { apiError } from '../api/client.js';
import { Button, Field, Input, Textarea, Select, Card, Badge, Modal, Alert, Spinner, Pagination } from '../components/ui.jsx';
import { formatMoney, formatDate } from '../utils/format.js';

const PAGE_SIZE = 10;
const STATUS = {
  PAID: { label: 'Payée', color: 'green' },
  PARTIAL: { label: 'Partiellement payée', color: 'amber' },
  UNPAID: { label: 'Non payée', color: 'red' },
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

export default function SupplierDebts() {
  const navigate = useNavigate();
  const [dash, setDash] = useState(null);
  const [filters, setFilters] = useState({ supplierId: '', status: '', from: '', to: '' });
  const [page, setPage] = useState(0);
  const [data, setData] = useState({ content: [], totalPages: 0, totalElements: 0 });
  const [suppliers, setSuppliers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [createOpen, setCreateOpen] = useState(false);
  const [suppliersOpen, setSuppliersOpen] = useState(false);

  const loadDash = () => debtsApi.dashboard().then((r) => setDash(r.data)).catch(() => {});
  const loadSuppliers = () => suppliersApi.list().then((r) => setSuppliers(r.data)).catch(() => {});
  useEffect(() => { loadSuppliers(); loadDash(); }, []);

  const load = useCallback(async () => {
    setLoading(true); setError('');
    try {
      const params = { page, size: PAGE_SIZE };
      if (filters.supplierId) params.supplierId = filters.supplierId;
      if (filters.status) params.status = filters.status;
      if (filters.from) params.from = filters.from;
      if (filters.to) params.to = filters.to;
      const { data: d } = await debtsApi.list(params);
      setData(d);
    } catch (err) { setError(apiError(err)); } finally { setLoading(false); }
  }, [page, filters]);
  useEffect(() => { load(); }, [load]);

  const submitFilters = (e) => { e.preventDefault(); setPage(0); load(); };
  const resetFilters = () => { setFilters({ supplierId: '', status: '', from: '', to: '' }); setPage(0); };
  const onCreated = () => { setCreateOpen(false); setNotice('Dette enregistrée.'); load(); loadDash(); };

  return (
    <div className="space-y-5">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <h2 className="text-xl font-bold text-slate-800">Dettes fournisseurs</h2>
        <div className="flex flex-col gap-2 sm:flex-row">
          <Button variant="secondary" onClick={() => setSuppliersOpen(true)} className="w-full sm:w-auto">Fournisseurs</Button>
          <Button onClick={() => setCreateOpen(true)} className="w-full sm:w-auto">+ Nouvelle dette</Button>
        </div>
      </div>

      <Alert type="success" onClose={() => setNotice('')}>{notice}</Alert>
      <Alert type="error" onClose={() => setError('')}>{error}</Alert>

      {dash && (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <StatCard label="Dettes" value={dash.debtsCount} />
          <StatCard label="Total dû" value={formatMoney(dash.totalAmount)} />
          <StatCard label="Déjà payé" value={formatMoney(dash.paid)} accent="text-green-700" />
          <StatCard label="Restant à payer" value={formatMoney(dash.outstanding)}
            accent={Number(dash.outstanding) > 0 ? 'text-red-600' : 'text-slate-800'} />
        </div>
      )}

      <Card>
        <form onSubmit={submitFilters} className="grid grid-cols-1 gap-3 md:grid-cols-5">
          <Field label="Fournisseur">
            <Select value={filters.supplierId} onChange={(e) => setFilters({ ...filters, supplierId: e.target.value })}>
              <option value="">Tous</option>
              {suppliers.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
            </Select>
          </Field>
          <Field label="Statut">
            <Select value={filters.status} onChange={(e) => setFilters({ ...filters, status: e.target.value })}>
              <option value="">Tous</option>
              <option value="UNPAID">Non payée</option>
              <option value="PARTIAL">Partiellement payée</option>
              <option value="PAID">Payée</option>
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

      <Card>
        {loading ? (
          <div className="flex justify-center py-10"><Spinner className="h-7 w-7" /></div>
        ) : data.content.length === 0 ? (
          <p className="py-6 text-center text-sm text-slate-400">Aucune dette.</p>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="text-left text-slate-500">
                  <th className="pb-2">Fournisseur</th>
                  <th className="pb-2 text-right">Total</th>
                  <th className="pb-2 text-right">Payé</th>
                  <th className="pb-2 text-right">Reste</th>
                  <th className="pb-2">Statut</th>
                  <th className="pb-2">Échéance</th>
                  <th className="pb-2"></th>
                </tr>
              </thead>
              <tbody>
                {data.content.map((d) => (
                  <tr key={d.id} className="cursor-pointer border-t border-slate-100 hover:bg-slate-50"
                    onClick={() => navigate(`/dettes/${d.id}`)}>
                    <td className="py-2 font-medium">{d.supplierName}</td>
                    <td className="py-2 text-right">{formatMoney(d.total)}</td>
                    <td className="py-2 text-right text-green-700">{formatMoney(d.paid)}</td>
                    <td className="py-2 text-right font-medium">{formatMoney(d.remaining)}</td>
                    <td className="py-2"><StatusBadge status={d.status} /></td>
                    <td className="py-2 text-slate-500">{d.dueDate ? formatDate(d.dueDate) : '—'}</td>
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
        <CreateDebtModal suppliers={suppliers} onClose={() => setCreateOpen(false)} onCreated={onCreated} />
      )}
      {suppliersOpen && (
        <SuppliersModal suppliers={suppliers} onClose={() => setSuppliersOpen(false)}
          onChanged={() => { loadSuppliers(); }} />
      )}
    </div>
  );
}

// --------------------------- Création de dette ---------------------------
function CreateDebtModal({ suppliers, onClose, onCreated }) {
  const [products, setProducts] = useState([]);
  const [mode, setMode] = useState('existing');
  const [supplierId, setSupplierId] = useState('');
  const [newSupplier, setNewSupplier] = useState({ name: '', phone: '', address: '' });
  const [form, setForm] = useState({ totalAmount: '', dueDate: '', invoiceReference: '', description: '', productId: '', downPayment: 0 });
  const [saving, setSaving] = useState(false);
  const [err, setErr] = useState('');

  useEffect(() => {
    productsApi.search({ size: 200, sort: 'name,asc' }).then((r) => setProducts(r.data.content)).catch(() => {});
  }, []);

  const submit = async () => {
    if (!form.totalAmount || Number(form.totalAmount) <= 0) { setErr('Le montant total est obligatoire.'); return; }
    if (mode === 'existing' && !supplierId) { setErr('Choisissez un fournisseur.'); return; }
    if (mode === 'new' && !newSupplier.name.trim()) { setErr('Nom du fournisseur requis.'); return; }
    setSaving(true); setErr('');
    try {
      const payload = {
        totalAmount: Number(form.totalAmount),
        dueDate: form.dueDate || null,
        invoiceReference: form.invoiceReference || null,
        description: form.description || null,
        productId: form.productId ? Number(form.productId) : null,
        downPayment: Number(form.downPayment) || 0,
        ...(mode === 'existing' ? { supplierId: Number(supplierId) } : { newSupplier }),
      };
      await debtsApi.create(payload);
      onCreated();
    } catch (e) { setErr(apiError(e)); } finally { setSaving(false); }
  };

  return (
    <Modal open onClose={onClose} title="Nouvelle dette fournisseur" wide
      footer={<>
        <Button variant="secondary" onClick={onClose}>Annuler</Button>
        <Button onClick={submit} disabled={saving}>{saving ? 'Enregistrement…' : 'Enregistrer'}</Button>
      </>}>
      <div className="space-y-4">
        <Alert type="error">{err}</Alert>

        <div>
          <div className="mb-1 text-sm font-medium text-slate-700">Fournisseur</div>
          <div className="mb-2 flex gap-3 text-sm">
            <label className="flex items-center gap-1"><input type="radio" checked={mode === 'existing'} onChange={() => setMode('existing')} /> Existant</label>
            <label className="flex items-center gap-1"><input type="radio" checked={mode === 'new'} onChange={() => setMode('new')} /> Nouveau</label>
          </div>
          {mode === 'existing' ? (
            <Select value={supplierId} onChange={(e) => setSupplierId(e.target.value)}>
              <option value="">— Choisir —</option>
              {suppliers.map((s) => <option key={s.id} value={s.id}>{s.name} {s.phone ? `(${s.phone})` : ''}</option>)}
            </Select>
          ) : (
            <div className="grid grid-cols-1 gap-2 md:grid-cols-3">
              <Input placeholder="Nom *" value={newSupplier.name} onChange={(e) => setNewSupplier({ ...newSupplier, name: e.target.value })} />
              <Input placeholder="Téléphone" value={newSupplier.phone} onChange={(e) => setNewSupplier({ ...newSupplier, phone: e.target.value })} />
              <Input placeholder="Adresse" value={newSupplier.address} onChange={(e) => setNewSupplier({ ...newSupplier, address: e.target.value })} />
            </div>
          )}
        </div>

        <div className="grid grid-cols-1 gap-3 md:grid-cols-3">
          <Field label="Montant total (facture)" required><Input type="number" step="0.01" min="0" value={form.totalAmount} onChange={(e) => setForm({ ...form, totalAmount: e.target.value })} required /></Field>
          <Field label="Acompte initial"><Input type="number" step="0.01" min="0" value={form.downPayment} onChange={(e) => setForm({ ...form, downPayment: e.target.value })} /></Field>
          <Field label="Échéance (optionnelle)"><Input type="date" value={form.dueDate} onChange={(e) => setForm({ ...form, dueDate: e.target.value })} /></Field>
          <Field label="Référence facture"><Input value={form.invoiceReference} onChange={(e) => setForm({ ...form, invoiceReference: e.target.value })} /></Field>
          <Field label="Produit lié (descriptif, optionnel)">
            <Select value={form.productId} onChange={(e) => setForm({ ...form, productId: e.target.value })}>
              <option value="">— Aucun —</option>
              {products.map((p) => <option key={p.id} value={p.id}>{p.name} ({p.reference})</option>)}
            </Select>
          </Field>
        </div>
        <Field label="Description"><Textarea rows={2} value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} /></Field>
        <p className="text-xs text-slate-400">Registre financier : aucune incidence sur le stock ni le chiffre d'affaires.</p>
      </div>
    </Modal>
  );
}

// --------------------------- Gestion fournisseurs ---------------------------
function SuppliersModal({ suppliers, onClose, onChanged }) {
  const [form, setForm] = useState({ id: null, name: '', phone: '', address: '' });
  const [err, setErr] = useState('');
  const reset = () => setForm({ id: null, name: '', phone: '', address: '' });

  const save = async () => {
    if (!form.name.trim()) { setErr('Nom requis.'); return; }
    setErr('');
    try {
      const payload = { name: form.name, phone: form.phone, address: form.address };
      if (form.id) await suppliersApi.update(form.id, payload);
      else await suppliersApi.create(payload);
      reset(); onChanged();
    } catch (e) { setErr(apiError(e)); }
  };

  return (
    <Modal open onClose={onClose} title="Fournisseurs"
      footer={<Button variant="secondary" onClick={onClose}>Fermer</Button>}>
      <div className="space-y-3">
        <Alert type="error">{err}</Alert>
        <div className="grid grid-cols-1 gap-2 md:grid-cols-4">
          <Input placeholder="Nom *" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
          <Input placeholder="Téléphone" value={form.phone} onChange={(e) => setForm({ ...form, phone: e.target.value })} />
          <Input placeholder="Adresse" value={form.address} onChange={(e) => setForm({ ...form, address: e.target.value })} />
          <div className="flex gap-1">
            <Button onClick={save}>{form.id ? 'Modifier' : 'Ajouter'}</Button>
            {form.id && <Button variant="secondary" onClick={reset}>Annuler</Button>}
          </div>
        </div>
        <table className="w-full text-sm">
          <tbody>
            {suppliers.map((s) => (
              <tr key={s.id} className="border-t border-slate-100">
                <td className="py-2 font-medium">{s.name}</td>
                <td className="py-2 text-slate-500">{s.phone || '—'}</td>
                <td className="py-2 text-slate-500">{s.address || '—'}</td>
                <td className="py-2 text-right">
                  <Button variant="ghost" onClick={() => setForm({ id: s.id, name: s.name, phone: s.phone || '', address: s.address || '' })}>✏️</Button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </Modal>
  );
}
