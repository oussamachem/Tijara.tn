import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { profileApi } from '../api/endpoints.js';
import { apiError } from '../api/client.js';
import { useAuth } from '../context/AuthContext.jsx';
import { Button, Field, Input, Alert, Spinner } from '../components/ui.jsx';

/** Écran Profil partagé (tous rôles) : éditer nom/email + changer le mot de passe. Route /profile. */
export default function Profile() {
  const navigate = useNavigate();
  const { user } = useAuth();
  const [form, setForm] = useState({ fullName: '', email: '' });
  const [loading, setLoading] = useState(true);
  const [savingP, setSavingP] = useState(false);
  const [okP, setOkP] = useState('');
  const [errP, setErrP] = useState('');

  const [pwd, setPwd] = useState({ oldPassword: '', newPassword: '' });
  const [savingPwd, setSavingPwd] = useState(false);
  const [okPwd, setOkPwd] = useState('');
  const [errPwd, setErrPwd] = useState('');

  useEffect(() => {
    profileApi.get()
      .then(({ data }) => setForm({ fullName: data.fullName || '', email: data.email || '' }))
      .catch((e) => setErrP(apiError(e)))
      .finally(() => setLoading(false));
  }, []);

  const saveProfile = async (e) => {
    e.preventDefault();
    setSavingP(true); setOkP(''); setErrP('');
    try {
      await profileApi.update({ fullName: form.fullName.trim(), email: form.email.trim() });
      setOkP('Profil mis à jour.');
    } catch (err) { setErrP(apiError(err)); } finally { setSavingP(false); }
  };

  const savePwd = async (e) => {
    e.preventDefault();
    if (pwd.newPassword.length < 6) { setErrPwd('Le nouveau mot de passe doit contenir au moins 6 caractères.'); return; }
    setSavingPwd(true); setOkPwd(''); setErrPwd('');
    try {
      await profileApi.changePassword({ oldPassword: pwd.oldPassword, newPassword: pwd.newPassword });
      setOkPwd('Mot de passe modifié.');
      setPwd({ oldPassword: '', newPassword: '' });
    } catch (err) { setErrPwd(apiError(err)); } finally { setSavingPwd(false); }
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
              <Field label="Email"><Input type="email" value={form.email} onChange={(e) => setForm((f) => ({ ...f, email: e.target.value }))} required /></Field>
              <Button type="submit" className="w-full" disabled={savingP}>{savingP ? 'Enregistrement…' : 'Enregistrer'}</Button>
            </form>

            <form onSubmit={savePwd} className="space-y-3 rounded-2xl bg-white p-5 shadow-sm">
              <h2 className="font-semibold text-slate-700">Changer le mot de passe</h2>
              <Alert type="error" onClose={() => setErrPwd('')}>{errPwd}</Alert>
              <Alert type="success" onClose={() => setOkPwd('')}>{okPwd}</Alert>
              <Field label="Mot de passe actuel"><Input type="password" autoComplete="current-password" value={pwd.oldPassword} onChange={(e) => setPwd((p) => ({ ...p, oldPassword: e.target.value }))} required /></Field>
              <Field label="Nouveau mot de passe"><Input type="password" autoComplete="new-password" value={pwd.newPassword} onChange={(e) => setPwd((p) => ({ ...p, newPassword: e.target.value }))} required /></Field>
              <Button type="submit" className="w-full" disabled={savingPwd}>{savingPwd ? 'Modification…' : 'Modifier le mot de passe'}</Button>
            </form>

            <p className="text-center text-xs text-slate-400">Connecté : {user?.email}</p>
          </>
        )}
      </div>
    </div>
  );
}
