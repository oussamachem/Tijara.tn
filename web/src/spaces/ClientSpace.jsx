import { Routes, Route, NavLink, Navigate, useLocation } from 'react-router-dom';
import { CartProvider, useCart } from '../client/cart/CartContext.jsx';
import { useAuth } from '../context/AuthContext.jsx';
import { useShop } from '../context/ShopContext.jsx';
import Home from '../client/pages/Home.jsx';
import Catalog from '../client/pages/Catalog.jsx';
import Gallery from '../client/pages/Gallery.jsx';
import ProductDetail from '../client/pages/ProductDetail.jsx';
import Cart from '../client/pages/Cart.jsx';
import Checkout from '../client/pages/Checkout.jsx';
import Orders from '../client/pages/Orders.jsx';
import Account from '../client/pages/Account.jsx';

function BottomNav() {
  const { count } = useCart();
  const { memberships, isPlatformAdmin } = useShop();
  const items = [
    { to: '/', icon: '🏬', label: 'Boutiques', end: true },
    { to: '/cart', icon: '🛒', label: 'Panier', badge: count },
    { to: '/orders', icon: '📦', label: 'Commandes' },
    { to: '/account', icon: '👤', label: 'Compte' },
  ];
  return (
    <nav className="safe-bottom fixed inset-x-0 bottom-0 z-20 mx-auto flex max-w-md items-stretch border-t border-slate-200 bg-white/95 backdrop-blur">
      {items.map((it) => (
        <NavLink
          key={it.to}
          to={it.to}
          end={it.end}
          className={({ isActive }) =>
            `relative flex flex-1 flex-col items-center gap-0.5 py-2.5 text-[11px] font-medium ${
              isActive ? 'text-brand-600' : 'text-slate-400'
            }`
          }
        >
          <span className="text-xl leading-none">{it.icon}</span>
          {it.label}
          {it.badge > 0 && (
            <span className="absolute right-1/2 top-1 translate-x-3 rounded-full bg-brand-600 px-1.5 py-0.5 text-[10px] font-bold leading-none text-white">
              {it.badge}
            </span>
          )}
        </NavLink>
      ))}
      {(memberships.length > 0 || isPlatformAdmin) && (
        <NavLink
          to="/account"
          className="relative flex flex-1 flex-col items-center gap-0.5 py-2.5 text-[11px] font-medium text-slate-400"
        >
          <span className="text-xl leading-none">🔀</span>
          Espaces
        </NavLink>
      )}
    </nav>
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
  return (
    <CartProvider>
      <div className="mx-auto flex min-h-full max-w-md flex-col bg-slate-50 shadow-sm">
        <main className="flex-1 pb-20">
          <Routes>
            <Route index element={<Home />} />
            <Route path="s/:slug" element={<Catalog />} />
            <Route path="s/:slug/gallery" element={<Gallery />} />
            <Route path="s/:slug/p/:productId" element={<ProductDetail />} />
            <Route path="cart" element={<Cart />} />
            <Route path="checkout" element={<RequireAuth><Checkout /></RequireAuth>} />
            <Route path="orders" element={<Orders />} />
            <Route path="account" element={<Account />} />
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </main>
        <BottomNav />
      </div>
    </CartProvider>
  );
}
