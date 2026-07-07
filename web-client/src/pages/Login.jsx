import { useState } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { apiError } from '../api/client';
import Header from '../components/Header.jsx';
import { Button, Input, Field, ErrorNote } from '../components/ui.jsx';

export default function Login() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const { state } = useLocation();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const submit = async (e) => {
    e.preventDefault();
    setBusy(true);
    setError('');
    try {
      await login(email.trim(), password);
      navigate(state?.from || '/', { replace: true });
    } catch (err) {
      setError(apiError(err, 'Identifiants invalides'));
    } finally {
      setBusy(false);
    }
  };

  return (
    <div>
      <Header title="Connexion" back />
      <form onSubmit={submit} className="space-y-4 p-4">
        <div className="py-2 text-center">
          <div className="text-4xl">👋</div>
          <p className="mt-1 text-sm text-slate-500">Connectez-vous pour commander et suivre vos achats.</p>
        </div>
        <Field label="Email"><Input type="email" autoComplete="email" value={email} onChange={(e) => setEmail(e.target.value)} placeholder="vous@email.com" required /></Field>
        <Field label="Mot de passe"><Input type="password" autoComplete="current-password" value={password} onChange={(e) => setPassword(e.target.value)} placeholder="••••••••" required /></Field>
        <ErrorNote message={error} />
        <Button className="w-full" type="submit" disabled={busy}>{busy ? 'Connexion…' : 'Se connecter'}</Button>
        <p className="text-center text-sm text-slate-500">
          Pas de compte ?{' '}
          <Link to="/register" state={state} className="font-semibold text-brand-600">Créer un compte</Link>
        </p>
      </form>
    </div>
  );
}
