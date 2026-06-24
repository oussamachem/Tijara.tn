import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext.jsx';
import { apiError } from '../api/client.js';
import { Button, Field, Input, Alert } from '../components/ui.jsx';

export default function Login() {
  const { login, logout } = useAuth();
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const user = await login(email, password);
      if (user.role !== 'ADMIN') {
        logout();
        setError("Accès réservé aux administrateurs. L'application vendeur est sur mobile.");
        return;
      }
      navigate('/');
    } catch (err) {
      setError(apiError(err, 'Connexion impossible'));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex min-h-full items-center justify-center bg-slate-100 p-4">
      <div className="w-full max-w-sm rounded-2xl bg-white p-8 shadow-lg">
        <div className="mb-6 text-center">
          <div className="text-3xl">🛍️</div>
          <h1 className="mt-2 text-xl font-bold text-slate-800">Smart Boutique</h1>
          <p className="text-sm text-slate-500">Espace administrateur</p>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          <Alert type="error" onClose={() => setError('')}>
            {error}
          </Alert>
          <Field label="Email" required>
            <Input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="admin@smartboutique.com"
              required
              autoFocus
            />
          </Field>
          <Field label="Mot de passe" required>
            <Input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="••••••••"
              required
            />
          </Field>
          <Button type="submit" className="w-full" disabled={loading}>
            {loading ? 'Connexion…' : 'Se connecter'}
          </Button>
        </form>

        <div className="mt-4 text-center text-sm">
          <Link to="/forgot-password" className="text-brand-600 hover:underline">
            Mot de passe oublié ?
          </Link>
        </div>
      </div>
    </div>
  );
}
