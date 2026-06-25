import { useEffect, useState, useCallback, Fragment } from 'react';
import { productsApi, categoriesApi, colorsApi, variantsApi } from '../api/endpoints.js';
import { apiError } from '../api/client.js';
import { Button, Field, Input, Textarea, Select, Card, Badge, Modal, Alert, Spinner, Pagination, ConfirmDialog } from '../components/ui.jsx';
import { formatMoney } from '../utils/format.js';

const API_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:8080';
const PAGE_SIZE = 10;

function escapeHtml(v) {
  return String(v ?? '').replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
}

const emptyHeader = { reference: '', name: '', description: '', categoryId: '', purchasePrice: '', salePrice: '' };

export default function Products() {
  const [filters, setFilters] = useState({ name: '', reference: '', categoryId: '' });
  const [page, setPage] = useState(0);
  const [pageData, setPageData] = useState({ content: [], totalPages: 0 });
  const [categories, setCategories] = useState([]);
  const [colors, setColors] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [expanded, setExpanded] = useState(null);

  // Création (matrice)
  const [createOpen, setCreateOpen] = useState(false);
  const [hdr, setHdr] = useState(emptyHeader);
  const [seuil, setSeuil] = useState(0);
  const [availableSizes, setAvailableSizes] = useState([]);
  const [pickedColors, setPickedColors] = useState([]); // colorIds
  const [pickedSizes, setPickedSizes] = useState([]);
  const [qtyMap, setQtyMap] = useState({}); // `${colorId}|${size}` -> qty
  const [saving, setSaving] = useState(false);
  const [createError, setCreateError] = useState('');

  // Édition entête
  const [editProduct, setEditProduct] = useState(null);
  const [editHdr, setEditHdr] = useState(emptyHeader);

  // QR variante
  const [qrVariant, setQrVariant] = useState(null);
  const [qrUrl, setQrUrl] = useState('');

  // Ajout variante (par produit)
  const [addForm, setAddForm] = useState({ colorId: '', size: '', quantity: 0 });
  const [addSizes, setAddSizes] = useState([]);

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
    colorsApi.list().then((r) => setColors(r.data)).catch(() => {});
  }, []);

  const refresh = () => load();
  const submitFilters = (e) => { e.preventDefault(); setPage(0); load(); };
  const resetFilters = () => { setFilters({ name: '', reference: '', categoryId: '' }); setPage(0); };

  // ---------- Création ----------
  const openCreate = () => {
    setHdr(emptyHeader); setSeuil(0); setAvailableSizes([]); setPickedColors([]); setPickedSizes([]); setQtyMap({});
    setCreateError(''); setCreateOpen(true);
  };
  const onCategoryChange = async (categoryId) => {
    setHdr((h) => ({ ...h, categoryId }));
    setPickedSizes([]); setQtyMap({});
    if (categoryId) {
      try { const { data } = await categoriesApi.sizeOptions(categoryId); setAvailableSizes(data.sizes); }
      catch { setAvailableSizes([]); }
    } else setAvailableSizes([]);
  };
  const toggle = (list, setList, value) =>
    setList(list.includes(value) ? list.filter((x) => x !== value) : [...list, value]);

  const submitCreate = async (e) => {
    e.preventDefault();
    if (pickedColors.length === 0 || pickedSizes.length === 0) {
      setCreateError('Sélectionnez au moins une couleur et une taille.');
      return;
    }
    setSaving(true); setCreateError('');
    try {
      const variants = [];
      for (const colorId of pickedColors)
        for (const size of pickedSizes)
          variants.push({ colorId: Number(colorId), size, quantity: Number(qtyMap[`${colorId}|${size}`] || 0), seuilAlerte: Number(seuil) || 0 });
      await productsApi.create({
        reference: hdr.reference, name: hdr.name, description: hdr.description,
        categoryId: Number(hdr.categoryId), purchasePrice: Number(hdr.purchasePrice),
        salePrice: Number(hdr.salePrice), variants,
      });
      setNotice(`Produit créé (${variants.length} variante(s), QR générés).`);
      setCreateOpen(false); load();
    } catch (err) { setCreateError(apiError(err)); }
    finally { setSaving(false); }
  };

  // ---------- Édition entête + image ----------
  const openEdit = (p) => {
    setEditProduct(p);
    setEditHdr({ reference: p.reference, name: p.name, description: p.description || '', categoryId: p.categoryId || '', purchasePrice: p.purchasePrice, salePrice: p.salePrice });
  };
  const submitEdit = async (e) => {
    e.preventDefault();
    setSaving(true);
    try {
      await productsApi.updateHeader(editProduct.id, {
        reference: editHdr.reference, name: editHdr.name, description: editHdr.description,
        categoryId: Number(editHdr.categoryId), purchasePrice: Number(editHdr.purchasePrice), salePrice: Number(editHdr.salePrice),
      });
      setNotice('Produit mis à jour.'); setEditProduct(null); load();
    } catch (err) { setError(apiError(err)); }
    finally { setSaving(false); }
  };
  const handleImage = async (e, productId) => {
    const file = e.target.files?.[0];
    if (!file) return;
    try { await productsApi.uploadImage(productId, file); setNotice('Image mise à jour.'); load(); }
    catch (err) { setError(apiError(err)); }
  };

  // ---------- Variantes (stock / QR / ajout / suppression) ----------
  const setVariantStock = async (variantId, quantity) => {
    try { await variantsApi.setStock(variantId, Number(quantity)); setNotice('Stock mis à jour.'); load(); }
    catch (err) { setError(apiError(err)); }
  };
  const removeVariant = async (variantId) => {
    try { await variantsApi.remove(variantId); setNotice('Variante supprimée.'); load(); }
    catch (err) { setError(apiError(err)); } // 409 si dernière / déjà vendue
  };
  const openAddVariant = async (product) => {
    setAddForm({ colorId: '', size: '', quantity: 0 });
    try { const { data } = await categoriesApi.sizeOptions(product.categoryId); setAddSizes(data.sizes); }
    catch { setAddSizes([]); }
  };
  const submitAddVariant = async (productId) => {
    try {
      await productsApi.addVariant(productId, { colorId: Number(addForm.colorId), size: addForm.size, quantity: Number(addForm.quantity), seuilAlerte: 0 });
      setNotice('Variante ajoutée.'); setAddForm({ colorId: '', size: '', quantity: 0 }); load();
    } catch (err) { setError(apiError(err)); }
  };

  const openQr = async (variant) => {
    setQrVariant(variant); setQrUrl('');
    try { const { data } = await variantsApi.qrCode(variant.id); setQrUrl(URL.createObjectURL(data)); }
    catch (err) { setError(apiError(err)); }
  };
  const closeQr = () => { if (qrUrl) URL.revokeObjectURL(qrUrl); setQrUrl(''); setQrVariant(null); };
  const printQr = () => {
    if (!qrUrl || !qrVariant) return;
    const w = window.open('', '_blank', 'width=420,height=560');
    if (!w) { setError("Pop-up bloqué (autorisez les pop-ups)."); return; }
    const ref = escapeHtml(qrVariant.reference);
    const sub = escapeHtml(`${qrVariant.colorName} · taille ${qrVariant.size}`);
    w.document.write(`<!doctype html><html><head><meta charset="utf-8"><title>QR ${ref}</title></head>
      <body style="text-align:center;font-family:sans-serif;padding:24px;margin:0">
        <h3 style="margin:0 0 2px">${ref}</h3><p style="margin:0 0 12px;color:#555">${sub}</p>
        <img src="${qrUrl}" style="width:260px" onload="window.focus();window.print();" />
      </body></html>`);
    w.document.close();
  };

  const confirmDelete = async () => {
    setDeleting(true);
    try { await productsApi.remove(toDelete.id); setNotice('Produit supprimé.'); setToDelete(null); load(); }
    catch (err) { setError(apiError(err)); setToDelete(null); }
    finally { setDeleting(false); }
  };

  return (
    <div className="space-y-5">
      <div className="flex items-center justify-between">
        <h2 className="text-xl font-bold text-slate-800">Produits</h2>
        <Button onClick={openCreate}>+ Nouveau produit</Button>
      </div>

      <Alert type="success" onClose={() => setNotice('')}>{notice}</Alert>
      <Alert type="error" onClose={() => setError('')}>{error}</Alert>

      <Card>
        <form onSubmit={submitFilters} className="grid grid-cols-1 gap-3 md:grid-cols-4">
          <Field label="Nom"><Input value={filters.name} onChange={(e) => setFilters({ ...filters, name: e.target.value })} placeholder="Rechercher…" /></Field>
          <Field label="Référence"><Input value={filters.reference} onChange={(e) => setFilters({ ...filters, reference: e.target.value })} /></Field>
          <Field label="Catégorie">
            <Select value={filters.categoryId} onChange={(e) => setFilters({ ...filters, categoryId: e.target.value })}>
              <option value="">Toutes</option>
              {categories.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
            </Select>
          </Field>
          <div className="flex items-end gap-2">
            <Button type="submit">Filtrer</Button>
            <Button type="button" variant="secondary" onClick={resetFilters}>Réinitialiser</Button>
          </div>
        </form>
      </Card>

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
                  <th className="pb-2"></th>
                  <th className="pb-2">Référence</th>
                  <th className="pb-2">Nom</th>
                  <th className="pb-2">Catégorie</th>
                  <th className="pb-2 text-right">Prix</th>
                  <th className="pb-2 text-right">Stock total</th>
                  <th className="pb-2 text-right">Variantes</th>
                  <th className="pb-2 text-right">Actions</th>
                </tr>
              </thead>
              <tbody>
                {pageData.content.map((p) => (
                  <Fragment key={p.id}>
                    <tr className="border-t border-slate-100">
                      <td className="py-2">
                        <button onClick={() => { setExpanded(expanded === p.id ? null : p.id); openAddVariant(p); }} className="text-slate-400">
                          {expanded === p.id ? '▼' : '▶'}
                        </button>
                      </td>
                      <td className="py-2 font-mono text-xs">{p.reference}</td>
                      <td className="py-2">{p.name}</td>
                      <td className="py-2 text-slate-500">{p.categoryName || '—'}</td>
                      <td className="py-2 text-right">{formatMoney(p.salePrice)}</td>
                      <td className="py-2 text-right">{p.lowStock ? <Badge color="amber">{p.totalStock}</Badge> : p.totalStock}</td>
                      <td className="py-2 text-right text-slate-500">{p.variants.length}</td>
                      <td className="py-2">
                        <div className="flex justify-end gap-1">
                          <Button variant="ghost" onClick={() => openEdit(p)} title="Modifier l'entête">✏️</Button>
                          <Button variant="ghost" onClick={() => setToDelete(p)} title="Supprimer">🗑️</Button>
                        </div>
                      </td>
                    </tr>
                    {expanded === p.id && (
                      <tr className="bg-slate-50">
                        <td></td>
                        <td colSpan={7} className="py-3 pr-4">
                          <div className="space-y-2">
                            {p.variants.map((v) => (
                              <div key={v.id} className="flex flex-wrap items-center gap-3 rounded-lg border border-slate-200 bg-white px-3 py-2">
                                <span className="inline-block h-4 w-4 rounded-full border" style={{ backgroundColor: v.colorHex || '#fff' }} />
                                <span className="font-medium">{v.colorName} · {v.size}</span>
                                <span className="font-mono text-xs text-slate-400">{v.reference}</span>
                                {v.lowStock && <Badge color={v.quantity === 0 ? 'red' : 'amber'}>{v.quantity === 0 ? 'Rupture' : 'Faible'}</Badge>}
                                <span className="ml-auto flex items-center gap-2">
                                  <span className="text-slate-500">Stock</span>
                                  <input type="number" min="0" defaultValue={v.quantity}
                                    className="w-20 rounded border border-slate-300 px-2 py-1 text-sm"
                                    onBlur={(e) => { if (Number(e.target.value) !== v.quantity) setVariantStock(v.id, e.target.value); }} />
                                  <Button variant="ghost" onClick={() => openQr(v)} title="QR">🔳</Button>
                                  <Button variant="ghost" onClick={() => removeVariant(v.id)} title="Retirer la variante">🗑️</Button>
                                </span>
                              </div>
                            ))}
                            {/* Ajout d'une variante */}
                            <div className="flex flex-wrap items-end gap-2 rounded-lg border border-dashed border-slate-300 px-3 py-2">
                              <Field label="Couleur">
                                <Select value={addForm.colorId} onChange={(e) => setAddForm({ ...addForm, colorId: e.target.value })} className="w-36">
                                  <option value="">—</option>
                                  {colors.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
                                </Select>
                              </Field>
                              <Field label="Taille">
                                <Select value={addForm.size} onChange={(e) => setAddForm({ ...addForm, size: e.target.value })} className="w-28">
                                  <option value="">—</option>
                                  {addSizes.map((s) => <option key={s} value={s}>{s}</option>)}
                                </Select>
                              </Field>
                              <Field label="Stock">
                                <Input type="number" min="0" value={addForm.quantity} onChange={(e) => setAddForm({ ...addForm, quantity: e.target.value })} className="w-24" />
                              </Field>
                              <Button type="button" variant="secondary" disabled={!addForm.colorId || !addForm.size} onClick={() => submitAddVariant(p.id)}>+ Variante</Button>
                            </div>
                          </div>
                        </td>
                      </tr>
                    )}
                  </Fragment>
                ))}
              </tbody>
            </table>
          </div>
        )}
        <Pagination page={page} totalPages={pageData.totalPages} onChange={setPage} />
      </Card>

      {/* Modale création */}
      <Modal open={createOpen} onClose={() => setCreateOpen(false)} title="Nouveau produit" wide
        footer={<><Button variant="secondary" onClick={() => setCreateOpen(false)}>Annuler</Button>
          <Button onClick={submitCreate} disabled={saving}>{saving ? 'Création…' : 'Créer'}</Button></>}>
        <form onSubmit={submitCreate} className="space-y-3">
          <Alert type="error">{createError}</Alert>
          <div className="grid grid-cols-1 gap-3 md:grid-cols-2">
            <Field label="Référence" required><Input value={hdr.reference} onChange={(e) => setHdr({ ...hdr, reference: e.target.value })} required /></Field>
            <Field label="Nom" required><Input value={hdr.name} onChange={(e) => setHdr({ ...hdr, name: e.target.value })} required /></Field>
            <Field label="Catégorie" required>
              <Select value={hdr.categoryId} onChange={(e) => onCategoryChange(e.target.value)} required>
                <option value="">— Choisir —</option>
                {categories.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
              </Select>
            </Field>
            <Field label="Seuil d'alerte (toutes variantes)"><Input type="number" min="0" value={seuil} onChange={(e) => setSeuil(e.target.value)} /></Field>
            <Field label="Prix d'achat" required><Input type="number" step="0.01" min="0" value={hdr.purchasePrice} onChange={(e) => setHdr({ ...hdr, purchasePrice: e.target.value })} required /></Field>
            <Field label="Prix de vente" required><Input type="number" step="0.01" min="0" value={hdr.salePrice} onChange={(e) => setHdr({ ...hdr, salePrice: e.target.value })} required /></Field>
          </div>
          <Field label="Description"><Textarea rows={2} value={hdr.description} onChange={(e) => setHdr({ ...hdr, description: e.target.value })} /></Field>

          {hdr.categoryId && (
            <>
              <div>
                <div className="mb-1 text-sm font-medium text-slate-700">Couleurs</div>
                <div className="flex flex-wrap gap-2">
                  {colors.map((c) => (
                    <button type="button" key={c.id} onClick={() => toggle(pickedColors, setPickedColors, String(c.id))}
                      className={`rounded-full border px-3 py-1 text-sm ${pickedColors.includes(String(c.id)) ? 'border-brand-600 bg-brand-50 text-brand-700' : 'border-slate-300 text-slate-600'}`}>
                      <span className="mr-1 inline-block h-3 w-3 rounded-full border align-middle" style={{ backgroundColor: c.hex || '#fff' }} />{c.name}
                    </button>
                  ))}
                </div>
              </div>
              <div>
                <div className="mb-1 text-sm font-medium text-slate-700">Tailles</div>
                <div className="flex flex-wrap gap-2">
                  {availableSizes.map((s) => (
                    <button type="button" key={s} onClick={() => toggle(pickedSizes, setPickedSizes, s)}
                      className={`rounded-lg border px-3 py-1 text-sm ${pickedSizes.includes(s) ? 'border-brand-600 bg-brand-50 text-brand-700' : 'border-slate-300 text-slate-600'}`}>{s}</button>
                  ))}
                </div>
              </div>

              {pickedColors.length > 0 && pickedSizes.length > 0 && (
                <div className="overflow-x-auto">
                  <div className="mb-1 text-sm font-medium text-slate-700">Stock par variante</div>
                  <table className="text-sm">
                    <thead>
                      <tr><th className="p-1"></th>{pickedSizes.map((s) => <th key={s} className="p-1 text-center text-slate-500">{s}</th>)}</tr>
                    </thead>
                    <tbody>
                      {pickedColors.map((cid) => (
                        <tr key={cid}>
                          <td className="p-1 font-medium">{colors.find((c) => String(c.id) === cid)?.name}</td>
                          {pickedSizes.map((s) => (
                            <td key={s} className="p-1">
                              <input type="number" min="0" value={qtyMap[`${cid}|${s}`] || 0}
                                onChange={(e) => setQtyMap({ ...qtyMap, [`${cid}|${s}`]: e.target.value })}
                                className="w-16 rounded border border-slate-300 px-2 py-1 text-center" />
                            </td>
                          ))}
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </>
          )}
        </form>
      </Modal>

      {/* Modale édition entête */}
      <Modal open={!!editProduct} onClose={() => setEditProduct(null)} title="Modifier le produit" wide
        footer={<><Button variant="secondary" onClick={() => setEditProduct(null)}>Annuler</Button>
          <Button onClick={submitEdit} disabled={saving}>{saving ? 'Enregistrement…' : 'Enregistrer'}</Button></>}>
        {editProduct && (
          <form onSubmit={submitEdit} className="grid grid-cols-1 gap-3 md:grid-cols-2">
            <Field label="Référence" required><Input value={editHdr.reference} onChange={(e) => setEditHdr({ ...editHdr, reference: e.target.value })} required /></Field>
            <Field label="Nom" required><Input value={editHdr.name} onChange={(e) => setEditHdr({ ...editHdr, name: e.target.value })} required /></Field>
            <Field label="Catégorie" required>
              <Select value={editHdr.categoryId} onChange={(e) => setEditHdr({ ...editHdr, categoryId: e.target.value })} required>
                {categories.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
              </Select>
            </Field>
            <div />
            <Field label="Prix d'achat" required><Input type="number" step="0.01" min="0" value={editHdr.purchasePrice} onChange={(e) => setEditHdr({ ...editHdr, purchasePrice: e.target.value })} required /></Field>
            <Field label="Prix de vente" required><Input type="number" step="0.01" min="0" value={editHdr.salePrice} onChange={(e) => setEditHdr({ ...editHdr, salePrice: e.target.value })} required /></Field>
            <div className="md:col-span-2"><Field label="Description"><Textarea rows={2} value={editHdr.description} onChange={(e) => setEditHdr({ ...editHdr, description: e.target.value })} /></Field></div>
            <div className="md:col-span-2 rounded-lg border border-slate-200 p-3">
              <div className="mb-2 text-sm font-medium text-slate-600">Image du produit</div>
              <div className="flex items-center gap-4">
                {editProduct.imageUrl
                  ? <img src={`${API_URL}${editProduct.imageUrl}`} alt="" className="h-20 w-20 rounded object-cover" />
                  : <div className="flex h-20 w-20 items-center justify-center rounded bg-slate-100 text-xs text-slate-400">Aucune</div>}
                <input type="file" accept="image/png,image/jpeg,image/webp" onChange={(e) => handleImage(e, editProduct.id)} className="text-sm" />
              </div>
            </div>
          </form>
        )}
      </Modal>

      {/* Modale QR variante */}
      <Modal open={!!qrVariant} onClose={closeQr} title={qrVariant ? `QR — ${qrVariant.reference}` : ''}
        footer={<><Button variant="secondary" onClick={closeQr}>Fermer</Button>
          <Button onClick={printQr} disabled={!qrUrl}>🖨️ Imprimer</Button></>}>
        <div className="flex flex-col items-center gap-2">
          {qrUrl ? <img src={qrUrl} alt="QR" className="h-60 w-60" /> : <Spinner className="h-7 w-7" />}
          <p className="text-sm text-slate-500">{qrVariant?.colorName} · taille {qrVariant?.size}</p>
        </div>
      </Modal>

      <ConfirmDialog open={!!toDelete} title="Supprimer le produit"
        message={`Supprimer « ${toDelete?.name} » et toutes ses variantes ? (impossible si déjà vendu)`}
        confirmLabel="Supprimer" onConfirm={confirmDelete} onCancel={() => setToDelete(null)} loading={deleting} />
    </div>
  );
}
