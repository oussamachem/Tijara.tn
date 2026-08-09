import { useEffect, useMemo, useRef, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { shopsApi, notificationsApi } from '../api/endpoints';
import { apiError } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { Spinner, ErrorNote, EmptyState } from '../components/ui.jsx';
import { money } from '../lib/format';

/**
 * Accueil marketplace (CLIENT) — design premium fashion. Flux : header clair (marque + recherche +
 * cloche notif) → Boutiques suivies → ✨ Pour vous (produits).
 * 100 % données réelles : recherche boutiques (serveur) + produits (filtre du fil), suivis, fil.
 * Aucune fonctionnalité inventée (pas de fausses catégories, pas de favori produit inexistant).
 */
export default function Home() {
  const { isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const searchRef = useRef(null);

  const [query, setQuery] = useState('');
  const [shops, setShops] = useState([]);
  const [feed, setFeed] = useState([]);
  const [follows, setFollows] = useState([]);
  const [tab, setTab] = useState('foryou');            // 'foryou' | 'follows'
  const [unread, setUnread] = useState(0);
  const [followsAll, setFollowsAll] = useState(false);  // « Voir tout » (suivies)
  const [loadingShops, setLoadingShops] = useState(true);
  const [loadingFeed, setLoadingFeed] = useState(true);
  const [error, setError] = useState('');

  // Fil produits + suivis + compteur notifications (une fois).
  useEffect(() => {
    shopsApi.feed(40).then(({ data }) => setFeed(data)).catch(() => {}).finally(() => setLoadingFeed(false));
    if (isAuthenticated) {
      shopsApi.myFollows().then(({ data }) => setFollows(data)).catch(() => {});
      notificationsApi.unreadCount().then(({ data }) => setUnread(data.count || 0)).catch(() => {});
    } else { setFollows([]); setUnread(0); }
  }, [isAuthenticated]);

  // Annuaire boutiques (recherche serveur, debounce). query='' -> toutes les boutiques actives.
  useEffect(() => {
    let alive = true;
    setLoadingShops(true);
    const t = setTimeout(() => {
      shopsApi.search(query)
        .then(({ data }) => alive && setShops(data))
        .catch((e) => alive && setError(apiError(e)))
        .finally(() => alive && setLoadingShops(false));
    }, 250);
    return () => { alive = false; clearTimeout(t); };
  }, [query]);

  const term = query.trim().toLowerCase();
  const searching = term.length > 0;
  const hasFollows = isAuthenticated && follows.length > 0;
  const followedSlugs = useMemo(() => new Set(follows.map((f) => f.slug)), [follows]);
  // Recherche produits : filtre du fil déjà chargé (aucun endpoint public de recherche produit).
  const productMatches = useMemo(
    () => (searching ? feed.filter((p) => p.name.toLowerCase().includes(term) || (p.shopName || '').toLowerCase().includes(term)) : []),
    [feed, term, searching],
  );
  const shownFeed = tab === 'follows' ? feed.filter((p) => followedSlugs.has(p.shopSlug)) : feed;

  const focusSearch = () => {
    window.scrollTo({ top: 0, behavior: 'smooth' });
    searchRef.current?.focus();
  };

  return (
    <div className="min-h-full bg-[#FAFAFA]">
      {/* ============================== HEADER (clair, compact) ============================== */}
      <header className="safe-top sticky top-0 z-20 border-b border-slate-100 bg-white/95 px-4 pb-3 pt-3 backdrop-blur">
        <div className="flex items-center justify-between gap-3">
          <div className="min-w-0">
            <h1 className="truncate text-lg font-extrabold tracking-tight text-slate-900">Smart Boutique</h1>
            <p className="truncate text-xs text-slate-500">Découvrez et commandez en ligne</p>
          </div>
          <button onClick={() => navigate('/notifications')} aria-label="Notifications"
            className="relative flex h-10 w-10 shrink-0 items-center justify-center rounded-full border border-slate-200 bg-white text-slate-700 transition hover:bg-slate-50 active:scale-95">
            <BellIcon />
            {unread > 0 && <span className="absolute right-2 top-2 h-2.5 w-2.5 rounded-full bg-rose-500 ring-2 ring-white" />}
          </button>
        </div>

        {/* Recherche (action principale) */}
        <div className="mt-3 flex items-center gap-2.5 rounded-2xl border border-slate-200 bg-white px-4 py-3 transition focus-within:border-slate-400 focus-within:ring-4 focus-within:ring-slate-100">
          <SearchIcon />
          <input ref={searchRef} value={query} onChange={(e) => setQuery(e.target.value)} placeholder="Rechercher une boutique, un produit…"
            className="min-w-0 flex-1 bg-transparent text-[15px] text-slate-800 outline-none placeholder:text-slate-400" />
          {query && <button onClick={() => setQuery('')} className="text-slate-400 hover:text-slate-600" aria-label="Effacer">✕</button>}
        </div>
      </header>

      {searching ? (
        /* ============================== RÉSULTATS DE RECHERCHE ============================== */
        <div className="space-y-6 px-4 pb-10 pt-5">
          {error && <ErrorNote message={error} />}
          <section>
            <h2 className="mb-2.5 text-sm font-bold uppercase tracking-wide text-slate-500">Boutiques</h2>
            {loadingShops ? (
              <Spinner label="Recherche…" />
            ) : shops.length === 0 ? (
              <p className="rounded-xl border border-slate-100 bg-white p-4 text-sm text-slate-400">Aucune boutique pour « {query} ».</p>
            ) : (
              <ul className="space-y-2.5">
                {shops.map((s) => (
                  <li key={s.id}>
                    <Link to={`/s/${s.slug}`} state={{ shopName: s.name }}
                      className="flex items-center gap-3 rounded-2xl border border-slate-100 bg-white p-3 shadow-sm active:scale-[.99]">
                      <Avatar name={s.name} url={s.logoUrl} />
                      <div className="min-w-0 flex-1">
                        <div className="truncate font-semibold text-slate-900">{s.name}</div>
                        <div className="truncate text-xs text-slate-400">@{s.slug}</div>
                      </div>
                      <ChevronRight />
                    </Link>
                  </li>
                ))}
              </ul>
            )}
          </section>

          <section>
            <h2 className="mb-2.5 text-sm font-bold uppercase tracking-wide text-slate-500">Produits</h2>
            {productMatches.length === 0 ? (
              <p className="rounded-xl border border-slate-100 bg-white p-4 text-sm text-slate-400">Aucun produit trouvé dans les nouveautés.</p>
            ) : (
              <ProductGrid items={productMatches} />
            )}
          </section>
        </div>
      ) : (
        /* ============================== ACCUEIL ============================== */
        <>
          {/* --- Boutiques suivies --- */}
          <section className="px-4 pt-5">
            <SectionHeader title="Boutiques suivies"
              action={hasFollows && follows.length > 4 ? <SeeAll expanded={followsAll} onClick={() => setFollowsAll((v) => !v)} /> : null} />
            {hasFollows ? (
              <BoutiqueRow shops={follows} expanded={followsAll} />
            ) : (
              <FollowEmptyState onDiscover={focusSearch} />
            )}
          </section>

          {/* --- ✨ Pour vous (produits) --- */}
          <section className="px-4 pb-10 pt-6">
            <div className="mb-3 flex items-center justify-between">
              <h2 className="text-base font-bold tracking-tight text-slate-900">✨ Pour vous</h2>
              {hasFollows && (
                <div className="flex gap-2">
                  <MiniChip active={tab === 'foryou'} onClick={() => setTab('foryou')}>Pour vous</MiniChip>
                  <MiniChip active={tab === 'follows'} onClick={() => setTab('follows')}>Suivies</MiniChip>
                </div>
              )}
            </div>
            {loadingFeed ? (
              <Spinner label="Chargement des produits…" />
            ) : shownFeed.length === 0 ? (
              <EmptyState icon={tab === 'follows' ? '⭐' : '🛍️'}
                title={tab === 'follows' ? 'Aucun produit suivi' : "Rien pour l'instant"}
                sub={tab === 'follows' ? 'Vos boutiques suivies n’ont pas de produit disponible.' : 'Les produits des boutiques apparaîtront ici.'} />
            ) : (
              <ProductGrid items={shownFeed} />
            )}
          </section>
        </>
      )}
    </div>
  );
}

/* =================================== sous-composants =================================== */

function SectionHeader({ title, action }) {
  return (
    <div className="mb-3 flex items-center justify-between">
      <h2 className="text-base font-bold tracking-tight text-slate-900">{title}</h2>
      {action}
    </div>
  );
}

function SeeAll({ expanded, onClick }) {
  return (
    <button onClick={onClick} className="flex items-center gap-0.5 text-sm font-semibold text-brand-600 active:opacity-70">
      {expanded ? 'Réduire' : 'Voir tout'}{!expanded && <span className="text-base leading-none">›</span>}
    </button>
  );
}

/** Ligne de boutiques : carrousel horizontal (replié) ou grille qui s'enroule (déplié via « Voir tout »). */
function BoutiqueRow({ shops, expanded }) {
  return expanded ? (
    <div className="flex flex-wrap gap-x-4 gap-y-4">
      {shops.map((s) => <BoutiqueCircle key={s.id} shop={s} />)}
    </div>
  ) : (
    <div className="no-scrollbar -mx-4 flex gap-4 overflow-x-auto px-4 pb-1">
      {shops.map((s) => <BoutiqueCircle key={s.id} shop={s} />)}
    </div>
  );
}

function BoutiqueCircle({ shop }) {
  return (
    <Link to={`/s/${shop.slug}`} state={{ shopName: shop.name }}
      className="flex w-[72px] shrink-0 flex-col items-center gap-1.5 text-center active:scale-95">
      <div className="h-[68px] w-[68px] rounded-full bg-white p-0.5 shadow-sm ring-1 ring-slate-200">
        <div className="flex h-full w-full items-center justify-center overflow-hidden rounded-full bg-slate-100 text-xl font-black text-slate-400">
          {shop.logoUrl
            ? <img src={shop.logoUrl} alt={shop.name} loading="lazy" className="h-full w-full object-cover" />
            : (shop.name || '?').charAt(0).toUpperCase()}
        </div>
      </div>
      <span className="w-full truncate text-[11px] font-medium text-slate-700">{shop.name}</span>
    </Link>
  );
}

/** Empty state compact (pas de grand vide) quand aucune boutique suivie / visiteur anonyme. */
function FollowEmptyState({ onDiscover }) {
  return (
    <div className="rounded-2xl border border-slate-100 bg-white p-5 text-center shadow-sm">
      <div className="mx-auto mb-2 flex h-11 w-11 items-center justify-center rounded-full bg-brand-50 text-xl">🛍️</div>
      <p className="text-sm font-semibold text-slate-900">Découvrez des boutiques</p>
      <p className="mx-auto mt-1 max-w-xs text-xs text-slate-500">Suivez vos boutiques préférées pour retrouver facilement leurs nouveautés.</p>
      <button onClick={onDiscover}
        className="mt-3 inline-flex rounded-full bg-slate-900 px-5 py-2 text-sm font-semibold text-white transition hover:bg-slate-800 active:scale-95">
        Rechercher une boutique
      </button>
    </div>
  );
}

/** Grille produits 2 colonnes — cartes premium (image portrait, nom, boutique, prix). */
function ProductGrid({ items }) {
  return (
    <ul className="grid grid-cols-2 gap-3.5">
      {items.map((p, i) => (
        <li key={`${p.shopSlug}-${p.productId}-${i}`}>
          <Link to={`/s/${p.shopSlug}/p/${p.productId}`} state={{ shopName: p.shopName }}
            className="group block overflow-hidden rounded-2xl border border-slate-100 bg-white shadow-sm transition hover:shadow-md active:scale-[.99]">
            <div className="aspect-[3/4] w-full overflow-hidden bg-slate-100">
              {p.imageUrl ? (
                <img src={p.imageUrl} alt={p.name} loading="lazy" decoding="async"
                  className="h-full w-full object-cover transition duration-300 group-hover:scale-105" />
              ) : (
                <div className="flex h-full w-full items-center justify-center bg-gradient-to-br from-slate-50 to-slate-100 text-4xl">🧥</div>
              )}
            </div>
            <div className="p-3">
              <div className="line-clamp-1 text-sm font-medium text-slate-900">{p.name}</div>
              <div className="mt-1 flex items-center gap-1.5 text-[11px] text-slate-400">
                {p.shopLogoUrl
                  ? <img src={p.shopLogoUrl} alt="" className="h-3.5 w-3.5 rounded-full object-cover" />
                  : <span>🏪</span>}
                <span className="truncate">{p.shopName}</span>
              </div>
              <div className="mt-1.5 text-[15px] font-extrabold text-slate-900">{money(p.price)}</div>
            </div>
          </Link>
        </li>
      ))}
    </ul>
  );
}

function MiniChip({ active, onClick, children }) {
  return (
    <button onClick={onClick}
      className={`whitespace-nowrap rounded-full px-3.5 py-1.5 text-xs font-semibold transition ${
        active ? 'bg-slate-900 text-white' : 'border border-slate-200 bg-white text-slate-600'}`}>
      {children}
    </button>
  );
}

function Avatar({ name, url }) {
  return (
    <div className="flex h-12 w-12 shrink-0 items-center justify-center overflow-hidden rounded-full bg-slate-100 text-lg font-black text-slate-400 ring-1 ring-slate-200">
      {url ? <img src={url} alt={name} loading="lazy" className="h-full w-full object-cover" /> : (name || '?').charAt(0).toUpperCase()}
    </div>
  );
}

/* --------------------------- petites icônes (inline SVG) --------------------------- */
function SearchIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" className="shrink-0 text-slate-400">
      <circle cx="11" cy="11" r="7" /><path d="m20 20-3-3" strokeLinecap="round" />
    </svg>
  );
}
function BellIcon() {
  return (
    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9" /><path d="M10.3 21a1.94 1.94 0 0 0 3.4 0" />
    </svg>
  );
}
function ChevronRight() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="shrink-0 text-slate-300">
      <path d="m9 18 6-6-6-6" />
    </svg>
  );
}
