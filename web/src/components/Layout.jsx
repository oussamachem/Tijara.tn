import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext.jsx';

const navItems = [
  { to: '/', label: 'Tableau de bord', end: true, icon: '📊' },
  { to: '/produits', label: 'Produits', icon: '👕' },
  { to: '/categories', label: 'Catégories', icon: '🏷️' },
  { to: '/vendeurs', label: 'Vendeurs', icon: '👤' },
  { to: '/historique', label: 'Historique', icon: '🧾' },
];

export default function Layout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <div className="flex h-full">
      {/* Barre latérale */}
      <aside className="flex w-60 flex-col bg-slate-900 text-slate-200">
        <div className="px-5 py-5 text-lg font-bold text-white">
          🛍️ Smart Boutique
        </div>
        <nav className="flex-1 space-y-1 px-3">
          {navItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.end}
              className={({ isActive }) =>
                `flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition ${
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

      {/* Contenu */}
      <div className="flex flex-1 flex-col overflow-hidden">
        <header className="flex items-center justify-between border-b border-slate-200 bg-white px-6 py-3">
          <h1 className="font-semibold text-slate-700">Administration</h1>
          <button
            onClick={handleLogout}
            className="rounded-lg px-3 py-1.5 text-sm font-medium text-slate-600 hover:bg-slate-100"
          >
            Déconnexion
          </button>
        </header>
        <main className="flex-1 overflow-y-auto p-6">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
