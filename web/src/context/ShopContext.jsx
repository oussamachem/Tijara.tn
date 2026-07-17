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
 * Contexte de travail actif de l'utilisateur. Après login, charge GET /api/me/shops et résout un
 * contexte : plateforme (super-admin), boutique (OWNER/VENDOR), ou client (aucune boutique). Le
 * même compte bascule de contexte SANS re-login. Pose la boutique active (X-Shop-Id) via
 * setActiveShopId AVANT que l'espace ne monte (garde `resolved`).
 */
export function ShopProvider({ children }) {
  const { user, isPlatformAdmin } = useAuth();
  const [memberships, setMemberships] = useState([]);
  const [ready, setReady] = useState(false);     // memberships chargés
  const [resolved, setResolved] = useState(false); // contexte actif fixé (+ X-Shop-Id posé)
  const [ctx, setCtx] = useState(readCtx);

  const applyCtx = useCallback((next) => {
    setCtx(next);
    localStorage.setItem(CTX_KEY, JSON.stringify(next));
    setActiveShopId(next.kind === 'shop' ? next.shopId : null);
  }, []);

  // (re)charge les memberships quand l'identité change.
  useEffect(() => {
    let alive = true;
    setReady(false);
    setResolved(false);
    if (!user) {
      setMemberships([]);
      setActiveShopId(null);
      return;
    }
    meApi
      .shops()
      .then(({ data }) => alive && setMemberships(data))
      .catch(() => alive && setMemberships([]))
      .finally(() => alive && setReady(true));
    return () => {
      alive = false;
    };
  }, [user]);

  // Résout le contexte actif une fois les memberships prêts (et le pose sur l'interceptor).
  useEffect(() => {
    if (!ready || !user) return;
    const persisted = ctx;
    const stillValid =
      persisted &&
      ((persisted.kind === 'client') ||
        (persisted.kind === 'platform' && isPlatformAdmin) ||
        (persisted.kind === 'shop' && memberships.some((m) => m.shopId === persisted.shopId)));

    let next = persisted;
    if (!stillValid) {
      if (memberships.length > 0) next = { kind: 'shop', shopId: memberships[0].shopId };
      else if (isPlatformAdmin) next = { kind: 'platform' };
      else next = { kind: 'client' };
    }
    applyCtx(next);
    setResolved(true);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [ready, memberships, user, isPlatformAdmin]);

  const activeMembership =
    ctx?.kind === 'shop' ? memberships.find((m) => m.shopId === ctx.shopId) || null : null;

  const value = {
    ready: ready && resolved,
    memberships,
    mode: ctx?.kind || 'client',           // 'client' | 'shop' | 'platform'
    role: activeMembership?.role || null,  // 'OWNER' | 'VENDOR' | null
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
