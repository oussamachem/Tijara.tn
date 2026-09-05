import { useEffect, useMemo, useState } from 'react';
import { useParams, useLocation, useNavigate, Link } from 'react-router-dom';
import { shopsApi, favoritesApi } from '../api/endpoints';
import { apiError } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { useCart } from '../cart/CartContext';
import Header from '../components/Header.jsx';
import WhatsAppButton from '../components/WhatsAppButton.jsx';
import ShareBar from '../components/ShareBar.jsx';
import { DEFAULT_WA_MESSAGE, productUrl } from '../lib/whatsapp';
import { Button, Spinner, ErrorNote, EmptyState } from '../components/ui.jsx';
import { money } from '../lib/format';

export default function ProductDetail() {
  const { slug, productId } = useParams();
  const { state } = useLocation();
  const navigate = useNavigate();
  const { isAuthenticated } = useAuth();
  const { add } = useCart();
  const [fav, setFav] = useState(false);

  const [product, setProduct] = useState(state?.product || null);
  const [shopName, setShopName] = useState(state?.shopName || slug);
  const [shopLogo, setShopLogo] = useState(null);
  const [contactPhone, setContactPhone] = useState(null);
  const [defaultMessage, setDefaultMessage] = useState('');
  const [others, setOthers] = useState([]);
  const [loading, setLoading] = useState(!state?.product);
  const [error, setError] = useState('');
  const [selColor, setSelColor] = useState(null);
  const [selSize, setSelSize] = useState(null);
  const [qty, setQty] = useState(1);
  const [toast, setToast] = useState('');

  const variants = product?.variants || [];
  // Couleurs distinctes (avec pastille hex) et tailles distinctes -> sélecteurs séparés (façon Shein).
  const colors = useMemo(() => {
    const m = new Map();
    for (const v of variants) if (!m.has(v.color)) m.set(v.color, v.colorHex);
    return [...m.entries()].map(([name, hex]) => ({ name, hex }));
  }, [variants]);
  const sizes = useMemo(() => [...new Set(variants.map((v) => v.size))], [variants]);
  // Variante résolue = couleur + taille sélectionnées.
  const variant = useMemo(
    () => variants.find((v) => v.color === selColor && v.size === selSize) || null,
    [variants, selColor, selSize],
  );
  const colorInStock = (c) => variants.some((v) => v.color === c && v.available > 0);
  const sizeInStock = (s) => variants.some((v) => v.size === s && (!selColor || v.color === selColor) && v.available > 0);

  // Catalogue de la boutique : résout le produit (si accès direct) + alimente « Plus de cette boutique ».
  useEffect(() => {
    let alive = true;
    setLoading(!product);
    shopsApi.catalog(slug)
      .then(({ data }) => {
        if (!alive) return;
        const current = data.find((x) => String(x.productId) === String(productId));
        if (current) setProduct(current);
        else if (!product) setError('Produit introuvable.');
        setOthers(data.filter((x) => String(x.productId) !== String(productId)).slice(0, 8));
      })
      .catch((e) => alive && !product && setError(apiError(e)))
      .finally(() => alive && setLoading(false));
    return () => { alive = false; };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [slug, productId]);

  // Fiche boutique (nom + logo) pour le bandeau cliquable.
  useEffect(() => {
    shopsApi.shop(slug).then(({ data }) => {
      setShopName(data.name); setShopLogo(data.logoUrl);
      setContactPhone(data.contactPhone); setDefaultMessage(data.whatsappDefaultMessage || '');
    }).catch(() => {});
  }, [slug]);

  // Réinitialise la sélection quand on change de produit (navigation interne).
  useEffect(() => { setSelColor(null); setSelSize(null); setQty(1); window.scrollTo(0, 0); }, [productId]);

  // État favori (cœur plein/vide) — uniquement si connecté.
  useEffect(() => {
    if (!isAuthenticated) { setFav(false); return; }
    favoritesApi.ids()
      .then(({ data }) => setFav((data || []).map(String).includes(String(productId))))
      .catch(() => {});
  }, [isAuthenticated, productId]);

  const toggleFav = async () => {
    if (!isAuthenticated) { navigate('/login'); return; }
    const next = !fav;
    setFav(next); // optimiste
    try {
      if (next) await favoritesApi.add(productId);
      else await favoritesApi.remove(productId);
    } catch { setFav(!next); } // rollback si échec
  };

  const addToCart = () => {
    if (!variant || !product) return;
    const line = {
      variantId: variant.variantId, productId: product.productId, name: product.name,
      color: variant.color, size: variant.size, price: product.price, qty, max: variant.available,
    };
    const ok = add(slug, shopName, line);
    if (!ok) {
      if (window.confirm('Votre panier contient des articles d’une autre boutique. Le remplacer ?')) {
        add(slug, shopName, line, { force: true });
        navigate('/cart');
      }
      return;
    }
    setToast('Ajouté au panier ✓');
    setTimeout(() => setToast(''), 1500);
  };

  if (loading) return (<div><Header title="Produit" back /><Spinner /></div>);
  if (!product) return (<div><Header title="Produit" back /><ErrorNote message={error || 'Introuvable'} /></div>);

  return (
    <div>
      <Header title={product.name} back />
      <ImageCarousel images={product.images} name={product.name} />

      <div className="space-y-5 p-4">
        {/* Bandeau boutique cliquable -> vitrine */}
        <Link to={`/s/${slug}`} state={{ shopName }}
          className="flex items-center gap-3 rounded-2xl border border-slate-100 bg-white p-3 shadow-card active:scale-[.99]">
          <span className="flex h-11 w-11 items-center justify-center overflow-hidden rounded-xl bg-brand-100 text-lg font-black text-brand-700">
            {shopLogo ? <img src={shopLogo} alt={shopName} className="h-full w-full object-cover" /> : (shopName || '?').charAt(0).toUpperCase()}
          </span>
          <div className="min-w-0 flex-1">
            <div className="truncate font-semibold text-slate-800">🏪 {shopName}</div>
            <div className="text-xs text-brand-600">Voir la boutique ›</div>
          </div>
        </Link>

        {/* Contacter la boutique sur WhatsApp (masqué si pas de numéro valide) : message boutique +
            infos produit + LIEN vers la fiche publique. */}
        <WhatsAppButton phone={contactPhone}
          message={`${defaultMessage || DEFAULT_WA_MESSAGE}\n`
            + `Produit : ${product.name}${(selColor || selSize) ? ` (${[selColor, selSize].filter(Boolean).join(' · ')})` : ''} — ${product.price} DT\n`
            + `Lien : ${productUrl(slug, product.productId)}`} />

        {/* Partage social (lien Open Graph -> bel aperçu sur Facebook / WhatsApp). */}
        <ShareBar slug={slug} productId={product.productId} name={product.name} />

        <div className="flex items-start justify-between gap-3">
          <div className="min-w-0">
            <h2 className="text-lg font-bold text-slate-800">{product.name}</h2>
            <p className="text-xs text-slate-400">Réf. {product.reference}</p>
            <p className="mt-1 text-2xl font-extrabold text-brand-700">{money(product.price)}</p>
          </div>
          <button onClick={toggleFav} aria-pressed={fav} aria-label={fav ? 'Retirer des favoris' : 'Ajouter aux favoris'}
            className={`flex h-11 w-11 shrink-0 items-center justify-center rounded-full border transition active:scale-90 ${
              fav ? 'border-rose-200 bg-rose-50 text-rose-500' : 'border-slate-200 bg-white text-slate-400 hover:text-rose-500'}`}>
            <svg width="22" height="22" viewBox="0 0 24 24" fill={fav ? 'currentColor' : 'none'} stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <path d="M12 21s-7.5-4.35-10-8.5C.5 9.5 2 6 5.5 6c2 0 3.3 1.2 4 2.2C10.2 7.2 11.5 6 13.5 6 17 6 18.5 9.5 22 12.5 19.5 16.65 12 21 12 21Z" />
            </svg>
          </button>
        </div>

        {variants.length === 0 ? (
          <EmptyState icon="🚫" title="Indisponible" sub="Aucune déclinaison en stock." />
        ) : (
          <>
            {/* Couleur (pastilles) */}
            <div>
              <div className="mb-2 text-sm font-semibold text-slate-600">
                Couleur{selColor && <span className="font-normal text-slate-400"> · {selColor}</span>}
              </div>
              <div className="flex flex-wrap gap-2">
                {colors.map((c) => {
                  const active = selColor === c.name;
                  return (
                    <button key={c.name} disabled={!colorInStock(c.name)}
                      onClick={() => {
                        setSelColor(c.name); setQty(1);
                        if (selSize && !variants.some((v) => v.color === c.name && v.size === selSize && v.available > 0)) setSelSize(null);
                      }}
                      className={`flex items-center gap-2 rounded-xl border px-3 py-2 text-sm font-medium disabled:opacity-40 ${
                        active ? 'border-brand-600 bg-brand-50 text-brand-700' : 'border-slate-200 bg-white text-slate-700'}`}>
                      <span className="h-4 w-4 rounded-full border border-slate-300" style={{ backgroundColor: c.hex || '#fff' }} />
                      {c.name}
                    </button>
                  );
                })}
              </div>
            </div>

            {/* Taille */}
            <div>
              <div className="mb-2 text-sm font-semibold text-slate-600">
                Taille{selSize && <span className="font-normal text-slate-400"> · {selSize}</span>}
              </div>
              <div className="flex flex-wrap gap-2">
                {sizes.map((s) => {
                  const active = selSize === s;
                  return (
                    <button key={s} disabled={!sizeInStock(s)}
                      onClick={() => { setSelSize(s); setQty(1); }}
                      className={`min-w-[52px] rounded-xl border px-3 py-2 text-center text-sm font-semibold disabled:opacity-40 disabled:line-through ${
                        active ? 'border-brand-600 bg-brand-50 text-brand-700' : 'border-slate-200 bg-white text-slate-700'}`}>
                      {s}
                    </button>
                  );
                })}
              </div>
              {variant && <p className="mt-2 text-xs text-slate-400">En stock : {variant.available}</p>}
            </div>
          </>
        )}

        {variant && (
          <div className="flex items-center justify-between">
            <span className="text-sm font-semibold text-slate-600">Quantité</span>
            <div className="flex items-center gap-4">
              <button onClick={() => setQty((n) => Math.max(1, n - 1))} className="h-9 w-9 rounded-lg border border-slate-200 bg-white text-lg font-bold">−</button>
              <span className="w-6 text-center font-bold">{qty}</span>
              <button onClick={() => setQty((n) => Math.min(variant.available, n + 1))} className="h-9 w-9 rounded-lg border border-slate-200 bg-white text-lg font-bold">+</button>
            </div>
          </div>
        )}
      </div>

      {/* Plus de cette boutique */}
      {others.length > 0 && (
        <div className="px-4 pb-4">
          <div className="mb-3 flex items-center justify-between">
            <h3 className="text-sm font-bold uppercase tracking-wide text-slate-500">Plus de cette boutique</h3>
            <Link to={`/s/${slug}`} state={{ shopName }} className="text-xs font-semibold text-brand-600">Tout voir ›</Link>
          </div>
          <ul className="grid grid-cols-2 gap-3">
            {others.map((p) => (
              <li key={p.productId}>
                <Link to={`/s/${slug}/p/${p.productId}`} state={{ product: p, shopName }}
                  className="flex h-full flex-col overflow-hidden rounded-2xl border border-slate-100 bg-white shadow-card active:scale-[.99]">
                  {p.images && p.images.length > 0 ? (
                    <img src={p.images[0].url} alt={p.name} loading="lazy" decoding="async" className="aspect-square w-full bg-slate-100 object-cover" />
                  ) : (
                    <div className="flex aspect-square items-center justify-center bg-gradient-to-br from-brand-50 to-slate-100 text-3xl">🧥</div>
                  )}
                  <div className="flex flex-1 flex-col p-2.5">
                    <div className="line-clamp-2 text-sm font-medium text-slate-800">{p.name}</div>
                    <div className="mt-auto pt-1 font-extrabold text-brand-700">{money(p.price)}</div>
                  </div>
                </Link>
              </li>
            ))}
          </ul>
        </div>
      )}

      <div className="safe-bottom sticky bottom-16 mt-2 border-t border-slate-100 bg-white p-4">
        <Button className="w-full" disabled={!variant} onClick={addToCart}>
          {variant ? `Ajouter au panier · ${money(product.price * qty)}`
            : !selColor ? 'Choisir une couleur' : 'Choisir une taille'}
        </Button>
      </div>

      {toast && (
        <div className="fixed inset-x-0 bottom-24 z-30 mx-auto max-w-md px-4">
          <div className="rounded-xl bg-slate-800 py-3 text-center text-sm font-semibold text-white shadow-lg">{toast}</div>
        </div>
      )}
    </div>
  );
}

/** Carrousel d'images swipeable (scroll-snap horizontal) + pastilles. Fallback si aucune image. */
function ImageCarousel({ images, name }) {
  const [idx, setIdx] = useState(0);
  const list = images || [];
  if (list.length === 0) {
    return <div className="flex aspect-square items-center justify-center bg-gradient-to-br from-brand-50 to-slate-100 text-7xl">🧥</div>;
  }
  const onScroll = (e) => {
    const i = Math.round(e.target.scrollLeft / e.target.clientWidth);
    if (i !== idx) setIdx(i);
  };
  return (
    <div className="relative">
      <div onScroll={onScroll} className="no-scrollbar flex aspect-square snap-x snap-mandatory overflow-x-auto">
        {list.map((img, i) => (
          <img key={i} src={img.url} alt={`${name} ${i + 1}`} loading={i === 0 ? 'eager' : 'lazy'} decoding="async"
            className="aspect-square w-full shrink-0 snap-center bg-slate-100 object-cover" />
        ))}
      </div>
      {list.length > 1 && (
        <div className="absolute inset-x-0 bottom-2 flex justify-center gap-1.5">
          {list.map((_, i) => (
            <span key={i} className={`h-1.5 rounded-full transition-all ${i === idx ? 'w-4 bg-white' : 'w-1.5 bg-white/60'}`} />
          ))}
        </div>
      )}
    </div>
  );
}
