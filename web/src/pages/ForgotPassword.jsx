import { useState } from 'react';
import { Link } from 'react-router-dom';
import { authApi } from '../api/endpoints.js';
import { apiError } from '../api/client.js';
import { Button, Field, Input, Alert } from '../components/ui.jsx';

export default function ForgotPassword() {
  const [email, setEmail] = useState('');
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setMessage('');
    setLoading(true);
    try {
      const { data } = await authApi.forgotPassword(email);
      setMessage(data.message || 'Si un compte existe, un lien de réinitialisation a été envoyé.');
    } catch (err) {
      setError(apiError(err));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex min-h-full items-center justify-center bg-slate-100 p-4">
      <div className="w-full max-w-sm rounded-2xl bg-white p-8 shadow-lg">
        <h1 className="mb-1 text-xl font-bold text-slate-800">Mot de passe oublié</h1>
        <p className="mb-6 text-sm text-slate-500">
          Saisissez votre email pour recevoir un lien de réinitialisation.
        </p>

        <form onSubmit={handleSubmit} className="space-y-4">
          <Alert type="error" onClose={() => setError('')}>{error}</Alert>
          <Alert type="success">{message}</Alert>
          <Field label="Email" required>
            <Input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required autoFocus />
          </Field>
          <Button type="submit" className="w-full" disabled={loading}>
            {loading ? 'Envoi…' : 'Envoyer le lien'}
          </Button>
        </form>

        <div className="mt-4 flex justify-between text-sm">
          <Link to="/login" className="text-brand-600 hover:underline">Retour à la connexion</Link>
          <Link to="/reset-password" className="text-slate-500 hover:underline">J'ai un token</Link>
        </div>
      </div>
    </div>
  );
}
