import { createContext, useContext, useMemo, useState } from 'react';

const PosCartContext = createContext(null);

/**
 * Panier de CAISSE (POS) = commodité UI. Chaque ligne cible une VARIANTE résolue par scan/recherche
 * (forme VariantScanResponse). Le sous-total affiché est INDICATIF ; la source de vérité du total
 * est la réponse de POST /api/sales.
 */
export function PosCartProvider({ children }) {
  const [items, setItems] = useState([]); // [{ variant, quantity }]

  const add = (variant, quantity = 1) => {
    setItems((prev) => {
      const existing = prev.find((i) => i.variant.variantId === variant.variantId);
      if (existing) {
        return prev.map((i) =>
          i.variant.variantId === variant.variantId ? { ...i, quantity: i.quantity + quantity } : i
        );
      }
      return [...prev, { variant, quantity }];
    });
  };

  const setQuantity = (variantId, quantity) =>
    setItems((prev) =>
      prev.map((i) => (i.variant.variantId === variantId ? { ...i, quantity } : i)).filter((i) => i.quantity > 0)
    );

  const remove = (variantId) => setItems((prev) => prev.filter((i) => i.variant.variantId !== variantId));
  const clear = () => setItems([]);

  const count = useMemo(() => items.reduce((s, i) => s + i.quantity, 0), [items]);
  const subtotal = useMemo(
    () => items.reduce((s, i) => s + Number(i.variant.salePrice) * i.quantity, 0),
    [items]
  );

  return (
    <PosCartContext.Provider value={{ items, add, setQuantity, remove, clear, count, subtotal }}>
      {children}
    </PosCartContext.Provider>
  );
}

export function usePosCart() {
  const ctx = useContext(PosCartContext);
  if (!ctx) throw new Error('usePosCart doit être utilisé dans PosCartProvider');
  return ctx;
}
