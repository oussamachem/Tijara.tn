import { useEffect, useState } from 'react';
import { boutiquesApi } from '../api/endpoints.js';
import { apiError } from '../api/client.js';
import ShopSwitcher from '../components/ShopSwitcher.jsx';
import { Button, Input, Field, ErrorNote, EmptyState, Spinner, Badge } from '../client/components/ui.jsx';
import { formatDate } from '../client/lib/format.js';

const STATUS_STYLE = { ACTIVE: 'bg-emerald-100 text-emerald-700', SUSPENDED: 'bg-rose-100 text-rose-700' };
const empty = { name: '', adminName: '', adminEmail: '', adminPassword: '' };

/** Espace PLATEFORME (PLATFORM_ADMIN) : gestion des boutiques (tenants). Non scopé -> sans X-Shop-Id. */
export default function PlatformSpace() {
  const [list, setList] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [form, setForm] = useState(null);   // null = fermé
  const [busy, setBusy] = useState(false);

  const load = () => {
    setLoading(true); setError('');
    boutiquesApi.list()
      .then(({ data }) => setList(data))
      .catch((e) => setError(apiError(e)))
      .finally(() => setLoading(false));
  };
  useEffect(load, []);

  const create = async () => {
    setBusy(true); setError('');
    try {
      await boutiquesApi.create({
        name: form.name.trim(), adminName: form.adminName.trim(),
        adminEmail: form.adminEmail.trim(), adminPassword: form.adminPassword,
      });
      setForm(null); load();
    } catch (e) { setError(apiError(e)); } finally { setBusy(false); }
  };

  const toggle = async (b) => {
    setError('');
    try {
      await (b.status === 'SUSPENDED' ? boutiquesApi.reactivate(b.id) : boutiquesApi.suspend(b.id));
      load();
    } catch (e) { setError(apiError(e)); }
  };

  return (
    <div className="flex h-full flex-col bg-slate-50">
      <header className="flex items-center justify-between gap-3 border-b border-slate-200 bg-white px-4 py-3 lg:px-6">
        <h1 className="font-bold text-slate-800">🛡️ Plateforme — Boutiques</h1>
        <ShopSwitcher />
      </header>

      <main className="min-w-0 flex-1 overflow-y-auto p-4 lg:p-6">
        <div className="mx-auto max-w-3xl">
          <div className="mb-4 flex items-center justify-between">
            <p className="text-sm text-slate-500">{list.length} boutique(s)</p>
            <Button onClick={() => setForm(empty)}>＋ Nouvelle boutique</Button>
          </div>
          <ErrorNote message={error} onRetry={load} />

          {loading ? <Spinner label="Chargement…" /> : list.length === 0 ? (
            <EmptyState icon="🏬" title="Aucune boutique" sub="Créez la première boutique de la plateforme." />
          ) : (
            <ul className="space-y-2">
              {list.map((b) => (
                <li key={b.id} className="flex items-center justify-between rounded-xl border border-slate-200 bg-white p-4">
                  <div className="min-w-0">
                    <div className="flex items-center gap-2">
                      <span className="truncate font-semibold text-slate-800">{b.name}</span>
                      <Badge className={STATUS_STYLE[b.status] || 'bg-slate-100 text-slate-600'}>{b.status}</Badge>
                    </div>
                    <div className="text-xs text-slate-400">/{b.slug}{b.createdAt ? ` · ${formatDate(b.createdAt)}` : ''}</div>
                  </div>
                  <Button variant={b.status === 'SUSPENDED' ? 'primary' : 'secondary'} onClick={() => toggle(b)}>
                    {b.status === 'SUSPENDED' ? 'Réactiver' : 'Suspendre'}
                  </Button>
                </li>
              ))}
            </ul>
          )}
        </div>
      </main>

      {form && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4" onClick={() => setForm(null)}>
          <div className="w-full max-w-md rounded-2xl bg-white p-6" onClick={(e) => e.stopPropagation()}>
            <h3 className="mb-4 text-lg font-extrabold text-slate-800">Nouvelle boutique</h3>
            <div className="space-y-3">
              <Field label="Nom de la boutique" required><Input value={form.name} onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))} placeholder="Ma Boutique" /></Field>
              <Field label="Nom du propriétaire" required><Input value={form.adminName} onChange={(e) => setForm((f) => ({ ...f, adminName: e.target.value }))} placeholder="Prénom Nom" /></Field>
              <Field label="Email du propriétaire" required><Input type="email" value={form.adminEmail} onChange={(e) => setForm((f) => ({ ...f, adminEmail: e.target.value }))} placeholder="owner@boutique.com" /></Field>
              <Field label="Mot de passe initial" required><Input type="password" value={form.adminPassword} onChange={(e) => setForm((f) => ({ ...f, adminPassword: e.target.value }))} placeholder="••••••••" /></Field>
              <ErrorNote message={error} />
              <div className="flex gap-2 pt-2">
                <Button className="flex-1" disabled={busy} onClick={create}>{busy ? 'Création…' : 'Créer'}</Button>
                <Button variant="secondary" onClick={() => setForm(null)}>Annuler</Button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
