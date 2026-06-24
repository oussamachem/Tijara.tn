import { useState, useEffect } from 'react';
import { Link, useSearchParams, useNavigate } from 'react-router-dom';
import { authApi } from '../api/endpoints.js';
import { apiError } from '../api/client.js';
import { Button, Field, Input, Alert } from '../components/ui.jsx';

export default function ResetPassword() {
  const [params] = useSearchParams();
  const navigate = useNavigate();
  const [token, setToken] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const [loading, setLoading] = useState(false);

  // Pré-remplit le token s'il est passé en query string (?token=...).
  useEffect(() => {
    const t = params.get('token');
    if (t) setToken(t);
  }, [params]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setMessage('');
    setLoading(true);
    try {
      const { data } = await authApi.resetPassword(token, password);
      setMessage(data.message || 'Mot de passe réinitialisé.');
      setTimeout(() => navigate('/login'), 1500);
    } catch (err) {
      setError(apiError(err));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex min-h-full items-center justify-center bg-slate-100 p-4">
      <div className="w-full max-w-sm rounded-2xl bg-white p-8 shadow-lg">
        <h1 className="mb-1 text-xl font-bold text-slate-800">Réinitialisation</h1>
        <p className="mb-6 text-sm text-slate-500">Saisissez le token reçu et votre nouveau mot de passe.</p>

        <form onSubmit={handleSubmit} className="space-y-4">
          <Alert type="error" onClose={() => setError('')}>{error}</Alert>
          <Alert type="success">{message}</Alert>
          <Field label="Token" required>
            <Input value={token} onChange={(e) => setToken(e.target.value)} required />
          </Field>
          <Field label="Nouveau mot de passe" required>
            <Input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              minLength={6}
              required
            />
          </Field>
          <Button type="submit" className="w-full" disabled={loading}>
            {loading ? 'Validation…' : 'Réinitialiser'}
          </Button>
        </form>

        <div className="mt-4 text-center text-sm">
          <Link to="/login" className="text-brand-600 hover:underline">Retour à la connexion</Link>
        </div>
      </div>
    </div>
  );
}
