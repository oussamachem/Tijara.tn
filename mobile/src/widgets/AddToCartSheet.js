import { useState, useEffect } from 'react';
import { Modal, View, Text, StyleSheet, TouchableOpacity } from 'react-native';
import { useCart } from '../cart/CartContext';
import { AppButton, Badge, QtyStepper } from '../components';
import { colors } from '../theme';
import { formatMoney } from '../utils/format';

/**
 * Feuille modale : détails produit résolu + choix quantité + ajout au panier.
 * La quantité est pré-bornée par le stock affiché (indicatif) ; le serveur reste
 * autoritaire au moment de la vente.
 */
export default function AddToCartSheet({ product, visible, onClose }) {
  const { add } = useCart();
  const [qty, setQty] = useState(1);

  useEffect(() => {
    if (visible) setQty(1);
  }, [visible, product]);

  if (!product) return null;

  const outOfStock = product.quantity <= 0;

  const onAdd = () => {
    add(product, qty);
    onClose(true);
  };

  return (
    <Modal visible={visible} transparent animationType="slide" onRequestClose={() => onClose(false)}>
      <TouchableOpacity style={styles.backdrop} activeOpacity={1} onPress={() => onClose(false)}>
        <TouchableOpacity activeOpacity={1} style={styles.sheet}>
          <Text style={styles.name}>{product.name}</Text>
          <Text style={styles.ref}>{product.reference}</Text>

          <View style={styles.row}>
            <Text style={styles.price}>{formatMoney(product.salePrice)}</Text>
            {outOfStock ? (
              <Badge text="Rupture" color={colors.danger} />
            ) : (
              <Badge text={`Stock : ${product.quantity}`} color={product.lowStock ? colors.amber : colors.success} />
            )}
          </View>

          <View style={styles.qtyRow}>
            <Text style={styles.label}>Quantité</Text>
            <QtyStepper value={qty} onChange={setQty} max={product.quantity > 0 ? product.quantity : 1} />
          </View>

          <AppButton
            title={outOfStock ? 'Indisponible' : 'Ajouter au panier'}
            onPress={onAdd}
            disabled={outOfStock}
          />
          <View style={{ height: 8 }} />
          <AppButton title="Annuler" variant="secondary" onPress={() => onClose(false)} />
        </TouchableOpacity>
      </TouchableOpacity>
    </Modal>
  );
}

const styles = StyleSheet.create({
  backdrop: { flex: 1, backgroundColor: 'rgba(0,0,0,0.4)', justifyContent: 'flex-end' },
  sheet: { backgroundColor: '#fff', borderTopLeftRadius: 20, borderTopRightRadius: 20, padding: 20, gap: 6 },
  name: { fontSize: 20, fontWeight: '800', color: colors.text },
  ref: { fontSize: 13, color: colors.muted, marginBottom: 8 },
  row: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 },
  price: { fontSize: 22, fontWeight: '800', color: colors.primary },
  qtyRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginVertical: 12 },
  label: { fontSize: 16, fontWeight: '600', color: colors.text },
});
