import { Routes, Route, NavLink, Navigate } from 'react-router-dom';
import { PosCartProvider, usePosCart } from '../vendor/PosCartContext.jsx';
import { useShop } from '../context/ShopContext.jsx';
import ShopSwitcher from '../components/ShopSwitcher.jsx';
import Pos from '../vendor/pages/Pos.jsx';
import Reservations from '../vendor/pages/Reservations.jsx';
import Returns from '../vendor/pages/Returns.jsx';
import MySales from '../vendor/pages/MySales.jsx';

const TABS = [
  { to: '/', icon: '🧾', label: 'Caisse', end: true },
  { to: '/reservations', icon: '📅', label: 'Réservations' },
  { to: '/returns', icon: '↩️', label: 'Retours' },
  { to: '/ventes', icon: '📊', label: 'Mes ventes' },
];

function BottomTabs() {
  const { count } = usePosCart();
  return (
    <nav className="safe-bottom fixed inset-x-0 bottom-0 z-30 flex items-stretch border-t border-slate-200 bg-white/95 backdrop-blur">
      {TABS.map((t) => (
        <NavLink key={t.to} to={t.to} end={t.end}
          className={({ isActive }) =>
            `relative flex flex-1 flex-col items-center gap-0.5 py-2.5 text-[11px] font-medium ${isActive ? 'text-brand-600' : 'text-slate-400'}`}>
          <span className="text-xl leading-none">{t.icon}</span>
          {t.label}
          {t.to === '/' && count > 0 && (
            <span className="absolute right-1/2 top-1 translate-x-4 rounded-full bg-brand-600 px-1.5 py-0.5 text-[10px] font-bold leading-none text-white">{count}</span>
          )}
        </NavLink>
      ))}
    </nav>
  );
}

/** Espace VENDEUR / CAISSE (rôle VENDOR sur la boutique active, X-Shop-Id). Mobile-first. */
export default function VendorSpace() {
  const { activeShop } = useShop();
  return (
    <PosCartProvider>
      <div className="flex h-full min-w-0 flex-col bg-slate-50">
        <header className="flex items-center justify-between gap-3 border-b border-slate-200 bg-white px-4 py-3">
          <div className="min-w-0">
            <div className="truncate font-bold text-slate-800">🛍️ {activeShop?.name || 'Caisse'}</div>
            <div className="text-xs text-slate-400">Caisse vendeur</div>
          </div>
          <ShopSwitcher />
        </header>
        <main className="min-w-0 flex-1 overflow-y-auto pb-16">
          <Routes>
            <Route index element={<Pos />} />
            <Route path="reservations" element={<Reservations />} />
            <Route path="returns" element={<Returns />} />
            <Route path="ventes" element={<MySales />} />
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </main>
        <BottomTabs />
      </div>
    </PosCartProvider>
  );
}
