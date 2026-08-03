import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { shopsApi } from '../api/endpoints';
import { apiError } from '../api/client';
import { Input, Spinner, ErrorNote, EmptyState } from '../components/ui.jsx';
import { money } from '../lib/format';

export default function Home() {
  const [query, setQuery] = useState('');
  const [shops, setShops] = useState([]);
  const [feed, setFeed] = useState([]);
  const [loadingShops, setLoadingShops] = useState(true);
  const [loadingFeed, setLoadingFeed] = useState(true);
  const [error, setError] = useState('');

  // Fil produits + rangée boutiques (une fois).
  useEffect(() => {
    shopsApi.feed(40).then(({ data }) => setFeed(data)).catch(() => {}).finally(() => setLoadingFeed(false));
  }, []);

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

  return (
    <div>
      <header className="safe-top sticky top-0 z-10 bg-brand-600 px-4 pb-4 pt-4 text-white">
        <h1 className="text-xl font-extrabold tracking-tight">Smart Boutique</h1>
        <p className="mt-0.5 text-sm text-brand-100">Découvrez et commandez en ligne</p>
        <div className="mt-3">
          <Input
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="🔍 Rechercher une boutique…"
            className="border-transparent bg-white/95"
          />
        </div>
      </header>

      {/* --- MODE RECHERCHE : liste de boutiques --- */}
      {searching ? (
        <div className="p-4">
          <ErrorNote message={error} />
          {loadingShops ? (
            <Spinner label="Recherche…" />
          ) : shops.length === 0 ? (
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
          {/* --- Rangée boutiques (défilement horizontal) --- */}
          {shops.length > 0 && (
            <div className="px-4 pt-4">
              <div className="mb-2 flex items-center justify-between">
                <h2 className="text-sm font-bold uppercase tracking-wide text-slate-500">Boutiques</h2>
              </div>
              <div className="no-scrollbar flex gap-3 overflow-x-auto pb-1">
                {shops.map((s) => (
                  <Link key={s.id} to={`/s/${s.slug}`} state={{ shopName: s.name }}
                    className="flex w-16 shrink-0 flex-col items-center gap-1 text-center">
                    <ShopAvatar name={s.name} url={s.logoUrl} big />
                    <span className="w-full truncate text-[11px] font-medium text-slate-600">{s.name}</span>
                  </Link>
                ))}
              </div>
            </div>
          )}

          {/* --- Fil produits (découverte) --- */}
          <div className="p-4">
            <h2 className="mb-3 text-sm font-bold uppercase tracking-wide text-slate-500">Découvrir</h2>
            {loadingFeed ? (
              <Spinner label="Chargement des produits…" />
            ) : feed.length === 0 ? (
              <EmptyState icon="🛍️" title="Rien pour l'instant" sub="Les produits des boutiques apparaîtront ici." />
            ) : (
              <ul className="grid grid-cols-2 gap-3">
                {feed.map((p, i) => (
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
                          <span className="truncate">🏪 {p.shopName}</span>
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

function ShopAvatar({ name, url, big = false }) {
  const size = big ? 'h-14 w-14' : 'h-12 w-12';
  return (
    <div className={`flex shrink-0 items-center justify-center overflow-hidden rounded-2xl bg-brand-100 text-xl font-black text-brand-700 ${size}`}>
      {url ? <img src={url} alt={name} loading="lazy" className="h-full w-full object-cover" /> : (name || '?').charAt(0).toUpperCase()}
    </div>
  );
}
