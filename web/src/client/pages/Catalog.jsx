import { useEffect, useMemo, useState } from 'react';
import { Link, useParams, useLocation, useNavigate } from 'react-router-dom';
import { shopsApi } from '../api/endpoints';
import { apiError } from '../api/client';
import { Input, Spinner, ErrorNote, EmptyState } from '../components/ui.jsx';
import { money } from '../lib/format';

/** Vitrine d'une boutique : bandeau (nom + galerie), recherche/tri, grille de produits. */
export default function Catalog() {
  const { slug } = useParams();
  const { state } = useLocation();
  const navigate = useNavigate();
  const [shopName, setShopName] = useState(state?.shopName || slug);
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

  useEffect(() => {
    if (!state?.shopName) {
      shopsApi.search(slug).then(({ data }) => {
        const exact = data.find((s) => s.slug === slug);
        if (exact) setShopName(exact.name);
      }).catch(() => {});
    }
    localStorage.setItem('sbc_lastShop', JSON.stringify({ slug, name: state?.shopName || slug }));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [slug]);

  useEffect(() => {
    if (shopName && shopName !== slug) {
      localStorage.setItem('sbc_lastShop', JSON.stringify({ slug, name: shopName }));
    }
  }, [shopName, slug]);

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
      {/* Bandeau vitrine */}
      <header className="safe-top relative bg-gradient-to-br from-brand-600 to-brand-800 px-4 pb-5 pt-4 text-white">
        <button onClick={() => navigate(-1)} aria-label="Retour"
          className="mb-2 -ml-1 flex h-8 w-8 items-center justify-center rounded-full text-white/90 hover:bg-white/10">
          <span className="text-xl leading-none">‹</span>
        </button>
        <div className="flex items-center gap-3">
          <div className="flex h-16 w-16 shrink-0 items-center justify-center rounded-2xl bg-white/20 text-2xl font-black backdrop-blur">
            {(shopName || '?').charAt(0).toUpperCase()}
          </div>
          <div className="min-w-0 flex-1">
            <h1 className="truncate text-xl font-extrabold">{shopName}</h1>
            <p className="truncate text-sm text-brand-100">/{slug} · {products.length} produit(s)</p>
          </div>
          <Link to={`/s/${slug}/gallery`} state={{ shopName }}
            className="shrink-0 rounded-full bg-white/95 px-3 py-1.5 text-xs font-semibold text-brand-700">
            📸 Galerie
          </Link>
        </div>
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
