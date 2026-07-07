import { useEffect, useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { shopsApi } from '../api/endpoints';
import { apiError } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import Header from '../components/Header.jsx';
import { Button, Spinner, ErrorNote, EmptyState, Badge } from '../components/ui.jsx';
import { money, formatDate, STATUS_LABEL, STATUS_STYLE } from '../lib/format';

export default function Orders() {
  const { isAuthenticated } = useAuth();
  const { state } = useLocation();
  const [shop, setShop] = useState(null);
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [flash, setFlash] = useState(state?.justOrdered || '');

  useEffect(() => {
    try { setShop(JSON.parse(localStorage.getItem('sbc_lastShop') || 'null')); } catch { setShop(null); }
  }, []);

  const load = (slug) => {
    setLoading(true);
    setError('');
    shopsApi
      .myOrders(slug)
      .then(({ data }) => setOrders(data))
      .catch((e) => setError(apiError(e)))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    if (!isAuthenticated) { setLoading(false); return; }
    if (shop?.slug) load(shop.slug);
    else setLoading(false);
  }, [shop, isAuthenticated]);

  if (!isAuthenticated) {
    return (
      <div>
        <Header title="Mes commandes" />
        <EmptyState icon="🔒" title="Connectez-vous" sub="Suivez vos commandes après connexion."
          action={<Link to="/login" className="mt-3"><Button>Se connecter</Button></Link>} />
      </div>
    );
  }

  return (
    <div>
      <Header title="Mes commandes" subtitle={shop?.name} />
      <div className="space-y-3 p-4">
        {flash && (
          <div className="flex items-center justify-between rounded-xl bg-emerald-50 px-4 py-3 text-sm font-semibold text-emerald-700">
            <span>✓ Commande {flash} envoyée !</span>
            <button onClick={() => setFlash('')} className="text-emerald-500">✕</button>
          </div>
        )}
        <ErrorNote message={error} onRetry={() => shop?.slug && load(shop.slug)} />

        {loading ? (
          <Spinner label="Chargement…" />
        ) : !shop?.slug ? (
          <EmptyState icon="📦" title="Aucune commande" sub="Visitez une boutique pour commander."
            action={<Link to="/" className="mt-3"><Button>Voir les boutiques</Button></Link>} />
        ) : orders.length === 0 ? (
          <EmptyState icon="📦" title="Aucune commande" sub={`Pas encore de commande chez ${shop.name}.`}
            action={<Link to={`/s/${shop.slug}`} className="mt-3"><Button>Voir le catalogue</Button></Link>} />
        ) : (
          orders.map((o) => (
            <div key={o.id} className="rounded-2xl border border-slate-100 bg-white p-4 shadow-card">
              <div className="flex items-center justify-between">
                <span className="font-bold text-slate-800">{o.reference}</span>
                <Badge className={STATUS_STYLE[o.status]}>{STATUS_LABEL[o.status] || o.status}</Badge>
              </div>
              <div className="mt-0.5 text-xs text-slate-400">{formatDate(o.createdAt)}</div>
              <ul className="mt-2 space-y-1">
                {o.items.map((it, i) => (
                  <li key={i} className="flex justify-between text-sm text-slate-600">
                    <span className="truncate">{it.productName} · {it.color}/{it.size} × {it.quantity}</span>
                    <span className="ml-2 shrink-0 font-medium">{money(it.unitPrice * it.quantity)}</span>
                  </li>
                ))}
              </ul>
              <div className="mt-2 flex justify-between border-t border-slate-100 pt-2">
                <span className="text-sm text-slate-500">Total</span>
                <span className="font-extrabold text-brand-700">{money(o.total)}</span>
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  );
}
