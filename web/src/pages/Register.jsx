import { useState } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext.jsx';
import { apiError } from '../api/client.js';
import { Button, Field, Input, Alert } from '../components/ui.jsx';

/** Inscription CLIENT (compte global). Après création, l'espace client s'ouvre automatiquement. */
export default function Register() {
  const { register } = useAuth();
  const navigate = useNavigate();
  const { state } = useLocation();
  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (password.length < 6) {
      setError('Le mot de passe doit contenir au moins 6 caractères.');
      return;
    }
    setError('');
    setLoading(true);
    try {
      await register(fullName.trim(), email.trim(), password);
      navigate(state?.from || '/', { replace: true });
    } catch (err) {
      setError(apiError(err, 'Inscription impossible'));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex min-h-full items-center justify-center bg-slate-100 p-4">
      <div className="w-full max-w-sm rounded-2xl bg-white p-8 shadow-lg">
        <div className="mb-6 text-center">
          <div className="text-3xl">🎉</div>
          <h1 className="mt-2 text-xl font-bold text-slate-800">Créer un compte</h1>
          <p className="text-sm text-slate-500">Un seul compte pour commander partout</p>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          <Alert type="error" onClose={() => setError('')}>{error}</Alert>
          <Field label="Nom complet" required>
            <Input value={fullName} onChange={(e) => setFullName(e.target.value)} placeholder="Prénom Nom" required autoFocus />
          </Field>
          <Field label="Email" required>
            <Input type="email" value={email} onChange={(e) => setEmail(e.target.value)}
              placeholder="vous@email.com" required autoComplete="email" />
          </Field>
          <Field label="Mot de passe" required>
            <Input type="password" value={password} onChange={(e) => setPassword(e.target.value)}
              placeholder="Au moins 6 caractères" required autoComplete="new-password" />
          </Field>
          <Button type="submit" className="w-full" disabled={loading}>
            {loading ? 'Création…' : 'Créer mon compte'}
          </Button>
        </form>

        <div className="mt-4 text-center text-sm">
          Déjà inscrit ? <Link to="/login" className="font-semibold text-brand-600 hover:underline">Se connecter</Link>
        </div>
      </div>
    </div>
  );
}
