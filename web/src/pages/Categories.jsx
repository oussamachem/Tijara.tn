import { useEffect, useState } from 'react';
import { categoriesApi } from '../api/endpoints.js';
import { apiError } from '../api/client.js';
import { Button, Field, Input, Textarea, Card, Modal, Alert, Spinner, ConfirmDialog } from '../components/ui.jsx';

const emptyForm = { name: '', description: '' };

export default function Categories() {
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
      const { data } = await categoriesApi.list();
      setItems(data);
    } catch (err) {
      setError(apiError(err));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, []);

  const openCreate = () => { setEditing(null); setForm(emptyForm); setFormError(''); setFormOpen(true); };
  const openEdit = (c) => { setEditing(c); setForm({ name: c.name, description: c.description || '' }); setFormError(''); setFormOpen(true); };

  const submitForm = async (e) => {
    e.preventDefault();
    setSaving(true);
    setFormError('');
    try {
      if (editing) {
        await categoriesApi.update(editing.id, form);
        setNotice('Catégorie mise à jour.');
      } else {
        await categoriesApi.create(form);
        setNotice('Catégorie créée.');
      }
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
      await categoriesApi.remove(toDelete.id);
      setNotice('Catégorie supprimée.');
      setToDelete(null);
      load();
    } catch (err) {
      // 409 si des produits y sont rattachés.
      setError(apiError(err));
      setToDelete(null);
    } finally {
      setDeleting(false);
    }
  };

  return (
    <div className="space-y-5">
      <div className="flex items-center justify-between">
        <h2 className="text-xl font-bold text-slate-800">Catégories</h2>
        <Button onClick={openCreate}>+ Nouvelle catégorie</Button>
      </div>

      <Alert type="success" onClose={() => setNotice('')}>{notice}</Alert>
      <Alert type="error" onClose={() => setError('')}>{error}</Alert>

      <Card>
        {loading ? (
          <div className="flex justify-center py-10"><Spinner className="h-7 w-7" /></div>
        ) : items.length === 0 ? (
          <p className="py-6 text-center text-sm text-slate-400">Aucune catégorie.</p>
        ) : (
          <table className="w-full text-sm">
            <thead>
              <tr className="text-left text-slate-500">
                <th className="pb-2">Nom</th>
                <th className="pb-2">Description</th>
                <th className="pb-2 text-right">Actions</th>
              </tr>
            </thead>
            <tbody>
              {items.map((c) => (
                <tr key={c.id} className="border-t border-slate-100">
                  <td className="py-2 font-medium">{c.name}</td>
                  <td className="py-2 text-slate-500">{c.description || '—'}</td>
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
        title={editing ? 'Modifier la catégorie' : 'Nouvelle catégorie'}
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
          <Field label="Description">
            <Textarea rows={3} value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} />
          </Field>
        </form>
      </Modal>

      <ConfirmDialog
        open={!!toDelete}
        title="Supprimer la catégorie"
        message={`Supprimer « ${toDelete?.name} » ? (impossible si des produits y sont rattachés)`}
        confirmLabel="Supprimer"
        onConfirm={confirmDelete}
        onCancel={() => setToDelete(null)}
        loading={deleting}
      />
    </div>
  );
}
