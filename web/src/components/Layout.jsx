import { useState } from 'react';
import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext.jsx';

const navItems = [
  { to: '/', label: 'Tableau de bord', end: true, icon: '📊' },
  { to: '/produits', label: 'Produits', icon: '👕' },
  { to: '/categories', label: 'Catégories', icon: '🏷️' },
  { to: '/couleurs', label: 'Couleurs', icon: '🎨' },
  { to: '/tailles', label: 'Tailles', icon: '📏' },
  { to: '/vendeurs', label: 'Vendeurs', icon: '👤' },
  { to: '/credits', label: 'Crédits', icon: '💳' },
  { to: '/dettes', label: 'Dettes fournisseurs', icon: '📒' },
  { to: '/historique', label: 'Historique', icon: '🧾' },
];

export default function Layout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  // Drawer mobile (< lg) ; sur desktop la sidebar est statique (cet etat est ignore).
  const [sidebarOpen, setSidebarOpen] = useState(false);

  const handleLogout = () => {
    logout();
    navigate('/login');
  };
  const closeDrawer = () => setSidebarOpen(false);

  return (
    <div className="flex h-full">
      {/* Backdrop : mobile uniquement, quand le drawer est ouvert */}
      {sidebarOpen && (
        <div
          className="fixed inset-0 z-30 bg-black/50 lg:hidden"
          onClick={closeDrawer}
          aria-hidden="true"
        />
      )}

      {/* Barre latérale : drawer off-canvas < lg, statique >= lg (desktop inchange) */}
      <aside
        className={`fixed inset-y-0 left-0 z-40 flex w-60 flex-col bg-slate-900 text-slate-200
                    transform transition-transform duration-200 ease-in-out
                    lg:static lg:z-auto lg:translate-x-0
                    ${sidebarOpen ? 'translate-x-0' : '-translate-x-full'}`}
      >
        <div className="flex items-center justify-between px-5 py-5 text-lg font-bold text-white">
          <span>🛍️ Smart Boutique</span>
          <button
            onClick={closeDrawer}
            aria-label="Fermer le menu"
            className="rounded-md p-1 text-slate-300 hover:bg-slate-800 lg:hidden"
          >
            ✕
          </button>
        </div>
        <nav className="flex-1 space-y-1 overflow-y-auto px-3">
          {navItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.end}
              onClick={closeDrawer}
              className={({ isActive }) =>
                `flex items-center gap-3 rounded-lg px-3 py-3 text-sm font-medium transition ${
                  isActive ? 'bg-brand-600 text-white' : 'text-slate-300 hover:bg-slate-800'
                }`
              }
            >
              <span>{item.icon}</span>
              {item.label}
            </NavLink>
          ))}
        </nav>
        <div className="border-t border-slate-800 px-5 py-3 text-xs text-slate-400">
          Connecté : {user?.fullName}
        </div>
      </aside>

      {/* Contenu — min-w-0 = fix crucial contre le debordement horizontal */}
      <div className="flex min-w-0 flex-1 flex-col">
        <header className="flex items-center justify-between gap-3 border-b border-slate-200 bg-white px-4 py-3 lg:px-6">
          <div className="flex min-w-0 items-center gap-3">
            {/* Hamburger : mobile uniquement */}
            <button
              onClick={() => setSidebarOpen(true)}
              aria-label="Ouvrir le menu"
              className="-ml-1 rounded-md p-2 text-slate-600 hover:bg-slate-100 lg:hidden"
            >
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M3 6h18M3 12h18M3 18h18" strokeLinecap="round" />
              </svg>
            </button>
            <h1 className="truncate font-semibold text-slate-700">Administration</h1>
          </div>
          <button
            onClick={handleLogout}
            className="shrink-0 rounded-lg px-3 py-1.5 text-sm font-medium text-slate-600 hover:bg-slate-100"
          >
            Déconnexion
          </button>
        </header>
        <main className="min-w-0 flex-1 overflow-y-auto p-4 lg:p-6">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
