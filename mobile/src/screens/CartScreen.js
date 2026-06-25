import { useState } from 'react';
import { View, Text, StyleSheet, FlatList, TouchableOpacity } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useNavigation } from '@react-navigation/native';
import { useCart } from '../cart/CartContext';
import { salesApi } from '../api/endpoints';
import { apiError } from '../api/client';
import { AppButton, AppTextInput, QtyStepper, ErrorBanner } from '../components';
import { colors } from '../theme';
import { formatMoney } from '../utils/format';

const PAYMENTS = [
  { key: 'ESPECES', label: 'Espèces' },
  { key: 'CARTE', label: 'Carte' },
  { key: 'MIXTE', label: 'Mixte' },
];

export default function CartScreen() {
  const navigation = useNavigation();
  const { items, setQuantity, remove, clear, subtotal, count } = useCart();
  const [payment, setPayment] = useState('ESPECES');
  const [discount, setDiscount] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  const validate = async () => {
    if (submitting || items.length === 0) return; // anti double-tap
    setSubmitting(true);
    setError('');
    try {
      const payload = {
        items: items.map((i) => ({ variantId: i.variant.variantId, quantity: i.quantity })),
        paymentMethod: payment,
      };
      const d = parseFloat(String(discount).replace(',', '.'));
      if (!Number.isNaN(d) && d > 0) payload.discount = d;

      const { data } = await salesApi.create(payload);
      // Succès : on vide le panier et on montre le reçu (totaux SERVEUR).
      clear();
      navigation.navigate('Receipt', { sale: data });
    } catch (err) {
      // 409 = stock insuffisant (message nomme le produit) -> on garde le panier pour corriger.
      setError(apiError(err));
    } finally {
      setSubmitting(false); // ne jamais auto-retry un POST /sales
    }
  };

  if (items.length === 0) {
    return (
      <SafeAreaView style={styles.safeCenter} edges={['bottom']}>
        <Text style={styles.emptyIcon}>🛒</Text>
        <Text style={styles.empty}>Panier vide</Text>
        <Text style={styles.emptySub}>Scannez un produit ou utilisez la recherche.</Text>
      </SafeAreaView>
    );
  }

  return (
    <SafeAreaView style={styles.safe} edges={['bottom']}>
      <FlatList
        data={items}
        keyExtractor={(i) => String(i.variant.variantId)}
        contentContainerStyle={{ padding: 16, gap: 10 }}
        renderItem={({ item }) => (
          <View style={styles.row}>
            <View style={{ flex: 1 }}>
              <Text style={styles.name}>{item.variant.productName}</Text>
              <Text style={styles.ref}>
                {item.variant.colorName} · {item.variant.size} — {formatMoney(item.variant.salePrice)} × {item.quantity} ={' '}
                {formatMoney(Number(item.variant.salePrice) * item.quantity)}
              </Text>
              <TouchableOpacity onPress={() => remove(item.variant.variantId)}>
                <Text style={styles.removeLink}>Retirer</Text>
              </TouchableOpacity>
            </View>
            <QtyStepper
              value={item.quantity}
              onChange={(q) => setQuantity(item.variant.variantId, q)}
              max={item.variant.quantity > 0 ? item.variant.quantity : undefined}
            />
          </View>
        )}
      />

      <View style={styles.footer}>
        <ErrorBanner message={error} />

        <Text style={styles.label}>Mode de paiement</Text>
        <View style={styles.payments}>
          {PAYMENTS.map((p) => (
            <TouchableOpacity
              key={p.key}
              style={[styles.payBtn, payment === p.key && styles.payBtnActive]}
              onPress={() => setPayment(p.key)}
            >
              <Text style={[styles.payText, payment === p.key && styles.payTextActive]}>{p.label}</Text>
            </TouchableOpacity>
          ))}
        </View>

        <Text style={styles.label}>Remise (montant, optionnel)</Text>
        <AppTextInput
          value={discount}
          onChangeText={setDiscount}
          placeholder="0.00"
          keyboardType="decimal-pad"
        />

        <View style={styles.totalRow}>
          <Text style={styles.totalLabel}>Sous-total indicatif ({count})</Text>
          <Text style={styles.totalValue}>{formatMoney(subtotal)}</Text>
        </View>
        <Text style={styles.note}>Le total définitif est calculé et renvoyé par le serveur.</Text>

        <AppButton
          title={submitting ? 'Validation…' : 'Valider la vente'}
          onPress={validate}
          loading={submitting}
          disabled={submitting}
        />
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: colors.bg },
  safeCenter: { flex: 1, backgroundColor: colors.bg, alignItems: 'center', justifyContent: 'center' },
  emptyIcon: { fontSize: 48 },
  empty: { fontSize: 18, fontWeight: '700', color: colors.text, marginTop: 8 },
  emptySub: { color: colors.muted, marginTop: 4 },
  row: { flexDirection: 'row', backgroundColor: '#fff', borderRadius: 12, padding: 14, borderWidth: 1, borderColor: colors.border, alignItems: 'center', gap: 12 },
  name: { fontSize: 16, fontWeight: '700', color: colors.text },
  ref: { fontSize: 13, color: colors.muted, marginVertical: 2 },
  removeLink: { color: colors.danger, fontSize: 13, fontWeight: '600', marginTop: 2 },
  footer: { backgroundColor: '#fff', borderTopWidth: 1, borderTopColor: colors.border, padding: 16, gap: 8 },
  label: { fontSize: 14, fontWeight: '600', color: colors.text },
  payments: { flexDirection: 'row', gap: 8 },
  payBtn: { flex: 1, paddingVertical: 10, borderRadius: 10, borderWidth: 1, borderColor: colors.border, alignItems: 'center' },
  payBtnActive: { backgroundColor: colors.primary, borderColor: colors.primary },
  payText: { color: colors.text, fontWeight: '600' },
  payTextActive: { color: '#fff' },
  totalRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginTop: 4 },
  totalLabel: { fontSize: 15, color: colors.muted },
  totalValue: { fontSize: 20, fontWeight: '800', color: colors.text },
  note: { fontSize: 12, color: colors.muted, fontStyle: 'italic' },
});
