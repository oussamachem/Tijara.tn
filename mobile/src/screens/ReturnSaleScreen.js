import { useState, useEffect } from 'react';
import { View, Text, StyleSheet, ScrollView, TouchableOpacity, Alert } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useNavigation } from '@react-navigation/native';
import { salesApi, returnsApi } from '../api/endpoints';
import { apiError } from '../api/client';
import { AppButton, AppTextInput, QtyStepper, Loading, ErrorBanner } from '../components';
import { colors } from '../theme';
import { formatMoney } from '../utils/format';

export default function ReturnSaleScreen({ route }) {
  const navigation = useNavigation();
  const { saleId } = route.params;

  const [sale, setSale] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const [selected, setSelected] = useState(null); // item sélectionné
  const [qty, setQty] = useState(1);
  const [reason, setReason] = useState('');
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    (async () => {
      try {
        const { data } = await salesApi.detail(saleId);
        setSale(data);
      } catch (err) {
        setError(apiError(err));
      } finally {
        setLoading(false);
      }
    })();
  }, [saleId]);

  const pick = (item) => {
    setSelected(item);
    setQty(1);
  };

  const submit = async () => {
    if (submitting || !selected) return;
    setSubmitting(true);
    setError('');
    try {
      await returnsApi.create({
        saleId,
        productId: selected.productId,
        quantity: qty,
        reason: reason.trim(),
      });
      Alert.alert('Retour enregistré', 'Le stock a été réintégré.', [
        { text: 'OK', onPress: () => navigation.goBack() },
      ]);
    } catch (err) {
      // 409 = plafond serveur dépassé (qty > vendu − déjà retourné).
      setError(apiError(err));
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) return <Loading text="Chargement de la vente…" />;
  if (!sale) return <ErrorBanner message={error || 'Vente introuvable'} />;

  return (
    <SafeAreaView style={styles.safe} edges={['bottom']}>
      <ScrollView contentContainerStyle={{ padding: 16, gap: 12 }}>
        <Text style={styles.title}>Vente #{sale.id}</Text>
        <Text style={styles.sub}>Choisissez l'article à retourner :</Text>

        {sale.items.map((it) => {
          const active = selected?.id === it.id;
          return (
            <TouchableOpacity
              key={it.id}
              style={[styles.item, active && styles.itemActive]}
              onPress={() => pick(it)}
              activeOpacity={0.7}
            >
              <View style={{ flex: 1 }}>
                <Text style={styles.itemName}>{it.productName}</Text>
                <Text style={styles.itemSub}>
                  {it.productReference} · vendu {it.quantity} × {formatMoney(it.unitPrice)}
                </Text>
              </View>
              {active && <Text style={styles.check}>✓</Text>}
            </TouchableOpacity>
          );
        })}

        {selected && (
          <View style={styles.panel}>
            <ErrorBanner message={error} />
            <Text style={styles.label}>Quantité à retourner (max {selected.quantity})</Text>
            <QtyStepper value={qty} onChange={setQty} max={selected.quantity} />
            <Text style={styles.note}>
              Le serveur refuse tout retour dépassant (vendu − déjà retourné).
            </Text>
            <View style={{ height: 10 }} />
            <Text style={styles.label}>Motif</Text>
            <AppTextInput
              value={reason}
              onChangeText={setReason}
              placeholder="Ex. taille incorrecte, défaut…"
              multiline
            />
            <View style={{ height: 14 }} />
            <AppButton
              title={submitting ? 'Validation…' : 'Valider le retour'}
              onPress={submit}
              loading={submitting}
              disabled={submitting}
            />
          </View>
        )}
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: colors.bg },
  title: { fontSize: 20, fontWeight: '800', color: colors.text },
  sub: { color: colors.muted },
  item: { flexDirection: 'row', alignItems: 'center', backgroundColor: '#fff', borderRadius: 12, padding: 14, borderWidth: 1, borderColor: colors.border },
  itemActive: { borderColor: colors.primary, borderWidth: 2 },
  itemName: { fontSize: 16, fontWeight: '700', color: colors.text },
  itemSub: { fontSize: 13, color: colors.muted },
  check: { fontSize: 20, color: colors.primary, fontWeight: '800' },
  panel: { backgroundColor: '#fff', borderRadius: 12, padding: 16, borderWidth: 1, borderColor: colors.border, marginTop: 4 },
  label: { fontSize: 14, fontWeight: '600', color: colors.text, marginBottom: 8 },
  note: { fontSize: 12, color: colors.muted, fontStyle: 'italic', marginTop: 8 },
});
