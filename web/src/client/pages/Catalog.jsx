import { useEffect, useMemo, useState } from 'react';
import { Link, useParams, useLocation, useNavigate } from 'react-router-dom';
import { shopsApi } from '../api/endpoints';
import { apiError } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { Spinner, ErrorNote, EmptyState } from '../components/ui.jsx';
import { money, compact } from '../lib/format';

const SORTS = [
  ['default', 'Pertinence'],
  ['name', 'Nom A–Z'],
  ['price_asc', 'Prix croissant'],
  ['price_desc', 'Prix décroissant'],
];

/**
 * Vitrine publique d'une boutique — design premium (fashion e-commerce). Fonctionnalités inchangées :
 * recherche, tri, suivre, partager, stats, navigation produit, states (chargement/vide/erreur).
 */
export default function Catalog() {
  const { slug } = useParams();
  const { state } = useLocation();
  const navigate = useNavigate();
  const { isAuthenticated } = useAuth();

  const [shopName, setShopName] = useState(state?.shopName || slug);
  const [logo, setLogo] = useState(null);
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [q, setQ] = useState('');
  const [sort, setSort] = useState('default');
  const [sortOpen, setSortOpen] = useState(false);
  const [menuOpen, setMenuOpen] = useState(false);
  const [following, setFollowing] = useState(false);
  const [followBusy, setFollowBusy] = useState(false);
  const [stats, setStats] = useState(null);
  const [copied, setCopied] = useState(false);

  const load = () => {
    setLoading(true); setError('');
    shopsApi.catalog(slug)
      .then(({ data }) => setProducts(data))
      .catch((e) => setError(apiError(e)))
      .finally(() => setLoading(false));
  };
  useEffect(load, [slug]);

  // Fiche boutique (nom + logo + following).
  useEffect(() => {
    shopsApi.shop(slug)
      .then(({ data }) => {
        setShopName(data.name); setLogo(data.logoUrl); setFollowing(!!data.following);
        localStorage.setItem('sbc_lastShop', JSON.stringify({ slug, name: data.name }));
      })
      .catch(() => { localStorage.setItem('sbc_lastShop', JSON.stringify({ slug, name: state?.shopName || slug })); });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [slug]);

  // Stats publiques (abonnés / ventes / produits).
  const loadStats = () => { shopsApi.stats(slug).then(({ data }) => setStats(data)).catch(() => {}); };
  useEffect(loadStats, [slug]); // eslint-disable-line react-hooks/exhaustive-deps

  const toggleFollow = async () => {
    if (!isAuthenticated) { navigate('/login', { state: { from: `/s/${slug}` } }); return; }
    setFollowBusy(true);
    try {
      if (following) { await shopsApi.unfollow(slug); setFollowing(false); }
      else { await shopsApi.follow(slug); setFollowing(true); }
      loadStats();
    } catch { /* silencieux */ } finally { setFollowBusy(false); }
  };

  const shareShop = async () => {
    const link = `${window.location.origin}/s/${slug}`;
    if (navigator.share) {
      try { await navigator.share({ title: shopName, text: `Découvrez ${shopName} sur Smart Boutique`, url: link }); } catch { /* annulé */ }
    } else {
      try { await navigator.clipboard.writeText(link); setCopied(true); setTimeout(() => setCopied(false), 1800); } catch { /* indispo */ }
    }
  };

  const filtered = useMemo(() => {
    const term = q.trim().toLowerCase();
    let list = products.filter((p) => !term || p.name.toLowerCase().includes(term) || (p.reference || '').toLowerCase().includes(term));
    if (sort === 'price_asc') list = [...list].sort((a, b) => a.price - b.price);
    else if (sort === 'price_desc') list = [...list].sort((a, b) => b.price - a.price);
    else if (sort === 'name') list = [...list].sort((a, b) => a.name.localeCompare(b.name));
    return list;
  }, [products, q, sort]);

  const sortLabel = SORTS.find(([k]) => k === sort)?.[1] || 'Pertinence';

  return (
    <div className="min-h-full bg-[#FAFAFA]">
      {/* ---------- Cover + en-tête ---------- */}
      <div className="relative">
        <div className="relative h-28 w-full overflow-hidden bg-gradient-to-br from-slate-800 to-slate-600">
          {logo && <img src={logo} alt="" aria-hidden className="h-full w-full object-cover" />}
          <div className="absolute inset-0 bg-gradient-to-t from-black/35 via-black/5 to-black/10" />
        </div>

        <button onClick={() => navigate(-1)} aria-label="Retour"
          className="absolute left-4 top-4 flex h-10 w-10 items-center justify-center rounded-full bg-white/95 text-slate-800 shadow-md ring-1 ring-black/5 transition active:scale-95">
          <span className="text-2xl leading-none">‹</span>
        </button>

        <div className="absolute right-4 top-4">
          <button onClick={() => setMenuOpen((o) => !o)} aria-label="Plus"
            className="flex h-10 w-10 items-center justify-center rounded-full bg-white/95 text-slate-800 shadow-md ring-1 ring-black/5 transition active:scale-95">
            <span className="text-xl leading-none">⋮</span>
          </button>
          {menuOpen && (
            <>
              <div className="fixed inset-0 z-10" onClick={() => setMenuOpen(false)} />
              <div className="absolute right-0 z-20 mt-2 w-48 overflow-hidden rounded-xl border border-slate-100 bg-white shadow-xl">
                <button onClick={() => { setMenuOpen(false); shareShop(); }}
                  className="block w-full px-4 py-3 text-left text-sm text-slate-700 hover:bg-slate-50">🔗 Copier le lien</button>
                <Link to={`/s/${slug}/gallery`} state={{ shopName }} onClick={() => setMenuOpen(false)}
                  className="block border-t border-slate-100 px-4 py-3 text-sm text-slate-700 hover:bg-slate-50">📸 Galerie photos</Link>
              </div>
            </>
          )}
        </div>
      </div>

      {/* ---------- Identité (centrée) ---------- */}
      <div className="px-4 text-center">
        {/* Logo pleinement visible (anneau blanc épais) — centré, plus de "demi-cercle" */}
        <div className="mx-auto -mt-12 h-24 w-24 rounded-full bg-white p-1.5 shadow-lg ring-1 ring-black/5">
          <div className="flex h-full w-full items-center justify-center overflow-hidden rounded-full bg-slate-100 text-3xl font-black text-slate-400">
            {logo ? <img src={logo} alt={shopName} className="h-full w-full object-cover" /> : (shopName || '?').charAt(0).toUpperCase()}
          </div>
        </div>
        <h1 className="mt-2.5 text-xl font-extrabold leading-tight tracking-tight text-slate-900">{shopName}</h1>
        <p className="text-sm text-slate-500">@{slug}</p>

        {/* Stats publiques centrées : abonnés · produits (icônes décoratives ; ventes non mises en avant) */}
        <div className="mt-2 flex items-center justify-center gap-3 text-sm text-slate-500">
          <span className="inline-flex items-center gap-1.5">
            <UserIcon /><span className="font-bold text-slate-900">{compact(stats?.followers ?? 0)}</span> abonné{(stats?.followers ?? 0) > 1 ? 's' : ''}
          </span>
          <span className="text-slate-300">•</span>
          <span className="inline-flex items-center gap-1.5">
            <BagIcon /><span className="font-bold text-slate-900">{stats?.products ?? products.length}</span> produit{(stats?.products ?? products.length) > 1 ? 's' : ''}
          </span>
        </div>

        {/* Actions centrées */}
        <div className="mt-3.5 flex justify-center gap-2.5">
          <button onClick={toggleFollow} disabled={followBusy}
            className={`rounded-full px-8 py-2.5 text-sm font-semibold transition active:scale-[.98] disabled:opacity-60 ${
              following ? 'border border-slate-300 bg-white text-slate-900' : 'bg-slate-900 text-white hover:bg-slate-800'}`}>
            {following ? '✓ Suivi' : '+ Suivre'}
          </button>
          <button onClick={shareShop}
            className="flex items-center justify-center gap-2 rounded-full border border-slate-300 bg-white px-6 py-2.5 text-sm font-semibold text-slate-900 transition hover:bg-slate-50 active:scale-[.98]">
            <ShareIcon /> Partager
          </button>
        </div>
        {copied && <p className="mt-2 text-xs font-medium text-slate-500">✓ Lien copié</p>}
      </div>

      {/* ---------- Recherche ---------- */}
      <div className="px-4 pt-4">
        <div className="flex items-center gap-2.5 rounded-2xl border border-slate-200 bg-white px-4 py-3 transition focus-within:border-slate-400 focus-within:ring-4 focus-within:ring-slate-100">
          <SearchIcon />
          <input value={q} onChange={(e) => setQ(e.target.value)} placeholder="Rechercher dans la boutique…"
            className="min-w-0 flex-1 bg-transparent text-[15px] text-slate-800 outline-none placeholder:text-slate-400" />
          {q && <button onClick={() => setQ('')} className="text-slate-400 hover:text-slate-600" aria-label="Effacer">✕</button>}
        </div>
      </div>

      {/* ---------- Filtre / tri ---------- */}
      <div className="flex items-center justify-between px-4 pt-4">
        <span className="rounded-full bg-slate-900 px-4 py-1.5 text-sm font-semibold text-white">Tous</span>
        <div className="relative">
          <button onClick={() => setSortOpen((o) => !o)}
            className="flex items-center gap-1.5 rounded-full border border-slate-200 bg-white px-3.5 py-1.5 text-sm font-medium text-slate-700 transition hover:bg-slate-50 active:scale-95">
            <SortIcon /> <span className="text-slate-400">Trier&nbsp;par&nbsp;·</span> <span className="font-semibold text-slate-800">{sortLabel}</span>
            <span className={`text-slate-400 transition ${sortOpen ? 'rotate-180' : ''}`}>▾</span>
          </button>
          {sortOpen && (
            <>
              <div className="fixed inset-0 z-10" onClick={() => setSortOpen(false)} />
              <div className="absolute right-0 z-20 mt-2 w-48 overflow-hidden rounded-xl border border-slate-100 bg-white shadow-xl">
                {SORTS.map(([k, label]) => (
                  <button key={k} onClick={() => { setSort(k); setSortOpen(false); }}
                    className={`flex w-full items-center justify-between px-4 py-2.5 text-left text-sm hover:bg-slate-50 ${
                      sort === k ? 'font-semibold text-slate-900' : 'text-slate-600'}`}>
                    {label}{sort === k && <span className="text-slate-900">✓</span>}
                  </button>
                ))}
              </div>
            </>
          )}
        </div>
      </div>

      {/* ---------- Grille produits ---------- */}
      <div className="px-4 pb-6 pt-4">
        {loading ? (
          <Spinner label="Chargement du catalogue…" />
        ) : error ? (
          <ErrorNote message={error} onRetry={load} />
        ) : filtered.length === 0 ? (
          <EmptyState icon="🛍️" title="Aucun article"
            sub={q ? 'Aucun résultat pour cette recherche.' : 'Cette boutique n’a pas encore de produit disponible.'} />
        ) : (
          <ul className="grid grid-cols-2 gap-3.5">
            {filtered.map((p) => (
              <li key={p.productId}>
                <Link to={`/s/${slug}/p/${p.productId}`} state={{ product: p, shopName }}
                  className="group block overflow-hidden rounded-2xl border border-slate-100 bg-white shadow-sm transition hover:shadow-md active:scale-[.99]">
                  <div className="aspect-[3/4] w-full overflow-hidden bg-slate-100">
                    {p.images && p.images.length > 0 ? (
                      <img src={p.images[0].url} alt={p.name} loading="lazy" decoding="async"
                        className="h-full w-full object-cover transition duration-300 group-hover:scale-105" />
                    ) : (
                      <div className="flex h-full w-full items-center justify-center bg-gradient-to-br from-slate-50 to-slate-100 text-4xl">🧥</div>
                    )}
                  </div>
                  <div className="p-3">
                    <div className="line-clamp-1 text-sm font-medium text-slate-900">{p.name}</div>
                    <div className="mt-0.5 text-xs text-slate-400">{p.variants.length} taille(s)/couleur(s)</div>
                    <div className="mt-1.5 text-[15px] font-extrabold text-slate-900">{money(p.price)}</div>
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

/* --------------------------- petites icônes (inline SVG) --------------------------- */
function UserIcon() {
  return (
    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="shrink-0 text-slate-400">
      <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" /><circle cx="12" cy="7" r="4" />
    </svg>
  );
}
function BagIcon() {
  return (
    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="shrink-0 text-slate-400">
      <path d="M6 2 3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4z" /><path d="M3 6h18" /><path d="M16 10a4 4 0 0 1-8 0" />
    </svg>
  );
}
function SearchIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" className="shrink-0 text-slate-400">
      <circle cx="11" cy="11" r="7" /><path d="m20 20-3-3" strokeLinecap="round" />
    </svg>
  );
}
function ShareIcon() {
  return (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <circle cx="18" cy="5" r="3" /><circle cx="6" cy="12" r="3" /><circle cx="18" cy="19" r="3" />
      <path d="m8.6 13.5 6.8 4M15.4 6.5l-6.8 4" />
    </svg>
  );
}
function SortIcon() {
  return (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" className="text-slate-500">
      <path d="M3 6h13M3 12h9M3 18h5M17 8l3-3 3 3M20 5v14" />
    </svg>
  );
}
