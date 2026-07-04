import { useEffect, useState } from 'react';
import { sizesApi } from '../api/endpoints.js';
import { apiError } from '../api/client.js';
import { Button, Field, Input, Card, Modal, Alert, Spinner, ConfirmDialog } from '../components/ui.jsx';

const emptyForm = { label: '', position: '' };

export default function Sizes() {
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
      const { data } = await sizesApi.list();
      setItems(data);
    } catch (err) {
      setError(apiError(err));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, []);

  const openCreate = () => { setEditing(null); setForm(emptyForm); setFormError(''); setFormOpen(true); };
  const openEdit = (s) => {
    setEditing(s);
    setForm({ label: s.label, position: s.position ?? '' });
    setFormError('');
    setFormOpen(true);
  };

  const submitForm = async (e) => {
    e.preventDefault();
    setSaving(true);
    setFormError('');
    // La position est optionnelle : chaîne vide -> null (affichage en fin de liste).
    const payload = {
      label: form.label,
      position: form.position === '' ? null : Number(form.position),
    };
    try {
      if (editing) {
        await sizesApi.update(editing.id, payload);
        setNotice('Taille mise à jour.');
      } else {
        await sizesApi.create(payload);
        setNotice('Taille créée.');
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
      await sizesApi.remove(toDelete.id);
      setNotice('Taille supprimée.');
      setToDelete(null);
      load();
    } catch (err) {
      // 409 si des produits référencent cette taille.
      setError(apiError(err));
      setToDelete(null);
    } finally {
      setDeleting(false);
    }
  };

  return (
    <div className="space-y-5">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <h2 className="text-xl font-bold text-slate-800">Tailles</h2>
        <Button onClick={openCreate} className="w-full sm:w-auto">+ Nouvelle taille</Button>
      </div>

      <Alert type="success" onClose={() => setNotice('')}>{notice}</Alert>
      <Alert type="error" onClose={() => setError('')}>{error}</Alert>

      <Card>
        {loading ? (
          <div className="flex justify-center py-10"><Spinner className="h-7 w-7" /></div>
        ) : items.length === 0 ? (
          <p className="py-6 text-center text-sm text-slate-400">
            Aucune taille. Créez-en une pour pouvoir la sélectionner sur un produit.
          </p>
        ) : (
          <table className="w-full text-sm">
            <thead>
              <tr className="text-left text-slate-500">
                <th className="pb-2">Libellé</th>
                <th className="pb-2 text-right">Ordre</th>
                <th className="pb-2 text-right">Actions</th>
              </tr>
            </thead>
            <tbody>
              {items.map((s) => (
                <tr key={s.id} className="border-t border-slate-100">
                  <td className="py-2 font-medium">{s.label}</td>
                  <td className="py-2 text-right text-slate-500">{s.position ?? '—'}</td>
                  <td className="py-2">
                    <div className="flex justify-end gap-1">
                      <Button variant="ghost" onClick={() => openEdit(s)} title="Modifier">✏️</Button>
                      <Button variant="ghost" onClick={() => setToDelete(s)} title="Supprimer">🗑️</Button>
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
        title={editing ? 'Modifier la taille' : 'Nouvelle taille'}
        footer={
          <>
            <Button variant="secondary" onClick={() => setFormOpen(false)}>Annuler</Button>
            <Button onClick={submitForm} disabled={saving}>{saving ? 'Enregistrement…' : 'Enregistrer'}</Button>
          </>
        }
      >
        <form onSubmit={submitForm} className="space-y-3">
          <Alert type="error">{formError}</Alert>
          <Field label="Libellé" required>
            <Input
              value={form.label}
              onChange={(e) => setForm({ ...form, label: e.target.value })}
              placeholder="S, M, L, 38, 40…"
              required
              autoFocus
            />
          </Field>
          <Field label="Ordre d'affichage (optionnel)">
            <Input
              type="number"
              min="0"
              value={form.position}
              onChange={(e) => setForm({ ...form, position: e.target.value })}
              placeholder="Laisser vide pour un tri en fin de liste"
            />
          </Field>
        </form>
      </Modal>

      <ConfirmDialog
        open={!!toDelete}
        title="Supprimer la taille"
        message={`Supprimer « ${toDelete?.label} » ? (impossible si des produits y sont rattachés)`}
        confirmLabel="Supprimer"
        onConfirm={confirmDelete}
        onCancel={() => setToDelete(null)}
        loading={deleting}
      />
    </div>
  );
}
