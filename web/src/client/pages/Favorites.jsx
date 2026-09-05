import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { favoritesApi } from '../api/endpoints';
import { apiError } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import Header from '../components/Header.jsx';
import { Spinner, ErrorNote, EmptyState } from '../components/ui.jsx';
import { money } from '../lib/format';

/**
 * « Mes favoris » (wishlist) — accessible depuis le cœur du header. Liste les produits favoris de
 * l'utilisateur (cross-boutique, champs publics). Retrait direct via le cœur de chaque carte.
 */
export default function Favorites() {
  const { isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!isAuthenticated) { setLoading(false); return; }
    favoritesApi.products()
      .then(({ data }) => setItems(Array.isArray(data) ? data : []))
      .catch((e) => setError(apiError(e)))
      .finally(() => setLoading(false));
  }, [isAuthenticated]);

  const removeFav = async (productId) => {
    const prev = items;
    setItems((list) => list.filter((p) => p.productId !== productId)); // optimiste
    try { await favoritesApi.remove(productId); } catch { setItems(prev); } // rollback si échec
  };

  return (
    <div>
      <Header title="Mes favoris" back />
      <div className="p-4">
        {!isAuthenticated ? (
          <EmptyState icon="❤️" title="Connectez-vous"
            sub="Connectez-vous pour retrouver vos produits favoris."
            action={<button onClick={() => navigate('/login')}
              className="rounded-full bg-slate-900 px-5 py-2 text-sm font-semibold text-white active:scale-95">Se connecter</button>} />
        ) : loading ? (
          <Spinner label="Chargement…" />
        ) : error ? (
          <ErrorNote message={error} />
        ) : items.length === 0 ? (
          <EmptyState icon="🤍" title="Aucun favori"
            sub="Touchez le ❤️ sur un produit pour le retrouver ici." />
        ) : (
          <ul className="grid grid-cols-2 gap-3.5">
            {items.map((p) => (
              <li key={`${p.shopSlug}-${p.productId}`} className="relative">
                <Link to={`/s/${p.shopSlug}/p/${p.productId}`} state={{ shopName: p.shopName }}
                  className="group block overflow-hidden rounded-2xl border border-slate-100 bg-white shadow-sm transition hover:shadow-md active:scale-[.99]">
                  <div className="aspect-[3/4] w-full overflow-hidden bg-slate-100"><FavThumb src={p.imageUrl} alt={p.name} /></div>
                  <div className="p-3">
                    <div className="line-clamp-1 text-sm font-medium text-slate-900">{p.name}</div>
                    <div className="mt-1 truncate text-[11px] text-slate-400">{p.shopName}</div>
                    <div className="mt-1.5 text-[15px] font-extrabold text-slate-900">{money(p.price)}</div>
                  </div>
                </Link>
                <button onClick={() => removeFav(p.productId)} aria-label={`Retirer ${p.name} des favoris`}
                  className="absolute right-2 top-2 flex h-9 w-9 items-center justify-center rounded-full bg-white/90 text-base text-rose-500 shadow ring-1 ring-slate-200 active:scale-90">
                  ❤️
                </button>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  );
}

/** Image OU placeholder — bascule sur le placeholder si l'image manque OU 404 (jamais de trou béant). */
function FavThumb({ src, alt }) {
  const [broken, setBroken] = useState(false);
  if (!src || broken) {
    return <div className="flex h-full w-full items-center justify-center bg-gradient-to-br from-slate-50 to-slate-100 text-4xl">🧥</div>;
  }
  return <img src={src} alt={alt} loading="lazy" decoding="async" onError={() => setBroken(true)} className="h-full w-full object-cover" />;
}
