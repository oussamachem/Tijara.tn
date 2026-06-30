import { useState, useEffect } from 'react';
import { Modal, View, Text, StyleSheet, TouchableOpacity } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { useCart } from '../cart/CartContext';
import { AppButton, Badge, QtyStepper } from '../components';
import { colors } from '../theme';
import { formatMoney } from '../utils/format';

/**
 * Feuille modale : variante résolue (scan ou recherche) + quantité + ajout au panier.
 * variant = { variantId, variantReference, productName, salePrice, colorName, size, quantity }
 */
export default function AddToCartSheet({ variant, visible, onClose }) {
  const { add } = useCart();
  const insets = useSafeAreaInsets();
  const [qty, setQty] = useState(1);

  useEffect(() => {
    if (visible) setQty(1);
  }, [visible, variant]);

  if (!variant) return null;

  const outOfStock = variant.quantity <= 0;
  const low = variant.quantity > 0 && variant.quantity <= (variant.seuilAlerte ?? 0);

  const onAdd = () => {
    add(variant, qty);
    onClose(true);
  };

  return (
    <Modal visible={visible} transparent animationType="slide" onRequestClose={() => onClose(false)}>
      <TouchableOpacity style={styles.backdrop} activeOpacity={1} onPress={() => onClose(false)}>
        <TouchableOpacity activeOpacity={1} style={[styles.sheet, { paddingBottom: insets.bottom + 20 }]}>
          <Text style={styles.name}>{variant.productName}</Text>
          <Text style={styles.decl}>{variant.colorName} · taille {variant.size}</Text>
          <Text style={styles.ref}>{variant.variantReference}</Text>

          <View style={styles.row}>
            <Text style={styles.price}>{formatMoney(variant.salePrice)}</Text>
            {outOfStock ? (
              <Badge text="Rupture" color={colors.danger} />
            ) : (
              <Badge text={`Stock : ${variant.quantity}`} color={low ? colors.amber : colors.success} />
            )}
          </View>

          <View style={styles.qtyRow}>
            <Text style={styles.label}>Quantité</Text>
            <QtyStepper value={qty} onChange={setQty} max={variant.quantity > 0 ? variant.quantity : 1} />
          </View>

          <AppButton title={outOfStock ? 'Indisponible' : 'Ajouter au panier'} onPress={onAdd} disabled={outOfStock} />
          <View style={{ height: 8 }} />
          <AppButton title="Annuler" variant="secondary" onPress={() => onClose(false)} />
        </TouchableOpacity>
      </TouchableOpacity>
    </Modal>
  );
}

const styles = StyleSheet.create({
  backdrop: { flex: 1, backgroundColor: 'rgba(0,0,0,0.4)', justifyContent: 'flex-end' },
  sheet: { backgroundColor: '#fff', borderTopLeftRadius: 20, borderTopRightRadius: 20, padding: 20, gap: 4 },
  name: { fontSize: 20, fontWeight: '800', color: colors.text },
  decl: { fontSize: 15, fontWeight: '600', color: colors.primary },
  ref: { fontSize: 12, color: colors.muted, marginBottom: 8 },
  row: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginVertical: 10 },
  price: { fontSize: 22, fontWeight: '800', color: colors.primary },
  qtyRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginVertical: 12 },
  label: { fontSize: 16, fontWeight: '600', color: colors.text },
});
