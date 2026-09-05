import { useEffect, useState } from 'react';
import { Routes, Route, NavLink, Navigate, useLocation } from 'react-router-dom';
import { CartProvider, useCart } from '../client/cart/CartContext.jsx';
import { useAuth } from '../context/AuthContext.jsx';
import { notificationsApi } from '../api/endpoints.js';
import Home from '../client/pages/Home.jsx';
import Catalog from '../client/pages/Catalog.jsx';
import Gallery from '../client/pages/Gallery.jsx';
import ProductDetail from '../client/pages/ProductDetail.jsx';
import Cart from '../client/pages/Cart.jsx';
import Checkout from '../client/pages/Checkout.jsx';
import Orders from '../client/pages/Orders.jsx';
import Account from '../client/pages/Account.jsx';
import Notifications from '../client/pages/Notifications.jsx';
import Favorites from '../client/pages/Favorites.jsx';

function BottomNav({ unread }) {
  const { count } = useCart();
  const items = [
    { to: '/', Icon: StoreIcon, label: 'Boutiques', end: true },
    { to: '/cart', Icon: CartIcon, label: 'Panier', badge: count },
    { to: '/orders', Icon: BoxIcon, label: 'Commandes' },
    { to: '/account', Icon: UserIcon, label: 'Compte', dot: unread > 0 },
  ];
  return (
    <nav className="safe-bottom fixed inset-x-0 bottom-0 z-20 mx-auto flex max-w-md items-stretch border-t border-slate-200 bg-white/95 backdrop-blur">
      {items.map(({ to, Icon, label, end, badge, dot }) => (
        <NavLink
          key={to}
          to={to}
          end={end}
          className={({ isActive }) =>
            `relative flex flex-1 flex-col items-center gap-1 py-2.5 text-[11px] ${
              isActive ? 'font-semibold text-slate-900' : 'font-medium text-slate-400'
            }`
          }
        >
          {({ isActive }) => (
            <>
              <Icon active={isActive} />
              {label}
              {badge > 0 && (
                <span className="absolute right-1/2 top-1 translate-x-3.5 rounded-full bg-slate-900 px-1.5 py-0.5 text-[10px] font-bold leading-none text-white">
                  {badge}
                </span>
              )}
              {dot && (
                <span className="absolute right-1/2 top-1.5 translate-x-3.5 h-2 w-2 rounded-full bg-rose-500" />
              )}
            </>
          )}
        </NavLink>
      ))}
    </nav>
  );
}

/* Icônes bottom nav (line style ; remplies quand l'onglet est actif). */
function StoreIcon({ active }) {
  return (
    <svg width="22" height="22" viewBox="0 0 24 24" fill={active ? 'currentColor' : 'none'} stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
      <path d="M3 9.5 4.5 4h15L21 9.5" fill="none" /><path d="M4 9.5V20h16V9.5" fill={active ? 'currentColor' : 'none'} opacity={active ? 0.14 : 1} />
      <path d="M4 9.5h16" fill="none" /><path d="M9 20v-5h6v5" fill="none" stroke={active ? '#fff' : 'currentColor'} />
    </svg>
  );
}
function CartIcon({ active }) {
  return (
    <svg width="22" height="22" viewBox="0 0 24 24" fill={active ? 'currentColor' : 'none'} stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
      <circle cx="9" cy="20" r="1.4" /><circle cx="18" cy="20" r="1.4" />
      <path d="M2.5 3.5h2l2.2 11a1.6 1.6 0 0 0 1.6 1.3h8.2a1.6 1.6 0 0 0 1.6-1.3L21.5 7H6" fill="none" />
    </svg>
  );
}
function BoxIcon({ active }) {
  return (
    <svg width="22" height="22" viewBox="0 0 24 24" fill={active ? 'currentColor' : 'none'} stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
      <path d="M21 8 12 3 3 8v8l9 5 9-5V8Z" opacity={active ? 0.14 : 1} /><path d="m3 8 9 5 9-5" fill="none" /><path d="M12 13v8" fill="none" />
    </svg>
  );
}
function UserIcon({ active }) {
  return (
    <svg width="22" height="22" viewBox="0 0 24 24" fill={active ? 'currentColor' : 'none'} stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
      <circle cx="12" cy="8" r="4" opacity={active ? 0.16 : 1} /><path d="M4 21a8 8 0 0 1 16 0" fill="none" />
    </svg>
  );
}

function RequireAuth({ children }) {
  const { isAuthenticated } = useAuth();
  const loc = useLocation();
  if (!isAuthenticated) return <Navigate to="/login" state={{ from: loc.pathname }} replace />;
  return children;
}

/**
 * Espace CLIENT (marketplace) : parcours d'achat public + suivi de commandes. Layout mobile-first
 * centré. Un utilisateur qui est AUSSI membre d'une boutique peut basculer via le compte/sélecteur
 * sans re-login (bouton « Espaces »).
 */
export default function ClientSpace() {
  const { isAuthenticated } = useAuth();
  const [unread, setUnread] = useState(0);

  useEffect(() => {
    if (!isAuthenticated) { setUnread(0); return; }
    notificationsApi.unreadCount()
      .then(({ data }) => setUnread(data.count || 0))
      .catch(() => {});
  }, [isAuthenticated]);

  return (
    <CartProvider>
      <div className="mx-auto flex min-h-full max-w-md flex-col bg-slate-50 shadow-sm">
        {/* Clearance = hauteur barre du bas + safe-area (sinon la nav recouvre le dernier produit). */}
        <main className="flex-1 pb-[calc(5rem+env(safe-area-inset-bottom,0px))]">
          <Routes>
            <Route index element={<Home />} />
            <Route path="s/:slug" element={<Catalog />} />
            <Route path="s/:slug/gallery" element={<Gallery />} />
            <Route path="s/:slug/p/:productId" element={<ProductDetail />} />
            <Route path="cart" element={<Cart />} />
            <Route path="checkout" element={<RequireAuth><Checkout /></RequireAuth>} />
            <Route path="orders" element={<Orders />} />
            <Route path="account" element={<Account />} />
            <Route path="notifications" element={<Notifications onSeen={() => setUnread(0)} />} />
            <Route path="favorites" element={<Favorites />} />
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </main>
        <BottomNav unread={unread} />
      </div>
    </CartProvider>
  );
}
