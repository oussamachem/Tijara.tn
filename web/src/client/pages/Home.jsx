import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { shopsApi } from '../api/endpoints';
import { apiError } from '../api/client';
import { Input, Spinner, ErrorNote, EmptyState } from '../components/ui.jsx';

export default function Home() {
  const [query, setQuery] = useState('');
  const [shops, setShops] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    let alive = true;
    setLoading(true);
    const t = setTimeout(() => {
      shopsApi
        .search(query)
        .then(({ data }) => alive && setShops(data))
        .catch((e) => alive && setError(apiError(e)))
        .finally(() => alive && setLoading(false));
    }, 250);
    return () => { alive = false; clearTimeout(t); };
  }, [query]);

  return (
    <div>
      <header className="safe-top sticky top-0 z-10 bg-brand-600 px-4 pb-4 pt-4 text-white">
        <h1 className="text-xl font-extrabold tracking-tight">Smart Boutique</h1>
        <p className="mt-0.5 text-sm text-brand-100">Trouvez une boutique et commandez en ligne</p>
        <div className="mt-3">
          <Input
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="🔍 Rechercher une boutique…"
            className="border-transparent bg-white/95"
            autoFocus
          />
        </div>
      </header>

      <div className="p-4">
        <ErrorNote message={error} onRetry={() => setQuery((q) => q + '')} />
        {loading ? (
          <Spinner label="Chargement des boutiques…" />
        ) : shops.length === 0 ? (
          <EmptyState icon="🔎" title="Aucune boutique" sub="Essayez un autre nom." />
        ) : (
          <ul className="space-y-3">
            {shops.map((s) => (
              <li key={s.id}>
                <Link
                  to={`/s/${s.slug}`}
                  className="flex items-center gap-3 rounded-2xl border border-slate-100 bg-white p-4 shadow-card active:scale-[.99]"
                >
                  <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-xl bg-brand-100 text-xl font-black text-brand-700">
                    {s.name.charAt(0).toUpperCase()}
                  </div>
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
    </div>
  );
}
