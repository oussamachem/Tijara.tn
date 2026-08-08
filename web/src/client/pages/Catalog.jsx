import { useEffect, useMemo, useState } from 'react';
import { Link, useParams, useLocation, useNavigate } from 'react-router-dom';
import { shopsApi } from '../api/endpoints';
import { apiError } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { Input, Spinner, ErrorNote, EmptyState } from '../components/ui.jsx';
import { money, compact } from '../lib/format';

/** Vitrine d'une boutique : bandeau (logo + suivre + galerie), recherche/tri, grille de produits. */
export default function Catalog() {
  const { slug } = useParams();
  const { state } = useLocation();
  const navigate = useNavigate();
  const { isAuthenticated } = useAuth();
  const [shopName, setShopName] = useState(state?.shopName || slug);
  const [logo, setLogo] = useState(null);
  const [following, setFollowing] = useState(false);
  const [followBusy, setFollowBusy] = useState(false);
  const [stats, setStats] = useState(null);
  const [copied, setCopied] = useState(false);
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [q, setQ] = useState('');
  const [sort, setSort] = useState('name');

  const load = () => {
    setLoading(true); setError('');
    shopsApi.catalog(slug)
      .then(({ data }) => setProducts(data))
      .catch((e) => setError(apiError(e)))
      .finally(() => setLoading(false));
  };
  useEffect(load, [slug]);

  // Fiche boutique (nom + logo + following) via l'endpoint dédié.
  useEffect(() => {
    shopsApi.shop(slug)
      .then(({ data }) => {
        setShopName(data.name); setLogo(data.logoUrl); setFollowing(!!data.following);
        localStorage.setItem('sbc_lastShop', JSON.stringify({ slug, name: data.name }));
      })
      .catch(() => { localStorage.setItem('sbc_lastShop', JSON.stringify({ slug, name: state?.shopName || slug })); });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [slug]);

  // Stats publiques (profil façon TikTok) : abonnés, ventes, produits.
  const loadStats = () => { shopsApi.stats(slug).then(({ data }) => setStats(data)).catch(() => {}); };
  useEffect(loadStats, [slug]); // eslint-disable-line react-hooks/exhaustive-deps

  const toggleFollow = async () => {
    if (!isAuthenticated) { navigate('/login', { state: { from: `/s/${slug}` } }); return; }
    setFollowBusy(true);
    try {
      if (following) { await shopsApi.unfollow(slug); setFollowing(false); }
      else { await shopsApi.follow(slug); setFollowing(true); }
      loadStats(); // rafraîchit le compteur d'abonnés
    } catch { /* silencieux */ } finally { setFollowBusy(false); }
  };

  // Partager le lien de la boutique (share natif mobile, sinon copie presse-papiers).
  const shareShop = async () => {
    const link = `${window.location.origin}/s/${slug}`;
    if (navigator.share) {
      try { await navigator.share({ title: shopName, text: `Découvrez ${shopName} sur Smart Boutique`, url: link }); } catch { /* annulé */ }
    } else {
      try { await navigator.clipboard.writeText(link); setCopied(true); setTimeout(() => setCopied(false), 1800); } catch { /* presse-papiers indispo */ }
    }
  };

  const filtered = useMemo(() => {
    const term = q.trim().toLowerCase();
    let list = products.filter(
      (p) => !term || p.name.toLowerCase().includes(term) || (p.reference || '').toLowerCase().includes(term)
    );
    if (sort === 'price_asc') list = [...list].sort((a, b) => a.price - b.price);
    else if (sort === 'price_desc') list = [...list].sort((a, b) => b.price - a.price);
    else list = [...list].sort((a, b) => a.name.localeCompare(b.name));
    return list;
  }, [products, q, sort]);

  return (
    <div>
      {/* Bandeau vitrine — profil centré façon TikTok */}
      <header className="safe-top relative bg-gradient-to-br from-brand-600 to-brand-800 px-4 pb-5 pt-4 text-white">
        <button onClick={() => navigate(-1)} aria-label="Retour"
          className="absolute left-3 top-4 flex h-8 w-8 items-center justify-center rounded-full text-white/90 hover:bg-white/10">
          <span className="text-xl leading-none">‹</span>
        </button>

        {/* Avatar rond centré + nom + @handle */}
        <div className="flex flex-col items-center pt-1.5">
          <div className="flex h-20 w-20 items-center justify-center overflow-hidden rounded-full bg-white/20 text-3xl font-black ring-2 ring-white/40 backdrop-blur">
            {logo ? <img src={logo} alt={shopName} className="h-full w-full object-cover" /> : (shopName || '?').charAt(0).toUpperCase()}
          </div>
          <h1 className="mt-2 max-w-full truncate px-2 text-center text-xl font-extrabold">{shopName}</h1>
          <p className="text-center text-sm text-brand-100">@{slug}</p>
        </div>

        {/* Stats centrées avec séparateurs verticaux (façon TikTok) */}
        <div className="mt-3 flex items-center justify-center">
          <Stat n={stats?.followers} label="Abonnés" />
          <Sep />
          <Stat n={stats?.sales} label="Ventes" />
          <Sep />
          <Stat n={stats?.products} label="Produits" />
        </div>

        {/* Actions centrées : Suivre + Partager le lien de la boutique */}
        <div className="mt-4 flex items-center justify-center gap-2">
          <button onClick={toggleFollow} disabled={followBusy}
            className={`rounded-full px-7 py-2 text-sm font-bold transition ${
              following ? 'bg-white/20 text-white ring-1 ring-white/60' : 'bg-white text-brand-700'}`}>
            {following ? '✓ Suivi' : '＋ Suivre'}
          </button>
          <button onClick={shareShop}
            className="rounded-full bg-white/15 px-5 py-2 text-sm font-semibold text-white ring-1 ring-white/30">
            🔗 Partager
          </button>
        </div>
        {copied && <div className="mt-2 text-center text-xs font-semibold text-white/90">✓ Lien copié</div>}
      </header>

      {/* Recherche + tri */}
      <div className="sticky top-0 z-10 space-y-2 border-b border-slate-100 bg-slate-50/95 px-4 py-3 backdrop-blur">
        <Input value={q} onChange={(e) => setQ(e.target.value)} placeholder="🔍 Rechercher un article…" />
        <div className="no-scrollbar flex gap-2 overflow-x-auto">
          {[['name', 'A–Z'], ['price_asc', 'Prix ↑'], ['price_desc', 'Prix ↓']].map(([key, label]) => (
            <button key={key} onClick={() => setSort(key)}
              className={`whitespace-nowrap rounded-full px-3 py-1.5 text-sm font-medium ${
                sort === key ? 'bg-brand-600 text-white' : 'border border-slate-200 bg-white text-slate-600'}`}>
              {label}
            </button>
          ))}
        </div>
      </div>

      <div className="p-4">
        <ErrorNote message={error} onRetry={load} />
        {loading ? (
          <Spinner label="Chargement du catalogue…" />
        ) : filtered.length === 0 ? (
          <EmptyState icon="👗" title="Aucun article" sub={q ? 'Aucun résultat.' : 'Cette boutique n’a pas encore de produit disponible.'} />
        ) : (
          <ul className="grid grid-cols-2 gap-3">
            {filtered.map((p) => (
              <li key={p.productId}>
                <Link to={`/s/${slug}/p/${p.productId}`} state={{ product: p, shopName }}
                  className="flex h-full flex-col overflow-hidden rounded-2xl border border-slate-100 bg-white shadow-card active:scale-[.99]">
                  {p.images && p.images.length > 0 ? (
                    <img src={p.images[0].url} alt={p.name} loading="lazy" decoding="async" className="aspect-square w-full bg-slate-100 object-cover" />
                  ) : (
                    <div className="flex aspect-square items-center justify-center bg-gradient-to-br from-brand-50 to-slate-100 text-4xl">🧥</div>
                  )}
                  <div className="flex flex-1 flex-col p-3">
                    <div className="line-clamp-2 text-sm font-semibold text-slate-800">{p.name}</div>
                    <div className="mt-1 text-xs text-slate-400">{p.variants.length} taille(s)/couleur(s)</div>
                    <div className="mt-auto pt-2 font-extrabold text-brand-700">{money(p.price)}</div>
                  </div>
                </Link>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  );
}

/** Une stat de profil (grand nombre + libellé), façon TikTok. */
function Stat({ n, label }) {
  return (
    <div className="px-6 text-center">
      <div className="text-2xl font-extrabold leading-none">{compact(n ?? 0)}</div>
      <div className="mt-1 text-xs text-white/80">{label}</div>
    </div>
  );
}

/** Séparateur vertical entre deux stats (façon TikTok). */
function Sep() {
  return <div className="h-8 w-px self-center bg-white/25" />;
}
