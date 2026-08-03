import { createContext, useCallback, useContext, useEffect, useState } from 'react';
import { useAuth } from './AuthContext.jsx';
import { meApi } from '../api/endpoints.js';
import { setActiveShopId } from '../api/client.js';

const ShopContext = createContext(null);
const CTX_KEY = 'sb_ctx'; // { kind: 'client' | 'shop' | 'platform', shopId? }

function readCtx() {
  try {
    return JSON.parse(localStorage.getItem(CTX_KEY) || 'null');
  } catch {
    return null;
  }
}

/**
 * Contexte de travail actif. Après login, charge GET /api/me/shops PUIS résout le contexte (dans le
 * MÊME effet, avec les memberships réellement chargés — évite toute course qui renverrait un OWNER
 * côté client). Visiteur anonyme = marketplace public. Le même compte bascule sans re-login.
 */
export function ShopProvider({ children }) {
  const { user, isPlatformAdmin } = useAuth();
  const [memberships, setMemberships] = useState([]);
  const [ready, setReady] = useState(false);
  const [ctx, setCtx] = useState(readCtx);

  const applyCtx = useCallback((next) => {
    setCtx(next);
    localStorage.setItem(CTX_KEY, JSON.stringify(next));
    setActiveShopId(next.kind === 'shop' ? next.shopId : null);
  }, []);

  // Résout le contexte à partir des memberships fraîchement chargés (respecte un choix explicite
  // encore valide, sinon défaut : boutique > plateforme > client).
  const resolve = useCallback((members, admin) => {
    const persisted = readCtx();
    const stillValid = persisted && (
      persisted.kind === 'client'
      || (persisted.kind === 'platform' && admin)
      || (persisted.kind === 'shop' && members.some((m) => m.shopId === persisted.shopId)));
    let next = persisted;
    if (!stillValid) {
      if (members.length > 0) next = { kind: 'shop', shopId: members[0].shopId };
      else if (admin) next = { kind: 'platform' };
      else next = { kind: 'client' };
    }
    applyCtx(next);
  }, [applyCtx]);

  useEffect(() => {
    let alive = true;
    setReady(false);
    if (!user) {
      // Visiteur anonyme : marketplace public (aucun X-Shop-Id). On NE touche PAS au contexte choisi.
      setMemberships([]);
      setActiveShopId(null);
      setReady(true);
      return undefined;
    }
    meApi.shops()
      .then(({ data }) => { if (!alive) return; setMemberships(data); resolve(data, isPlatformAdmin); })
      .catch(() => { if (!alive) return; setMemberships([]); resolve([], isPlatformAdmin); })
      .finally(() => { if (alive) setReady(true); });
    return () => { alive = false; };
  }, [user, isPlatformAdmin, resolve]);

  const activeMembership =
    ctx?.kind === 'shop' ? memberships.find((m) => m.shopId === ctx.shopId) || null : null;

  const value = {
    ready,
    memberships,
    mode: ctx?.kind || 'client',
    role: activeMembership?.role || null,
    activeShop: activeMembership,
    activeShopId: ctx?.kind === 'shop' ? ctx.shopId : null,
    isPlatformAdmin,
    switchToShop: (shopId) => applyCtx({ kind: 'shop', shopId }),
    switchToClient: () => applyCtx({ kind: 'client' }),
    switchToPlatform: () => applyCtx({ kind: 'platform' }),
    refresh: () => meApi.shops().then(({ data }) => setMemberships(data)).catch(() => {}),
  };

  return <ShopContext.Provider value={value}>{children}</ShopContext.Provider>;
}

export function useShop() {
  const ctx = useContext(ShopContext);
  if (!ctx) throw new Error('useShop doit être utilisé dans ShopProvider');
  return ctx;
}
