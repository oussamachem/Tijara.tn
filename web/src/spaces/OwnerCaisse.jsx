import { Routes, Route, NavLink, Navigate, useNavigate } from 'react-router-dom';
import { PosCartProvider, usePosCart } from '../vendor/PosCartContext.jsx';
import { useShop } from '../context/ShopContext.jsx';
import Pos from '../vendor/pages/Pos.jsx';
import Reservations from '../vendor/pages/Reservations.jsx';
import Returns from '../vendor/pages/Returns.jsx';
import MySales from '../vendor/pages/MySales.jsx';

const TABS = [
  { to: '/caisse', icon: '🧾', label: 'Caisse', end: true },
  { to: '/caisse/reservations', icon: '📅', label: 'Réservations' },
  { to: '/caisse/retours', icon: '↩️', label: 'Retours' },
  { to: '/caisse/ventes', icon: '📊', label: 'Ventes' },
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
          {t.end && count > 0 && (
            <span className="absolute right-1/2 top-1 translate-x-4 rounded-full bg-brand-600 px-1.5 py-0.5 text-[10px] font-bold leading-none text-white">{count}</span>
          )}
        </NavLink>
      ))}
    </nav>
  );
}

/**
 * Caisse du PROPRIÉTAIRE : le patron tient lui-même la caisse. Plein écran (hors sidebar admin),
 * identique à l'espace vendeur mais avec un retour vers l'admin. Le propriétaire (SHOP_OWNER) est
 * autorisé sur les routes de vente (hasAnyRole SHOP_OWNER/SHOP_VENDOR).
 */
export default function OwnerCaisse() {
  const { activeShop } = useShop();
  const navigate = useNavigate();
  return (
    <PosCartProvider>
      <div className="flex h-full min-w-0 flex-col bg-slate-50">
        <header className="flex items-center gap-3 border-b border-slate-200 bg-white px-4 py-3">
          <button onClick={() => navigate('/')} className="-ml-2 flex h-9 w-9 items-center justify-center rounded-full text-slate-600 hover:bg-slate-100" aria-label="Retour admin">
            <span className="text-xl leading-none">‹</span>
          </button>
          <div className="min-w-0">
            <div className="truncate font-bold text-slate-800">🧾 Caisse — {activeShop?.name}</div>
            <div className="text-xs text-slate-400">Retour à l'administration</div>
          </div>
        </header>
        <main className="min-w-0 flex-1 overflow-y-auto pb-16">
          <Routes>
            <Route index element={<Pos reservationsPath="/caisse/reservations" />} />
            <Route path="reservations" element={<Reservations />} />
            <Route path="retours" element={<Returns />} />
            <Route path="ventes" element={<MySales all />} />
            <Route path="*" element={<Navigate to="/caisse" replace />} />
          </Routes>
        </main>
        <BottomTabs />
      </div>
    </PosCartProvider>
  );
}
