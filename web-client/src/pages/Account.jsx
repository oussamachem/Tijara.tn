import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import Header from '../components/Header.jsx';
import { Button, EmptyState } from '../components/ui.jsx';

export default function Account() {
  const { user, isAuthenticated, logout } = useAuth();
  const navigate = useNavigate();

  if (!isAuthenticated) {
    return (
      <div>
        <Header title="Mon compte" />
        <EmptyState
          icon="👤"
          title="Bienvenue"
          sub="Connectez-vous ou créez un compte pour commander."
          action={
            <div className="mt-4 flex w-full max-w-xs flex-col gap-2">
              <Link to="/login"><Button className="w-full">Se connecter</Button></Link>
              <Link to="/register"><Button variant="secondary" className="w-full">Créer un compte</Button></Link>
            </div>
          }
        />
      </div>
    );
  }

  return (
    <div>
      <Header title="Mon compte" />
      <div className="p-4">
        <div className="flex items-center gap-4 rounded-2xl border border-slate-100 bg-white p-4 shadow-card">
          <div className="flex h-14 w-14 items-center justify-center rounded-full bg-brand-100 text-2xl font-black text-brand-700">
            {(user?.fullName || 'C').charAt(0).toUpperCase()}
          </div>
          <div className="min-w-0">
            <div className="truncate font-bold text-slate-800">{user?.fullName}</div>
            <div className="truncate text-sm text-slate-400">{user?.email}</div>
          </div>
        </div>

        <div className="mt-4 space-y-2">
          <Link to="/orders" className="flex items-center justify-between rounded-xl border border-slate-100 bg-white px-4 py-3.5 text-sm font-medium text-slate-700 shadow-card">
            📦 Mes commandes <span className="text-slate-300">›</span>
          </Link>
          <Link to="/" className="flex items-center justify-between rounded-xl border border-slate-100 bg-white px-4 py-3.5 text-sm font-medium text-slate-700 shadow-card">
            🏬 Découvrir les boutiques <span className="text-slate-300">›</span>
          </Link>
        </div>

        <Button variant="secondary" className="mt-6 w-full" onClick={() => { logout(); navigate('/'); }}>
          Se déconnecter
        </Button>
      </div>
    </div>
  );
}
