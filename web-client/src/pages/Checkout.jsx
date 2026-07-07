import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { shopsApi } from '../api/endpoints';
import { apiError } from '../api/client';
import { useCart } from '../cart/CartContext';
import { useAuth } from '../auth/AuthContext';
import Header from '../components/Header.jsx';
import { Button, ErrorNote, EmptyState } from '../components/ui.jsx';
import { money } from '../lib/format';

export default function Checkout() {
  const { cart, subtotal, count, clear } = useCart();
  const { user } = useAuth();
  const navigate = useNavigate();
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  if (count === 0) {
    return (<div><Header title="Commande" back /><EmptyState icon="🛒" title="Panier vide" /></div>);
  }

  const placeOrder = async () => {
    setSubmitting(true);
    setError('');
    try {
      const items = cart.items.map((i) => ({ variantId: i.variantId, quantity: i.qty }));
      const { data } = await shopsApi.order(cart.slug, items);
      localStorage.setItem('sbc_lastShop', JSON.stringify({ slug: cart.slug, name: cart.shopName }));
      clear();
      navigate('/orders', { replace: true, state: { justOrdered: data.reference } });
    } catch (e) {
      setError(apiError(e));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div>
      <Header title="Valider la commande" subtitle={cart.shopName} back />
      <div className="space-y-4 p-4">
        <div className="rounded-2xl border border-slate-100 bg-white p-4 shadow-card">
          <div className="mb-2 text-sm font-semibold text-slate-600">Client</div>
          <div className="text-sm text-slate-800">{user?.fullName}</div>
          <div className="text-xs text-slate-400">{user?.email}</div>
        </div>

        <div className="rounded-2xl border border-slate-100 bg-white shadow-card">
          <div className="border-b border-slate-100 px-4 py-3 text-sm font-semibold text-slate-600">
            Récapitulatif · {cart.shopName}
          </div>
          <ul className="divide-y divide-slate-100">
            {cart.items.map((it) => (
              <li key={it.variantId} className="flex items-center justify-between px-4 py-3">
                <div className="min-w-0">
                  <div className="truncate text-sm font-medium text-slate-800">{it.name}</div>
                  <div className="text-xs text-slate-400">{it.color} · {it.size} × {it.qty}</div>
                </div>
                <div className="text-sm font-bold text-slate-700">{money(it.price * it.qty)}</div>
              </li>
            ))}
          </ul>
          <div className="flex items-center justify-between px-4 py-3">
            <span className="text-sm text-slate-500">Sous-total indicatif</span>
            <span className="text-lg font-extrabold text-slate-800">{money(subtotal)}</span>
          </div>
        </div>

        <div className="rounded-xl bg-brand-50 p-3 text-sm text-brand-800">
          💡 Paiement à la récupération en boutique. Vous serez notifié quand la commande sera prête.
        </div>

        <ErrorNote message={error} />
      </div>

      <div className="safe-bottom sticky bottom-16 border-t border-slate-100 bg-white p-4">
        <Button className="w-full" disabled={submitting} onClick={placeOrder}>
          {submitting ? 'Envoi…' : 'Confirmer la commande'}
        </Button>
      </div>
    </div>
  );
}
