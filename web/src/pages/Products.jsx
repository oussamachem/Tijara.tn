import { useEffect, useState, useCallback } from 'react';
import { productsApi, categoriesApi } from '../api/endpoints.js';
import { apiError } from '../api/client.js';
import { Button, Field, Input, Textarea, Select, Card, Badge, Modal, Alert, Spinner, Pagination, ConfirmDialog } from '../components/ui.jsx';
import { formatMoney } from '../utils/format.js';

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';
const PAGE_SIZE = 10;

const emptyForm = {
  reference: '', name: '', description: '', categoryId: '',
  size: '', color: '', purchasePrice: '', salePrice: '', quantity: 0, seuilAlerte: 0,
};

// Échappement HTML pour l'injection dans la fenêtre d'impression.
function escapeHtml(value) {
  return String(value ?? '').replace(/[&<>"']/g, (c) =>
    ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c])
  );
}

export default function Products() {
  const [filters, setFilters] = useState({ name: '', reference: '', categoryId: '' });
  const [page, setPage] = useState(0);
  const [pageData, setPageData] = useState({ content: [], totalPages: 0, totalElements: 0 });
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

  // Modale de formulaire (création / édition)
  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState(null); // produit en cours d'édition (ou null = création)
  const [form, setForm] = useState(emptyForm);
  const [saving, setSaving] = useState(false);
  const [formError, setFormError] = useState('');

  // Modale QR Code
  const [qrProduct, setQrProduct] = useState(null);
  const [qrUrl, setQrUrl] = useState('');

  // Suppression
  const [toDelete, setToDelete] = useState(null);
  const [deleting, setDeleting] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const params = { page, size: PAGE_SIZE };
      if (filters.name) params.name = filters.name;
      if (filters.reference) params.reference = filters.reference;
      if (filters.categoryId) params.categoryId = filters.categoryId;
      const { data } = await productsApi.search(params);
      setPageData(data);
    } catch (err) {
      setError(apiError(err));
    } finally {
      setLoading(false);
    }
  }, [page, filters]);

  useEffect(() => { load(); }, [load]);

  useEffect(() => {
    categoriesApi.list().then((r) => setCategories(r.data)).catch(() => {});
  }, []);

  const submitFilters = (e) => {
    e.preventDefault();
    setPage(0);
    load();
  };

  const resetFilters = () => {
    setFilters({ name: '', reference: '', categoryId: '' });
    setPage(0);
  };

  // ---- Formulaire ----
  const openCreate = () => {
    setEditing(null);
    setForm(emptyForm);
    setFormError('');
    setFormOpen(true);
  };

  const openEdit = (p) => {
    setEditing(p);
    setForm({
      reference: p.reference, name: p.name, description: p.description || '',
      categoryId: p.categoryId || '', size: p.size || '', color: p.color || '',
      purchasePrice: p.purchasePrice, salePrice: p.salePrice,
      quantity: p.quantity, seuilAlerte: p.seuilAlerte,
    });
    setFormError('');
    setFormOpen(true);
  };

  const submitForm = async (e) => {
    e.preventDefault();
    setSaving(true);
    setFormError('');
    try {
      const payload = {
        ...form,
        categoryId: form.categoryId ? Number(form.categoryId) : null,
        purchasePrice: Number(form.purchasePrice),
        salePrice: Number(form.salePrice),
        quantity: Number(form.quantity),
        seuilAlerte: Number(form.seuilAlerte),
      };
      if (editing) {
        await productsApi.update(editing.id, payload);
        setNotice('Produit mis à jour.');
      } else {
        await productsApi.create(payload);
        setNotice('Produit créé (QR Code généré).');
      }
      setFormOpen(false);
      load();
    } catch (err) {
      setFormError(apiError(err));
    } finally {
      setSaving(false);
    }
  };

  const handleImage = async (e) => {
    const file = e.target.files?.[0];
    if (!file || !editing) return;
    try {
      const { data } = await productsApi.uploadImage(editing.id, file);
      setEditing(data);
      setNotice('Image mise à jour.');
      load();
    } catch (err) {
      setFormError(apiError(err));
    }
  };

  // ---- QR Code ----
  const openQr = async (p) => {
    setQrProduct(p);
    setQrUrl('');
    try {
      const { data } = await productsApi.qrCode(p.id);
      setQrUrl(URL.createObjectURL(data));
    } catch (err) {
      setError(apiError(err));
    }
  };

  const closeQr = () => {
    if (qrUrl) URL.revokeObjectURL(qrUrl);
    setQrUrl('');
    setQrProduct(null);
  };

  const printQr = () => {
    if (!qrUrl || !qrProduct) return;
    // Fenêtre ouverte SYNCHRONIQUEMENT dans le gestionnaire de clic -> non bloquée.
    const w = window.open('', '_blank', 'width=420,height=540');
    if (!w) {
      setError("La fenêtre d'impression a été bloquée par le navigateur (autorisez les pop-ups).");
      return;
    }
    const name = escapeHtml(qrProduct.name);
    const ref = escapeHtml(qrProduct.reference);
    // Impression déclenchée sur img.onload : on n'imprime jamais une page blanche.
    w.document.write(
      `<!doctype html><html><head><meta charset="utf-8"><title>QR ${ref}</title></head>
       <body style="text-align:center;font-family:sans-serif;padding:24px;margin:0">
         <h3 style="margin:0 0 4px">${name}</h3>
         <p style="margin:0 0 12px;color:#555">${ref}</p>
         <img src="${qrUrl}" style="width:260px" onload="window.focus();window.print();" />
       </body></html>`
    );
    w.document.close();
  };

  // ---- Suppression ----
  const confirmDelete = async () => {
    setDeleting(true);
    try {
      await productsApi.remove(toDelete.id);
      setNotice('Produit supprimé.');
      setToDelete(null);
      load();
    } catch (err) {
      setError(apiError(err));
      setToDelete(null);
    } finally {
      setDeleting(false);
    }
  };

  return (
    <div className="space-y-5">
      <div className="flex items-center justify-between">
        <h2 className="text-xl font-bold text-slate-800">Produits</h2>
        <Button onClick={openCreate}>+ Nouveau produit</Button>
      </div>

      <Alert type="success" onClose={() => setNotice('')}>{notice}</Alert>
      <Alert type="error" onClose={() => setError('')}>{error}</Alert>

      {/* Filtres */}
      <Card>
        <form onSubmit={submitFilters} className="grid grid-cols-1 gap-3 md:grid-cols-4">
          <Field label="Nom">
            <Input value={filters.name} onChange={(e) => setFilters({ ...filters, name: e.target.value })} placeholder="Rechercher…" />
          </Field>
          <Field label="Référence">
            <Input value={filters.reference} onChange={(e) => setFilters({ ...filters, reference: e.target.value })} />
          </Field>
          <Field label="Catégorie">
            <Select value={filters.categoryId} onChange={(e) => setFilters({ ...filters, categoryId: e.target.value })}>
              <option value="">Toutes</option>
              {categories.map((c) => (
                <option key={c.id} value={c.id}>{c.name}</option>
              ))}
            </Select>
          </Field>
          <div className="flex items-end gap-2">
            <Button type="submit">Filtrer</Button>
            <Button type="button" variant="secondary" onClick={resetFilters}>Réinitialiser</Button>
          </div>
        </form>
      </Card>

      {/* Tableau */}
      <Card>
        {loading ? (
          <div className="flex justify-center py-10"><Spinner className="h-7 w-7" /></div>
        ) : pageData.content.length === 0 ? (
          <p className="py-6 text-center text-sm text-slate-400">Aucun produit.</p>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="text-left text-slate-500">
                  <th className="pb-2">Référence</th>
                  <th className="pb-2">Nom</th>
                  <th className="pb-2">Catégorie</th>
                  <th className="pb-2 text-right">Prix vente</th>
                  <th className="pb-2 text-right">Stock</th>
                  <th className="pb-2 text-right">Actions</th>
                </tr>
              </thead>
              <tbody>
                {pageData.content.map((p) => (
                  <tr key={p.id} className="border-t border-slate-100">
                    <td className="py-2 font-mono text-xs">{p.reference}</td>
                    <td className="py-2">{p.name}</td>
                    <td className="py-2 text-slate-500">{p.categoryName || '—'}</td>
                    <td className="py-2 text-right">{formatMoney(p.salePrice)}</td>
                    <td className="py-2 text-right">
                      {p.lowStock ? <Badge color={p.quantity === 0 ? 'red' : 'amber'}>{p.quantity}</Badge> : p.quantity}
                    </td>
                    <td className="py-2">
                      <div className="flex justify-end gap-1">
                        <Button variant="ghost" onClick={() => openQr(p)} title="QR Code">🔳</Button>
                        <Button variant="ghost" onClick={() => openEdit(p)} title="Modifier">✏️</Button>
                        <Button variant="ghost" onClick={() => setToDelete(p)} title="Supprimer">🗑️</Button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
        <Pagination page={page} totalPages={pageData.totalPages} onChange={setPage} />
      </Card>

      {/* Modale formulaire */}
      <Modal
        open={formOpen}
        onClose={() => setFormOpen(false)}
        title={editing ? 'Modifier le produit' : 'Nouveau produit'}
        wide
        footer={
          <>
            <Button variant="secondary" onClick={() => setFormOpen(false)}>Annuler</Button>
            <Button onClick={submitForm} disabled={saving}>{saving ? 'Enregistrement…' : 'Enregistrer'}</Button>
          </>
        }
      >
        <form onSubmit={submitForm} className="grid grid-cols-1 gap-3 md:grid-cols-2">
          <Alert type="error">{formError}</Alert>
          <div className="md:col-span-2" />
          <Field label="Référence" required>
            <Input value={form.reference} onChange={(e) => setForm({ ...form, reference: e.target.value })} required />
          </Field>
          <Field label="Nom" required>
            <Input value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} required />
          </Field>
          <Field label="Catégorie">
            <Select value={form.categoryId} onChange={(e) => setForm({ ...form, categoryId: e.target.value })}>
              <option value="">— Aucune —</option>
              {categories.map((c) => (
                <option key={c.id} value={c.id}>{c.name}</option>
              ))}
            </Select>
          </Field>
          <Field label="Taille">
            <Input value={form.size} onChange={(e) => setForm({ ...form, size: e.target.value })} />
          </Field>
          <Field label="Couleur">
            <Input value={form.color} onChange={(e) => setForm({ ...form, color: e.target.value })} />
          </Field>
          <Field label="Prix d'achat" required>
            <Input type="number" step="0.01" min="0" value={form.purchasePrice} onChange={(e) => setForm({ ...form, purchasePrice: e.target.value })} required />
          </Field>
          <Field label="Prix de vente" required>
            <Input type="number" step="0.01" min="0" value={form.salePrice} onChange={(e) => setForm({ ...form, salePrice: e.target.value })} required />
          </Field>
          <Field label="Quantité" required>
            <Input type="number" min="0" value={form.quantity} onChange={(e) => setForm({ ...form, quantity: e.target.value })} required />
          </Field>
          <Field label="Seuil d'alerte" required>
            <Input type="number" min="0" value={form.seuilAlerte} onChange={(e) => setForm({ ...form, seuilAlerte: e.target.value })} required />
          </Field>
          <div className="md:col-span-2">
            <Field label="Description">
              <Textarea rows={2} value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} />
            </Field>
          </div>

          {editing && (
            <div className="md:col-span-2 rounded-lg border border-slate-200 p-3">
              <div className="mb-2 text-sm font-medium text-slate-600">Image du produit</div>
              <div className="flex items-center gap-4">
                {editing.imageUrl ? (
                  <img src={`${API_URL}${editing.imageUrl}`} alt="" className="h-20 w-20 rounded object-cover" />
                ) : (
                  <div className="flex h-20 w-20 items-center justify-center rounded bg-slate-100 text-xs text-slate-400">Aucune</div>
                )}
                <input type="file" accept="image/png,image/jpeg,image/webp" onChange={handleImage} className="text-sm" />
              </div>
            </div>
          )}
        </form>
      </Modal>

      {/* Modale QR */}
      <Modal
        open={!!qrProduct}
        onClose={closeQr}
        title={qrProduct ? `QR Code — ${qrProduct.reference}` : ''}
        footer={
          <>
            <Button variant="secondary" onClick={closeQr}>Fermer</Button>
            <Button onClick={printQr} disabled={!qrUrl}>🖨️ Imprimer</Button>
          </>
        }
      >
        <div className="flex flex-col items-center gap-2">
          {qrUrl ? <img src={qrUrl} alt="QR Code" className="h-60 w-60" /> : <Spinner className="h-7 w-7" />}
          <p className="text-sm text-slate-500">{qrProduct?.name}</p>
        </div>
      </Modal>

      <ConfirmDialog
        open={!!toDelete}
        title="Supprimer le produit"
        message={`Supprimer définitivement « ${toDelete?.name} » ?`}
        confirmLabel="Supprimer"
        onConfirm={confirmDelete}
        onCancel={() => setToDelete(null)}
        loading={deleting}
      />
    </div>
  );
}
