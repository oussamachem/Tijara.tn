import { createContext, useContext, useMemo, useState } from 'react';

const CartContext = createContext(null);

/**
 * Panier = commodité UI uniquement. Le sous-total affiché est INDICATIF (calculé sur les
 * prix lus). La source de vérité du total est la réponse de POST /api/sales (cf. reçu).
 */
export function CartProvider({ children }) {
  const [items, setItems] = useState([]); // [{ product, quantity }]

  const add = (product, quantity = 1) => {
    setItems((prev) => {
      const existing = prev.find((i) => i.product.id === product.id);
      if (existing) {
        return prev.map((i) =>
          i.product.id === product.id ? { ...i, quantity: i.quantity + quantity } : i
        );
      }
      return [...prev, { product, quantity }];
    });
  };

  const setQuantity = (productId, quantity) => {
    setItems((prev) =>
      prev
        .map((i) => (i.product.id === productId ? { ...i, quantity } : i))
        .filter((i) => i.quantity > 0)
    );
  };

  const remove = (productId) => setItems((prev) => prev.filter((i) => i.product.id !== productId));
  const clear = () => setItems([]);

  const count = useMemo(() => items.reduce((s, i) => s + i.quantity, 0), [items]);
  const subtotal = useMemo(
    () => items.reduce((s, i) => s + Number(i.product.salePrice) * i.quantity, 0),
    [items]
  );

  return (
    <CartContext.Provider value={{ items, add, setQuantity, remove, clear, count, subtotal }}>
      {children}
    </CartContext.Provider>
  );
}

export function useCart() {
  const ctx = useContext(CartContext);
  if (!ctx) throw new Error('useCart doit être utilisé dans CartProvider');
  return ctx;
}
