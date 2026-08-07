import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { shopsApi } from '../api/endpoints';
import { apiError } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { Input, Spinner, ErrorNote, EmptyState } from '../components/ui.jsx';
import { money } from '../lib/format';

export default function Home() {
  const { isAuthenticated } = useAuth();
  const [query, setQuery] = useState('');
  const [shops, setShops] = useState([]);
  const [feed, setFeed] = useState([]);
  const [follows, setFollows] = useState([]);
  const [tab, setTab] = useState('foryou'); // 'foryou' | 'follows'
  const [loadingShops, setLoadingShops] = useState(true);
  const [loadingFeed, setLoadingFeed] = useState(true);
  const [error, setError] = useState('');

  // Fil produits + boutiques suivies (une fois).
  useEffect(() => {
    shopsApi.feed(40).then(({ data }) => setFeed(data)).catch(() => {}).finally(() => setLoadingFeed(false));
    if (isAuthenticated) shopsApi.myFollows().then(({ data }) => setFollows(data)).catch(() => {});
  }, [isAuthenticated]);

  // Recherche boutiques (debounce).
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

  const searching = query.trim().length > 0;
  const hasFollows = isAuthenticated && follows.length > 0;
  const followedSlugs = useMemo(() => new Set(follows.map((f) => f.slug)), [follows]);
  const rowShops = hasFollows ? follows : shops;          // en tête : mes boutiques suivies, sinon toutes
  const shownFeed = tab === 'follows' ? feed.filter((p) => followedSlugs.has(p.shopSlug)) : feed;

  return (
    <div>
      <header className="safe-top sticky top-0 z-10 bg-brand-600 px-4 pb-4 pt-4 text-white">
        <h1 className="text-xl font-extrabold tracking-tight">Smart Boutique</h1>
        <p className="mt-0.5 text-sm text-brand-100">Découvrez et commandez en ligne</p>
        <div className="mt-3">
          <Input value={query} onChange={(e) => setQuery(e.target.value)}
            placeholder="🔍 Rechercher une boutique…" className="border-transparent bg-white/95" />
        </div>
      </header>

      {searching ? (
        <div className="p-4">
          <ErrorNote message={error} />
          {loadingShops ? <Spinner label="Recherche…" /> : shops.length === 0 ? (
            <EmptyState icon="🔎" title="Aucune boutique" sub="Essayez un autre nom." />
          ) : (
            <ul className="space-y-3">
              {shops.map((s) => (
                <li key={s.id}>
                  <Link to={`/s/${s.slug}`} state={{ shopName: s.name }}
                    className="flex items-center gap-3 rounded-2xl border border-slate-100 bg-white p-4 shadow-card active:scale-[.99]">
                    <ShopAvatar name={s.name} url={s.logoUrl} />
                    <div className="min-w-0 flex-1">
                      <div className="truncate font-semibold text-slate-800">{s.name}</div>
                      <div className="truncate text-xs text-slate-400">/{s.slug}</div>
                    </div>
                    <span className="text-slate-300">›</span>
                  </Link>
                </li>
              ))}
            </ul>
          )}
        </div>
      ) : (
        <>
          {/* --- Grille boutiques (2 rangées de tuiles, façon Shein) : MES BOUTIQUES suivies sinon toutes --- */}
          {rowShops.length > 0 && (
            <div className="px-4 pt-4">
              <h2 className="mb-2.5 text-sm font-bold uppercase tracking-wide text-slate-500">
                {hasFollows ? '⭐ Mes boutiques' : '🏬 Boutiques'}
              </h2>
              <div className="no-scrollbar -mx-1 overflow-x-auto px-1 pb-1">
                <div className="grid grid-flow-col grid-rows-2 gap-x-4 gap-y-3" style={{ gridAutoColumns: '72px' }}>
                  {rowShops.map((s) => (
                    <Link key={s.id} to={`/s/${s.slug}`} state={{ shopName: s.name }}
                      className="flex w-[72px] flex-col items-center gap-1 text-center active:scale-95">
                      <div className="flex h-[72px] w-[72px] items-center justify-center overflow-hidden rounded-3xl bg-brand-50 text-2xl font-black text-brand-700 ring-1 ring-slate-200">
                        {s.logoUrl
                          ? <img src={s.logoUrl} alt={s.name} loading="lazy" className="h-full w-full object-cover" />
                          : (s.name || '?').charAt(0).toUpperCase()}
                      </div>
                      <span className="w-full truncate text-[11px] font-medium text-slate-700">{s.name}</span>
                    </Link>
                  ))}
                </div>
              </div>
              {!hasFollows && isAuthenticated && (
                <p className="mt-1.5 text-xs text-slate-400">💡 Appuyez sur « Suivre » dans une boutique : elle apparaîtra ici.</p>
              )}
            </div>
          )}

          {/* --- Chips Pour toi / Suivies (comme Shein For You / New In) --- */}
          <div className="sticky top-[92px] z-10 mt-3 flex gap-2 border-b border-slate-100 bg-slate-50/95 px-4 py-2 backdrop-blur">
            <Chip active={tab === 'foryou'} onClick={() => setTab('foryou')}>✨ Pour toi</Chip>
            {hasFollows && <Chip active={tab === 'follows'} onClick={() => setTab('follows')}>⭐ Suivies</Chip>}
          </div>

          {/* --- Grille produits --- */}
          <div className="p-4">
            {loadingFeed ? (
              <Spinner label="Chargement des produits…" />
            ) : shownFeed.length === 0 ? (
              <EmptyState icon={tab === 'follows' ? '⭐' : '🛍️'}
                title={tab === 'follows' ? 'Aucun produit suivi' : "Rien pour l'instant"}
                sub={tab === 'follows' ? 'Vos boutiques suivies n’ont pas de produit disponible.' : 'Les produits des boutiques apparaîtront ici.'} />
            ) : (
              <ul className="grid grid-cols-2 gap-3">
                {shownFeed.map((p, i) => (
                  <li key={`${p.shopSlug}-${p.productId}-${i}`}>
                    <Link to={`/s/${p.shopSlug}/p/${p.productId}`} state={{ shopName: p.shopName }}
                      className="flex h-full flex-col overflow-hidden rounded-2xl border border-slate-100 bg-white shadow-card active:scale-[.99]">
                      {p.imageUrl ? (
                        <img src={p.imageUrl} alt={p.name} loading="lazy" decoding="async"
                          className="aspect-square w-full bg-slate-100 object-cover" />
                      ) : (
                        <div className="flex aspect-square items-center justify-center bg-gradient-to-br from-brand-50 to-slate-100 text-4xl">🧥</div>
                      )}
                      <div className="flex flex-1 flex-col p-2.5">
                        <div className="line-clamp-2 text-sm font-semibold text-slate-800">{p.name}</div>
                        <div className="mt-0.5 flex items-center gap-1 text-[11px] text-slate-400">
                          {p.shopLogoUrl
                            ? <img src={p.shopLogoUrl} alt="" className="h-3.5 w-3.5 rounded-full object-cover" />
                            : <span>🏪</span>}
                          <span className="truncate">{p.shopName}</span>
                        </div>
                        <div className="mt-auto pt-1.5 font-extrabold text-brand-700">{money(p.price)}</div>
                      </div>
                    </Link>
                  </li>
                ))}
              </ul>
            )}
          </div>
        </>
      )}
    </div>
  );
}

function Chip({ active, onClick, children }) {
  return (
    <button onClick={onClick}
      className={`whitespace-nowrap rounded-full px-4 py-1.5 text-sm font-semibold ${
        active ? 'bg-slate-900 text-white' : 'border border-slate-200 bg-white text-slate-600'}`}>
      {children}
    </button>
  );
}

function ShopAvatar({ name, url, big = false }) {
  const size = big ? 'h-14 w-14' : 'h-12 w-12';
  return (
    <div className={`flex shrink-0 items-center justify-center overflow-hidden rounded-2xl bg-brand-100 text-xl font-black text-brand-700 ${size}`}>
      {url ? <img src={url} alt={name} loading="lazy" className="h-full w-full object-cover" /> : (name || '?').charAt(0).toUpperCase()}
    </div>
  );
}
