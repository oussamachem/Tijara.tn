import { useEffect, useState } from 'react';
import { useParams, useLocation, useNavigate } from 'react-router-dom';
import { shopsApi } from '../api/endpoints';
import { apiError } from '../api/client';
import { useCart } from '../cart/CartContext';
import Header from '../components/Header.jsx';
import { Button, Spinner, ErrorNote, EmptyState } from '../components/ui.jsx';
import { money } from '../lib/format';

export default function ProductDetail() {
  const { slug, productId } = useParams();
  const { state } = useLocation();
  const navigate = useNavigate();
  const { add } = useCart();

  const [product, setProduct] = useState(state?.product || null);
  const shopName = state?.shopName || slug;
  const [loading, setLoading] = useState(!state?.product);
  const [error, setError] = useState('');
  const [variant, setVariant] = useState(null);
  const [qty, setQty] = useState(1);
  const [toast, setToast] = useState('');

  useEffect(() => {
    if (product) return;
    setLoading(true);
    shopsApi
      .catalog(slug)
      .then(({ data }) => {
        const p = data.find((x) => String(x.productId) === String(productId));
        if (p) setProduct(p);
        else setError('Produit introuvable.');
      })
      .catch((e) => setError(apiError(e)))
      .finally(() => setLoading(false));
  }, [slug, productId]);

  const addToCart = () => {
    if (!variant) return;
    const ok = add(slug, shopName, {
      variantId: variant.variantId,
      productId: product.productId,
      name: product.name,
      color: variant.color,
      size: variant.size,
      price: product.price,
      qty,
      max: variant.available,
    });
    if (!ok) {
      if (window.confirm('Votre panier contient des articles d’une autre boutique. Le remplacer ?')) {
        add(slug, shopName, {
          variantId: variant.variantId, productId: product.productId, name: product.name,
          color: variant.color, size: variant.size, price: product.price, qty, max: variant.available,
        }, { force: true });
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
      <Header title={product.name} subtitle={shopName} back />
      <div className="flex aspect-square items-center justify-center bg-gradient-to-br from-brand-50 to-slate-100 text-7xl">🧥</div>

      <div className="space-y-5 p-4">
        <div>
          <h2 className="text-lg font-bold text-slate-800">{product.name}</h2>
          <p className="text-xs text-slate-400">Réf. {product.reference}</p>
          <p className="mt-1 text-2xl font-extrabold text-brand-700">{money(product.price)}</p>
        </div>

        <div>
          <div className="mb-2 text-sm font-semibold text-slate-600">Choisir une déclinaison</div>
          {product.variants.length === 0 ? (
            <EmptyState icon="🚫" title="Indisponible" sub="Aucune déclinaison en stock." />
          ) : (
            <div className="flex flex-wrap gap-2">
              {product.variants.map((v) => (
                <button
                  key={v.variantId}
                  onClick={() => { setVariant(v); setQty(1); }}
                  className={`rounded-xl border px-3 py-2 text-sm font-medium ${
                    variant?.variantId === v.variantId
                      ? 'border-brand-600 bg-brand-50 text-brand-700'
                      : 'border-slate-200 bg-white text-slate-700'
                  }`}
                >
                  {v.color} · {v.size}
                  <span className="ml-1 text-xs text-slate-400">({v.available})</span>
                </button>
              ))}
            </div>
          )}
        </div>

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

      <div className="safe-bottom sticky bottom-16 mt-2 border-t border-slate-100 bg-white p-4">
        <Button className="w-full" disabled={!variant} onClick={addToCart}>
          {variant ? `Ajouter au panier · ${money(product.price * qty)}` : 'Choisir une déclinaison'}
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
