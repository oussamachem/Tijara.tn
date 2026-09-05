import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { profileApi } from '../api/endpoints.js';
import { apiError } from '../api/client.js';
import { useAuth } from '../context/AuthContext.jsx';
import { keycloak } from '../auth/keycloak.js';
import { Button, Field, Input, Select, Alert, Spinner } from '../components/ui.jsx';
import { GOVERNORATS } from '../lib/goodex.js';

/** Écran Profil partagé (tous rôles) : éditer nom/email + livraison + changer le mot de passe. Route /profile. */
export default function Profile() {
  const navigate = useNavigate();
  const { user } = useAuth();
  const [form, setForm] = useState({ fullName: '', email: '', phone: '', address: '', governorat: '' });
  const [loading, setLoading] = useState(true);
  const [savingP, setSavingP] = useState(false);
  const [okP, setOkP] = useState('');
  const [errP, setErrP] = useState('');

  useEffect(() => {
    profileApi.get()
      .then(({ data }) => setForm({
        fullName: data.fullName || '', email: data.email || '',
        phone: data.phone || '', address: data.address || '', governorat: data.governorat || '',
      }))
      .catch((e) => setErrP(apiError(e)))
      .finally(() => setLoading(false));
  }, []);

  const saveProfile = async (e) => {
    e.preventDefault();
    setSavingP(true); setOkP(''); setErrP('');
    try {
      // L'email est en lecture seule (identité Keycloak = clé de rattachement) : on renvoie la valeur
      // stockée telle quelle, jamais modifiée depuis l'app.
      await profileApi.update({
        fullName: form.fullName.trim(), email: form.email.trim(),
        phone: form.phone.trim(), address: form.address.trim(), governorat: form.governorat || null,
      });
      setOkP('Profil mis à jour.');
    } catch (err) { setErrP(apiError(err)); } finally { setSavingP(false); }
  };

  return (
    <div className="min-h-full bg-slate-100">
      <header className="flex items-center gap-3 border-b border-slate-200 bg-white px-4 py-3">
        <button onClick={() => navigate(-1)} className="-ml-2 flex h-9 w-9 items-center justify-center rounded-full text-slate-600 hover:bg-slate-100" aria-label="Retour">
          <span className="text-xl leading-none">‹</span>
        </button>
        <h1 className="font-semibold text-slate-800">Mon profil</h1>
      </header>

      <div className="mx-auto max-w-md space-y-6 p-4">
        {loading ? <Spinner className="mx-auto mt-10 h-8 w-8" /> : (
          <>
            <form onSubmit={saveProfile} className="space-y-3 rounded-2xl bg-white p-5 shadow-sm">
              <h2 className="font-semibold text-slate-700">Informations</h2>
              <Alert type="error" onClose={() => setErrP('')}>{errP}</Alert>
              <Alert type="success" onClose={() => setOkP('')}>{okP}</Alert>
              <Field label="Nom complet"><Input value={form.fullName} onChange={(e) => setForm((f) => ({ ...f, fullName: e.target.value }))} required /></Field>
              <Field label="Email">
                <Input type="email" value={form.email} readOnly disabled className="cursor-not-allowed bg-slate-100 text-slate-500" />
                <p className="mt-1 text-xs text-slate-400">Géré par votre compte de connexion (Keycloak).</p>
              </Field>

              <div className="mt-1 border-t border-slate-100 pt-3">
                <p className="mb-2 text-xs font-semibold uppercase tracking-wide text-slate-400">Livraison à domicile</p>
                <div className="space-y-3">
                  <Field label="Téléphone"><Input value={form.phone} onChange={(e) => setForm((f) => ({ ...f, phone: e.target.value }))} placeholder="Ex. 55 123 456" inputMode="tel" /></Field>
                  <Field label="Adresse"><Input value={form.address} onChange={(e) => setForm((f) => ({ ...f, address: e.target.value }))} placeholder="Rue, ville, code postal" /></Field>
                  <Field label="Gouvernorat">
                    <Select value={form.governorat} onChange={(e) => setForm((f) => ({ ...f, governorat: e.target.value }))}>
                      <option value="">— Choisir —</option>
                      {GOVERNORATS.map((g) => <option key={g} value={g}>{g}</option>)}
                    </Select>
                  </Field>
                </div>
              </div>

              <Button type="submit" className="w-full" disabled={savingP}>{savingP ? 'Enregistrement…' : 'Enregistrer'}</Button>
            </form>

            <div className="space-y-3 rounded-2xl bg-white p-5 shadow-sm">
              <h2 className="font-semibold text-slate-700">Sécurité</h2>
              <p className="text-sm text-slate-500">
                Le mot de passe et la connexion à deux facteurs sont gérés dans votre compte sécurisé.
              </p>
              <Button type="button" variant="secondary" className="w-full" onClick={() => keycloak.accountManagement()}>
                Gérer mon mot de passe
              </Button>
            </div>

            <p className="text-center text-xs text-slate-400">Connecté : {user?.email}</p>
          </>
        )}
      </div>
    </div>
  );
}
