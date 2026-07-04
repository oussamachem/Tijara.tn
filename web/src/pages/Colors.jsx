import { useEffect, useState } from 'react';
import { colorsApi } from '../api/endpoints.js';
import { apiError } from '../api/client.js';
import { Button, Field, Input, Card, Modal, Alert, Spinner, ConfirmDialog } from '../components/ui.jsx';

const emptyForm = { name: '', hex: '' };

export default function Colors() {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState(emptyForm);
  const [saving, setSaving] = useState(false);
  const [formError, setFormError] = useState('');

  const [toDelete, setToDelete] = useState(null);
  const [deleting, setDeleting] = useState(false);

  const load = async () => {
    setLoading(true);
    setError('');
    try {
      const { data } = await colorsApi.list();
      setItems(data);
    } catch (err) {
      setError(apiError(err));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, []);

  const openCreate = () => { setEditing(null); setForm(emptyForm); setFormError(''); setFormOpen(true); };
  const openEdit = (c) => { setEditing(c); setForm({ name: c.name, hex: c.hex || '' }); setFormError(''); setFormOpen(true); };

  const submitForm = async (e) => {
    e.preventDefault();
    setSaving(true);
    setFormError('');
    try {
      const payload = { name: form.name, hex: form.hex || null };
      if (editing) { await colorsApi.update(editing.id, payload); setNotice('Couleur mise à jour.'); }
      else { await colorsApi.create(payload); setNotice('Couleur créée.'); }
      setFormOpen(false);
      load();
    } catch (err) {
      setFormError(apiError(err));
    } finally {
      setSaving(false);
    }
  };

  const confirmDelete = async () => {
    setDeleting(true);
    try {
      await colorsApi.remove(toDelete.id);
      setNotice('Couleur supprimée.');
      setToDelete(null);
      load();
    } catch (err) {
      setError(apiError(err)); // 409 si une variante l'utilise
      setToDelete(null);
    } finally {
      setDeleting(false);
    }
  };

  return (
    <div className="space-y-5">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <h2 className="text-xl font-bold text-slate-800">Couleurs</h2>
        <Button onClick={openCreate} className="w-full sm:w-auto">+ Nouvelle couleur</Button>
      </div>

      <Alert type="success" onClose={() => setNotice('')}>{notice}</Alert>
      <Alert type="error" onClose={() => setError('')}>{error}</Alert>

      <Card>
        {loading ? (
          <div className="flex justify-center py-10"><Spinner className="h-7 w-7" /></div>
        ) : items.length === 0 ? (
          <p className="py-6 text-center text-sm text-slate-400">Aucune couleur.</p>
        ) : (
          <table className="w-full text-sm">
            <thead>
              <tr className="text-left text-slate-500">
                <th className="pb-2">Aperçu</th>
                <th className="pb-2">Nom</th>
                <th className="pb-2">Hex</th>
                <th className="pb-2 text-right">Actions</th>
              </tr>
            </thead>
            <tbody>
              {items.map((c) => (
                <tr key={c.id} className="border-t border-slate-100">
                  <td className="py-2">
                    <span className="inline-block h-5 w-5 rounded border border-slate-300" style={{ backgroundColor: c.hex || '#fff' }} />
                  </td>
                  <td className="py-2 font-medium">{c.name}</td>
                  <td className="py-2 font-mono text-xs text-slate-500">{c.hex || '—'}</td>
                  <td className="py-2">
                    <div className="flex justify-end gap-1">
                      <Button variant="ghost" onClick={() => openEdit(c)} title="Modifier">✏️</Button>
                      <Button variant="ghost" onClick={() => setToDelete(c)} title="Supprimer">🗑️</Button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </Card>

      <Modal
        open={formOpen}
        onClose={() => setFormOpen(false)}
        title={editing ? 'Modifier la couleur' : 'Nouvelle couleur'}
        footer={
          <>
            <Button variant="secondary" onClick={() => setFormOpen(false)}>Annuler</Button>
            <Button onClick={submitForm} disabled={saving}>{saving ? 'Enregistrement…' : 'Enregistrer'}</Button>
          </>
        }
      >
        <form onSubmit={submitForm} className="space-y-3">
          <Alert type="error">{formError}</Alert>
          <Field label="Nom" required>
            <Input value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} required autoFocus />
          </Field>
          <Field label="Code couleur (optionnel)">
            <div className="flex items-center gap-3">
              <input type="color" value={form.hex || '#000000'} onChange={(e) => setForm({ ...form, hex: e.target.value })} className="h-10 w-14 rounded border border-slate-300" />
              <Input value={form.hex} onChange={(e) => setForm({ ...form, hex: e.target.value })} placeholder="#1E40AF" className="flex-1" />
            </div>
          </Field>
        </form>
      </Modal>

      <ConfirmDialog
        open={!!toDelete}
        title="Supprimer la couleur"
        message={`Supprimer « ${toDelete?.name} » ? (impossible si une variante l'utilise)`}
        confirmLabel="Supprimer"
        onConfirm={confirmDelete}
        onCancel={() => setToDelete(null)}
        loading={deleting}
      />
    </div>
  );
}
