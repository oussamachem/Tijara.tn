import { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useShop } from '../context/ShopContext.jsx';
import { useAuth } from '../context/AuthContext.jsx';

const ROLE_LABEL = { OWNER: 'Propriétaire', VENDOR: 'Vendeur' };

/**
 * Sélecteur de contexte : bascule entre la plateforme (super-admin), les boutiques dont on est
 * membre (OWNER/VENDOR), et l'espace client — le tout SANS re-login. Change la boutique active
 * (X-Shop-Id) et renvoie à l'accueil de l'espace ciblé.
 */
export default function ShopSwitcher({ dark = false }) {
  const { memberships, mode, activeShop, isPlatformAdmin, switchToShop, switchToClient, switchToPlatform } = useShop();
  const { logout } = useAuth();
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);
  const ref = useRef(null);

  useEffect(() => {
    const onClick = (e) => ref.current && !ref.current.contains(e.target) && setOpen(false);
    document.addEventListener('mousedown', onClick);
    return () => document.removeEventListener('mousedown', onClick);
  }, []);

  const label =
    mode === 'platform' ? '🛡️ Plateforme'
    : mode === 'client' ? '👤 Espace client'
    : activeShop ? `🛍️ ${activeShop.name}` : '🛍️ Boutique';
  const sub = mode === 'shop' && activeShop ? ROLE_LABEL[activeShop.role] : null;

  const go = (fn) => { fn(); setOpen(false); navigate('/'); };

  const btn = dark
    ? 'bg-slate-800 text-white hover:bg-slate-700 border-slate-700'
    : 'bg-white text-slate-700 hover:bg-slate-50 border-slate-200';

  return (
    <div className="relative" ref={ref}>
      <button
        onClick={() => setOpen((o) => !o)}
        className={`flex max-w-[60vw] items-center gap-2 rounded-xl border px-3 py-2 text-sm font-semibold transition ${btn}`}
      >
        <span className="truncate">{label}</span>
        {sub && <span className="shrink-0 rounded-full bg-brand-100 px-2 py-0.5 text-[10px] font-bold text-brand-700">{sub}</span>}
        <span className="shrink-0 text-slate-400">▾</span>
      </button>

      {open && (
        <div className="absolute right-0 z-50 mt-2 w-64 overflow-hidden rounded-xl border border-slate-200 bg-white shadow-xl">
          {isPlatformAdmin && (
            <Item active={mode === 'platform'} onClick={() => go(switchToPlatform)} icon="🛡️" title="Plateforme" sub="Gestion des boutiques" />
          )}
          {memberships.map((m) => (
            <Item
              key={m.shopId}
              active={mode === 'shop' && activeShop?.shopId === m.shopId}
              onClick={() => go(() => switchToShop(m.shopId))}
              icon="🛍️"
              title={m.name}
              sub={`${ROLE_LABEL[m.role]}${m.status === 'SUSPENDED' ? ' · suspendue' : ''}`}
            />
          ))}
          <Item active={mode === 'client'} onClick={() => go(switchToClient)} icon="👤" title="Espace client" sub="Parcourir & commander" />
          <button
            onClick={() => { setOpen(false); navigate('/profile'); }}
            className="flex w-full items-center gap-3 border-t border-slate-100 px-4 py-3 text-left text-sm font-medium text-slate-700 hover:bg-slate-50"
          >
            <span className="text-lg">⚙️</span> Mon profil
          </button>
          <button
            onClick={() => { logout(); navigate('/login'); }}
            className="block w-full border-t border-slate-100 px-4 py-3 text-left text-sm font-medium text-rose-600 hover:bg-rose-50"
          >
            Se déconnecter
          </button>
        </div>
      )}
    </div>
  );
}

function Item({ active, onClick, icon, title, sub }) {
  return (
    <button
      onClick={onClick}
      className={`flex w-full items-center gap-3 px-4 py-3 text-left hover:bg-slate-50 ${active ? 'bg-brand-50' : ''}`}
    >
      <span className="text-lg">{icon}</span>
      <span className="min-w-0 flex-1">
        <span className="block truncate text-sm font-semibold text-slate-800">{title}</span>
        {sub && <span className="block truncate text-xs text-slate-400">{sub}</span>}
      </span>
      {active && <span className="text-brand-600">✓</span>}
    </button>
  );
}
