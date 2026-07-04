import { useEffect, useState } from 'react';
import { sellersApi } from '../api/endpoints.js';
import { apiError } from '../api/client.js';
import { Button, Field, Input, Card, Badge, Modal, Alert, Spinner } from '../components/ui.jsx';
import { formatDate } from '../utils/format.js';

const emptyForm = { fullName: '', email: '', password: '' };

export default function Sellers() {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState(emptyForm);
  const [saving, setSaving] = useState(false);
  const [formError, setFormError] = useState('');

  const load = async () => {
    setLoading(true);
    setError('');
    try {
      const { data } = await sellersApi.list();
      setItems(data);
    } catch (err) {
      setError(apiError(err));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, []);

  const openCreate = () => { setEditing(null); setForm(emptyForm); setFormError(''); setFormOpen(true); };
  const openEdit = (s) => { setEditing(s); setForm({ fullName: s.fullName, email: s.email, password: '' }); setFormError(''); setFormOpen(true); };

  const submitForm = async (e) => {
    e.preventDefault();
    setSaving(true);
    setFormError('');
    try {
      if (editing) {
        await sellersApi.update(editing.id, { fullName: form.fullName, email: form.email });
        setNotice('Vendeur mis à jour.');
      } else {
        await sellersApi.create(form);
        setNotice('Vendeur créé.');
      }
      setFormOpen(false);
      load();
    } catch (err) {
      setFormError(apiError(err));
    } finally {
      setSaving(false);
    }
  };

  const toggleActive = async (s) => {
    setError('');
    try {
      if (s.active) await sellersApi.deactivate(s.id);
      else await sellersApi.activate(s.id);
      setNotice(s.active ? 'Compte désactivé.' : 'Compte réactivé.');
      load();
    } catch (err) {
      setError(apiError(err));
    }
  };

  return (
    <div className="space-y-5">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <h2 className="text-xl font-bold text-slate-800">Vendeurs</h2>
        <Button onClick={openCreate} className="w-full sm:w-auto">+ Nouveau vendeur</Button>
      </div>

      <Alert type="success" onClose={() => setNotice('')}>{notice}</Alert>
      <Alert type="error" onClose={() => setError('')}>{error}</Alert>

      <Card>
        {loading ? (
          <div className="flex justify-center py-10"><Spinner className="h-7 w-7" /></div>
        ) : items.length === 0 ? (
          <p className="py-6 text-center text-sm text-slate-400">Aucun vendeur.</p>
        ) : (
          <table className="w-full text-sm">
            <thead>
              <tr className="text-left text-slate-500">
                <th className="pb-2">Nom</th>
                <th className="pb-2">Email</th>
                <th className="pb-2">Créé le</th>
                <th className="pb-2">Statut</th>
                <th className="pb-2 text-right">Actions</th>
              </tr>
            </thead>
            <tbody>
              {items.map((s) => (
                <tr key={s.id} className="border-t border-slate-100">
                  <td className="py-2 font-medium">{s.fullName}</td>
                  <td className="py-2 text-slate-500">{s.email}</td>
                  <td className="py-2 text-slate-500">{formatDate(s.createdAt)}</td>
                  <td className="py-2">
                    {s.active ? <Badge color="green">Actif</Badge> : <Badge color="red">Désactivé</Badge>}
                  </td>
                  <td className="py-2">
                    <div className="flex justify-end gap-1">
                      <Button variant="ghost" onClick={() => openEdit(s)} title="Modifier">✏️</Button>
                      <Button
                        variant={s.active ? 'secondary' : 'primary'}
                        onClick={() => toggleActive(s)}
                      >
                        {s.active ? 'Désactiver' : 'Réactiver'}
                      </Button>
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
        title={editing ? 'Modifier le vendeur' : 'Nouveau vendeur'}
        footer={
          <>
            <Button variant="secondary" onClick={() => setFormOpen(false)}>Annuler</Button>
            <Button onClick={submitForm} disabled={saving}>{saving ? 'Enregistrement…' : 'Enregistrer'}</Button>
          </>
        }
      >
        <form onSubmit={submitForm} className="space-y-3">
          <Alert type="error">{formError}</Alert>
          <Field label="Nom complet" required>
            <Input value={form.fullName} onChange={(e) => setForm({ ...form, fullName: e.target.value })} required autoFocus />
          </Field>
          <Field label="Email" required>
            <Input type="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} required />
          </Field>
          {!editing && (
            <Field label="Mot de passe" required>
              <Input type="password" minLength={6} value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} required />
            </Field>
          )}
        </form>
      </Modal>
    </div>
  );
}
