import { createContext, useContext, useEffect, useMemo, useState } from 'react';

const CartContext = createContext(null);
const KEY = 'sbc_cart';

// Panier = UNE boutique (C1). Forme : { slug, shopName, items: [{ variantId, productId, name,
// color, size, price, qty }] }.
export function CartProvider({ children }) {
  const [cart, setCart] = useState(() => {
    try { return JSON.parse(localStorage.getItem(KEY) || 'null') || empty(); } catch { return empty(); }
  });

  useEffect(() => { localStorage.setItem(KEY, JSON.stringify(cart)); }, [cart]);

  /** Ajoute une declinaison. Renvoie false si le panier appartient a une AUTRE boutique. */
  const add = (slug, shopName, item, { force = false } = {}) => {
    if (cart.slug && cart.slug !== slug && cart.items.length > 0 && !force) return false;
    setCart((c) => {
      const base = c.slug === slug ? c : { slug, shopName, items: [] };
      const items = [...base.items];
      const i = items.findIndex((x) => x.variantId === item.variantId);
      if (i >= 0) items[i] = { ...items[i], qty: Math.min(items[i].qty + item.qty, item.max ?? 99) };
      else items.push(item);
      return { slug, shopName, items };
    });
    return true;
  };

  const setQty = (variantId, qty) =>
    setCart((c) => ({ ...c, items: c.items.map((x) => (x.variantId === variantId ? { ...x, qty } : x)) }));
  const remove = (variantId) =>
    setCart((c) => {
      const items = c.items.filter((x) => x.variantId !== variantId);
      return items.length ? { ...c, items } : empty();
    });
  const clear = () => setCart(empty());

  const count = useMemo(() => cart.items.reduce((s, x) => s + x.qty, 0), [cart]);
  const subtotal = useMemo(() => cart.items.reduce((s, x) => s + x.qty * Number(x.price), 0), [cart]);

  return (
    <CartContext.Provider value={{ cart, add, setQty, remove, clear, count, subtotal }}>
      {children}
    </CartContext.Provider>
  );
}

function empty() { return { slug: null, shopName: null, items: [] }; }
export const useCart = () => useContext(CartContext);
