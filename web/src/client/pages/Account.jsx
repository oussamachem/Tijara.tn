import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { useShop } from '../../context/ShopContext.jsx';
import { myShopApi, notificationsApi } from '../api/endpoints';
import { apiError } from '../api/client';
import ShopSwitcher from '../../components/ShopSwitcher.jsx';
import Header from '../components/Header.jsx';
import { Button, Input, EmptyState, ErrorNote } from '../components/ui.jsx';

export default function Account() {
  const { user, isAuthenticated, logout } = useAuth();
  const { memberships, isPlatformAdmin, refresh, switchToShop } = useShop();
  const navigate = useNavigate();

  const [unread, setUnread] = useState(0);
  const [creating, setCreating] = useState(false); // formulaire ouvert
  const [name, setName] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!isAuthenticated) return;
    notificationsApi.unreadCount().then(({ data }) => setUnread(data.count || 0)).catch(() => {});
  }, [isAuthenticated]);

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

  const createShop = async () => {
    if (!name.trim()) { setError('Le nom de la boutique est obligatoire.'); return; }
    setBusy(true); setError('');
    try {
      const { data } = await myShopApi.create(name.trim());
      await refresh();            // recharge les memberships (inclut la nouvelle boutique)
      switchToShop(data.id);      // bascule sur l'espace propriétaire
      navigate('/');
    } catch (e) {
      setError(apiError(e));
    } finally { setBusy(false); }
  };

  const hasSpaces = memberships.length > 0 || isPlatformAdmin;

  return (
    <div>
      <Header title="Mon compte" />
      <div className="space-y-4 p-4">
        <div className="flex items-center gap-4 rounded-2xl border border-slate-100 bg-white p-4 shadow-card">
          <div className="flex h-14 w-14 items-center justify-center rounded-full bg-brand-100 text-2xl font-black text-brand-700">
            {(user?.fullName || 'C').charAt(0).toUpperCase()}
          </div>
          <div className="min-w-0">
            <div className="truncate font-bold text-slate-800">{user?.fullName}</div>
            <div className="truncate text-sm text-slate-400">{user?.email}</div>
          </div>
        </div>

        {/* Bascule vers mes espaces boutique (propriétaire / vendeur / plateforme) */}
        {hasSpaces && (
          <div className="flex items-center justify-between rounded-xl border border-slate-100 bg-white px-4 py-3 shadow-card">
            <span className="text-sm font-medium text-slate-600">Mes espaces</span>
            <ShopSwitcher />
          </div>
        )}

        <div className="space-y-2">
          <Link to="/orders" className="flex items-center justify-between rounded-xl border border-slate-100 bg-white px-4 py-3.5 text-sm font-medium text-slate-700 shadow-card">
            📦 Mes commandes <span className="text-slate-300">›</span>
          </Link>
          <Link to="/notifications" className="flex items-center justify-between rounded-xl border border-slate-100 bg-white px-4 py-3.5 text-sm font-medium text-slate-700 shadow-card">
            <span>🔔 Notifications</span>
            {unread > 0
              ? <span className="rounded-full bg-rose-500 px-2 py-0.5 text-[11px] font-bold text-white">{unread}</span>
              : <span className="text-slate-300">›</span>}
          </Link>
          <Link to="/profile" className="flex items-center justify-between rounded-xl border border-slate-100 bg-white px-4 py-3.5 text-sm font-medium text-slate-700 shadow-card">
            ⚙️ Mon profil <span className="text-slate-300">›</span>
          </Link>
          <Link to="/" className="flex items-center justify-between rounded-xl border border-slate-100 bg-white px-4 py-3.5 text-sm font-medium text-slate-700 shadow-card">
            🏬 Découvrir les boutiques <span className="text-slate-300">›</span>
          </Link>
        </div>

        {/* Self-service : créer ma boutique (Phase B) */}
        <div className="rounded-2xl border border-slate-100 bg-white p-4 shadow-card">
          {!creating ? (
            <button onClick={() => { setCreating(true); setError(''); }} className="flex w-full items-center justify-between text-sm font-semibold text-brand-700">
              🏪 Créer ma boutique <span className="rounded-full bg-brand-100 px-2 py-0.5 text-[10px] font-bold text-brand-700">Nouveau</span>
            </button>
          ) : (
            <div className="space-y-3">
              <p className="text-sm font-semibold text-slate-700">Créer ma boutique</p>
              <p className="text-xs text-slate-400">Vous en devenez le propriétaire. Vous pourrez ensuite ajouter produits et vendeurs.</p>
              <ErrorNote message={error} />
              <Input value={name} onChange={(e) => setName(e.target.value)} placeholder="Nom de la boutique" autoFocus />
              <div className="flex gap-2">
                <Button className="flex-1" disabled={busy} onClick={createShop}>{busy ? 'Création…' : 'Créer'}</Button>
                <Button variant="secondary" onClick={() => { setCreating(false); setName(''); setError(''); }}>Annuler</Button>
              </div>
            </div>
          )}
        </div>

        <Button variant="secondary" className="w-full" onClick={() => { logout(); navigate('/'); }}>
          Se déconnecter
        </Button>
      </div>
    </div>
  );
}
