import { useState } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { apiError } from '../api/client';
import Header from '../components/Header.jsx';
import { Button, Input, Field, ErrorNote } from '../components/ui.jsx';

export default function Register() {
  const { register } = useAuth();
  const navigate = useNavigate();
  const { state } = useLocation();
  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const submit = async (e) => {
    e.preventDefault();
    if (password.length < 6) { setError('Le mot de passe doit contenir au moins 6 caractères.'); return; }
    setBusy(true);
    setError('');
    try {
      await register(fullName.trim(), email.trim(), password);
      navigate(state?.from || '/', { replace: true });
    } catch (err) {
      setError(apiError(err));
    } finally {
      setBusy(false);
    }
  };

  return (
    <div>
      <Header title="Créer un compte" back />
      <form onSubmit={submit} className="space-y-4 p-4">
        <div className="py-2 text-center">
          <div className="text-4xl">🎉</div>
          <p className="mt-1 text-sm text-slate-500">Un seul compte pour commander dans toutes les boutiques.</p>
        </div>
        <Field label="Nom complet"><Input value={fullName} onChange={(e) => setFullName(e.target.value)} placeholder="Prénom Nom" required /></Field>
        <Field label="Email"><Input type="email" autoComplete="email" value={email} onChange={(e) => setEmail(e.target.value)} placeholder="vous@email.com" required /></Field>
        <Field label="Mot de passe"><Input type="password" autoComplete="new-password" value={password} onChange={(e) => setPassword(e.target.value)} placeholder="Au moins 6 caractères" required /></Field>
        <ErrorNote message={error} />
        <Button className="w-full" type="submit" disabled={busy}>{busy ? 'Création…' : 'Créer mon compte'}</Button>
        <p className="text-center text-sm text-slate-500">
          Déjà inscrit ?{' '}
          <Link to="/login" state={state} className="font-semibold text-brand-600">Se connecter</Link>
        </p>
      </form>
    </div>
  );
}
