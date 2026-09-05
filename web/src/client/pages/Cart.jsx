import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useCart } from '../cart/CartContext';
import { useAuth } from '../auth/AuthContext';
import { shopsApi } from '../api/endpoints';
import Header from '../components/Header.jsx';
import WhatsAppButton from '../components/WhatsAppButton.jsx';
import { DEFAULT_WA_MESSAGE, productUrl } from '../lib/whatsapp';
import { Button, EmptyState } from '../components/ui.jsx';
import { money } from '../lib/format';

export default function Cart() {
  const { cart, setQty, remove, subtotal, count } = useCart();
  const { isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const [contactPhone, setContactPhone] = useState(null);
  const [defaultMessage, setDefaultMessage] = useState('');

  // Numéro + message par défaut de la boutique du panier (pour le bouton « Contacter »).
  useEffect(() => {
    if (!cart.slug) { setContactPhone(null); setDefaultMessage(''); return; }
    shopsApi.shop(cart.slug).then(({ data }) => { setContactPhone(data.contactPhone); setDefaultMessage(data.whatsappDefaultMessage || ''); }).catch(() => {});
  }, [cart.slug]);

  // Récap WhatsApp : message boutique + articles (avec LIEN produit) + total indicatif. Données
  // uniquement visibles par le client (nom, déclinaison, qté, prix, total) — rien de sensible.
  const waRecap = [
    defaultMessage || DEFAULT_WA_MESSAGE,
    ...cart.items.map((it) => `- ${it.name} (${it.color} · ${it.size}) × ${it.qty} — ${money(it.price * it.qty)}\n  ${productUrl(cart.slug, it.productId)}`),
    `Total indicatif : ${money(subtotal)}`,
  ].join('\n');

  if (count === 0) {
    return (
      <div>
        <Header title="Mon panier" />
        <EmptyState
          icon="🛒"
          title="Votre panier est vide"
          sub="Parcourez une boutique et ajoutez des articles."
          action={<Link to="/" className="mt-3"><Button>Découvrir les boutiques</Button></Link>}
        />
      </div>
    );
  }

  return (
    <div>
      <Header title="Mon panier" subtitle={cart.shopName} />
      <div className="space-y-3 p-4">
        {cart.items.map((it) => (
          <div key={it.variantId} className="flex gap-3 rounded-2xl border border-slate-100 bg-white p-3 shadow-card">
            <div className="flex h-16 w-16 shrink-0 items-center justify-center rounded-xl bg-brand-50 text-2xl">🧥</div>
            <div className="min-w-0 flex-1">
              <div className="truncate text-sm font-semibold text-slate-800">{it.name}</div>
              <div className="text-xs text-slate-400">{it.color} · {it.size}</div>
              <div className="mt-0.5 text-sm font-bold text-brand-700">{money(it.price)}</div>
              <button onClick={() => remove(it.variantId)} className="mt-1 text-xs font-semibold text-rose-600">Retirer</button>
            </div>
            <div className="flex flex-col items-end justify-between">
              <div className="flex items-center gap-2">
                <button onClick={() => setQty(it.variantId, Math.max(1, it.qty - 1))} className="h-8 w-8 rounded-lg border border-slate-200 text-lg font-bold">−</button>
                <span className="w-5 text-center text-sm font-bold">{it.qty}</span>
                <button onClick={() => setQty(it.variantId, Math.min(it.max ?? 99, it.qty + 1))} className="h-8 w-8 rounded-lg border border-slate-200 text-lg font-bold">+</button>
              </div>
              <div className="text-sm font-bold text-slate-700">{money(it.price * it.qty)}</div>
            </div>
          </div>
        ))}
      </div>

      <div className="safe-bottom sticky bottom-16 border-t border-slate-100 bg-white p-4">
        <div className="mb-3 flex items-center justify-between">
          <span className="text-sm text-slate-500">Sous-total ({count} article{count > 1 ? 's' : ''})</span>
          <span className="text-lg font-extrabold text-slate-800">{money(subtotal)}</span>
        </div>
        <Button className="w-full" onClick={() => navigate(isAuthenticated ? '/checkout' : '/login', { state: { from: '/checkout' } })}>
          {isAuthenticated ? 'Passer la commande' : 'Se connecter pour commander'}
        </Button>
        {/* Contacter la boutique sur WhatsApp avec le récap (masqué si pas de numéro). */}
        <div className="mt-2"><WhatsAppButton phone={contactPhone} message={waRecap} /></div>
        <p className="mt-2 text-center text-xs text-slate-400">Le total définitif est calculé par la boutique.</p>
      </div>
    </div>
  );
}
