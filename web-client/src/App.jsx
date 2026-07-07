import { Routes, Route, NavLink, useLocation, Navigate } from 'react-router-dom';
import { useCart } from './cart/CartContext';
import { useAuth } from './auth/AuthContext';
import Home from './pages/Home.jsx';
import Catalog from './pages/Catalog.jsx';
import ProductDetail from './pages/ProductDetail.jsx';
import Cart from './pages/Cart.jsx';
import Checkout from './pages/Checkout.jsx';
import Orders from './pages/Orders.jsx';
import Account from './pages/Account.jsx';
import Login from './pages/Login.jsx';
import Register from './pages/Register.jsx';

function BottomNav() {
  const { count } = useCart();
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
    </nav>
  );
}

function RequireAuth({ children }) {
  const { isAuthenticated } = useAuth();
  const loc = useLocation();
  if (!isAuthenticated) return <Navigate to="/login" state={{ from: loc.pathname }} replace />;
  return children;
}

export default function App() {
  return (
    <div className="mx-auto flex min-h-full max-w-md flex-col bg-slate-50 shadow-sm">
      <main className="flex-1 pb-20">
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/s/:slug" element={<Catalog />} />
          <Route path="/s/:slug/p/:productId" element={<ProductDetail />} />
          <Route path="/cart" element={<Cart />} />
          <Route path="/checkout" element={<RequireAuth><Checkout /></RequireAuth>} />
          <Route path="/orders" element={<Orders />} />
          <Route path="/account" element={<Account />} />
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </main>
      <BottomNav />
    </div>
  );
}
