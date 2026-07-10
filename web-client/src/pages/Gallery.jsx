import { useCallback, useEffect, useRef, useState } from 'react';
import { Link, useParams, useLocation } from 'react-router-dom';
import { shopsApi } from '../api/endpoints';
import { apiError } from '../api/client';
import Header from '../components/Header.jsx';
import { Spinner, ErrorNote, EmptyState } from '../components/ui.jsx';
import { money } from '../lib/format';

export default function Gallery() {
  const { slug } = useParams();
  const { state } = useLocation();
  const [shopName, setShopName] = useState(state?.shopName || slug);
  const [items, setItems] = useState([]);
  const [page, setPage] = useState(0);
  const [last, setLast] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [initial, setInitial] = useState(true);
  const sentinel = useRef(null);

  const loadNext = useCallback(() => {
    if (loading || last) return;
    setLoading(true);
    setError('');
    shopsApi
      .gallery(slug, page, 24)
      .then(({ data }) => {
        setItems((prev) => [...prev, ...data.content]);
        setLast(data.last);
        setPage((p) => p + 1);
      })
      .catch((e) => setError(apiError(e)))
      .finally(() => { setLoading(false); setInitial(false); });
  }, [slug, page, loading, last]);

  // Premier chargement.
  useEffect(() => { loadNext(); /* eslint-disable-next-line */ }, []);

  // Resout le nom de boutique si acces direct.
  useEffect(() => {
    if (!state?.shopName) {
      shopsApi.search(slug).then(({ data }) => {
        const exact = data.find((s) => s.slug === slug);
        if (exact) setShopName(exact.name);
      }).catch(() => {});
    }
  }, [slug]);

  // Scroll infini : charge la page suivante quand le sentinel devient visible.
  useEffect(() => {
    if (!sentinel.current) return;
    const obs = new IntersectionObserver((entries) => {
      if (entries[0].isIntersecting) loadNext();
    }, { rootMargin: '400px' });
    obs.observe(sentinel.current);
    return () => obs.disconnect();
  }, [loadNext]);

  return (
    <div>
      <Header title={shopName} subtitle="Galerie" back
        right={<Link to={`/s/${slug}`} state={{ shopName }} className="rounded-full bg-slate-100 px-3 py-1.5 text-xs font-semibold text-slate-600">Liste</Link>} />

      <div className="p-3">
        <ErrorNote message={error} onRetry={loadNext} />

        {initial && loading ? (
          <Spinner label="Chargement de la galerie…" />
        ) : items.length === 0 ? (
          <EmptyState icon="📷" title="Aucune photo" sub="Cette boutique n’a pas encore de produit en photo." />
        ) : (
          <div className="columns-2 gap-3 sm:columns-3 lg:columns-4 [column-fill:_balance]">
            {items.map((p, idx) => {
              const cover = p.images && p.images.length > 0 ? p.images[0].url : null;
              const available = (p.variants || []).length > 0;
              return (
                <Link
                  key={`${p.productId}-${idx}`}
                  to={`/s/${slug}/p/${p.productId}`}
                  state={{ product: p, shopName }}
                  className="mb-3 block break-inside-avoid overflow-hidden rounded-2xl border border-slate-100 bg-white shadow-card active:scale-[.99]"
                >
                  {cover ? (
                    <img src={cover} alt={p.name} loading="lazy" decoding="async"
                      className="w-full bg-slate-100 object-cover" />
                  ) : (
                    <div className="flex aspect-square items-center justify-center bg-gradient-to-br from-brand-50 to-slate-100 text-4xl">🧥</div>
                  )}
                  <div className="p-2.5">
                    <div className="line-clamp-2 text-sm font-semibold text-slate-800">{p.name}</div>
                    <div className="mt-1 flex items-center justify-between">
                      <span className="font-extrabold text-brand-700">{money(p.price)}</span>
                      {!available && <span className="rounded-full bg-slate-100 px-2 py-0.5 text-[10px] font-semibold text-slate-400">Rupture</span>}
                    </div>
                  </div>
                </Link>
              );
            })}
          </div>
        )}

        {/* Sentinel scroll infini + spinner de page suivante */}
        {!last && <div ref={sentinel} className="h-8" />}
        {loading && !initial && <Spinner />}
        {last && items.length > 0 && <p className="py-6 text-center text-xs text-slate-400">— fin du catalogue —</p>}
      </div>
    </div>
  );
}
