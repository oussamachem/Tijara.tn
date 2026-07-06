import { useState, useCallback } from 'react';
import { View, Text, StyleSheet, ScrollView, TouchableOpacity, Alert, RefreshControl } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useFocusEffect, useRoute, useNavigation } from '@react-navigation/native';
import { reservationsApi } from '../api/endpoints';
import { apiError } from '../api/client';
import { AppButton, AppTextInput, ErrorBanner, Loading, Badge, Card } from '../components';
import { colors } from '../theme';
import { formatMoney, formatDate } from '../utils/format';

const METHODS = [
  { key: 'ESPECES', label: 'Espèces' },
  { key: 'CARTE', label: 'Carte' },
  { key: 'TICKET_CADEAU', label: 'Ticket cadeau' },
];
const STATUS_COLOR = { ACTIVE: colors.primary, COMPLETED: colors.success, EXPIRED: colors.danger, CANCELLED: colors.muted };
const STATUS_LABEL = { ACTIVE: 'En cours', COMPLETED: 'Soldée', EXPIRED: 'Expirée', CANCELLED: 'Annulée' };

export default function ReservationDetailScreen() {
  const { id } = useRoute().params;
  const navigation = useNavigation();
  const [res, setRes] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [amount, setAmount] = useState('');
  const [method, setMethod] = useState('ESPECES');
  const [busy, setBusy] = useState(false);

  const load = useCallback(async () => {
    setError('');
    try {
      const { data } = await reservationsApi.detail(id);
      setRes(data);
    } catch (err) {
      setError(apiError(err));
    } finally {
      setLoading(false);
    }
  }, [id]);

  useFocusEffect(useCallback(() => { setLoading(true); load(); }, [load]));

  const pay = async (rawAmount) => {
    const n = parseFloat(String(rawAmount).replace(',', '.'));
    if (busy) return;
    if (Number.isNaN(n) || n <= 0) { setError('Montant invalide.'); return; }
    setBusy(true); setError('');
    try {
      const { data } = await reservationsApi.pay(id, { amount: n, method });
      setRes(data);
      setAmount('');
      if (data.status === 'COMPLETED') {
        Alert.alert('Réservation soldée', 'Vous pouvez remettre le produit au client.');
      }
    } catch (err) {
      setError(apiError(err)); // 400 = versement > reste ; 409 = non active
    } finally {
      setBusy(false);
    }
  };

  const confirmCancel = () => {
    Alert.alert('Annuler la réservation', 'Le stock sera rendu. L\'acompte déjà versé est retenu.', [
      { text: 'Retour', style: 'cancel' },
      {
        text: 'Annuler la réservation', style: 'destructive',
        onPress: async () => {
          setBusy(true); setError('');
          try { const { data } = await reservationsApi.cancel(id); setRes(data); }
          catch (err) { setError(apiError(err)); }
          finally { setBusy(false); }
        },
      },
    ]);
  };

  if (loading) return <Loading text="Chargement…" />;
  if (!res) return <SafeAreaView style={styles.safe}><ErrorBanner message={error || 'Introuvable'} onRetry={load} /></SafeAreaView>;

  const active = res.status === 'ACTIVE';

  return (
    <SafeAreaView style={styles.safe} edges={['bottom']}>
      <ScrollView contentContainerStyle={{ padding: 16, gap: 12 }}
        refreshControl={<RefreshControl refreshing={false} onRefresh={load} />}>
        <ErrorBanner message={error} onRetry={load} />

        <Card>
          <View style={styles.headRow}>
            <Text style={styles.ref}>{res.reference}</Text>
            <Badge text={STATUS_LABEL[res.status] || res.status} color={STATUS_COLOR[res.status]} />
          </View>
          <Text style={styles.customer}>{res.customerName}{res.customerPhone ? ` · ${res.customerPhone}` : ''}</Text>
          {active && (
            <Text style={[styles.due, res.daysRemaining <= 4 && { color: colors.danger, fontWeight: '700' }]}>
              Échéance {formatDate(res.dueDate)} · {res.daysRemaining < 0 ? `en retard de ${-res.daysRemaining} j` : `${res.daysRemaining} j restant(s)`}
            </Text>
          )}
          {res.depositForfeited && <Text style={styles.forfeit}>Acompte retenu (non remboursé).</Text>}

          <View style={styles.amounts}>
            <View style={styles.amountCell}><Text style={styles.amtLabel}>Total</Text><Text style={styles.amtVal}>{formatMoney(res.total)}</Text></View>
            <View style={styles.amountCell}><Text style={styles.amtLabel}>Versé</Text><Text style={[styles.amtVal, { color: colors.success }]}>{formatMoney(res.paid)}</Text></View>
            <View style={styles.amountCell}><Text style={styles.amtLabel}>Reste</Text><Text style={[styles.amtVal, { color: Number(res.remaining) > 0 ? colors.danger : colors.success }]}>{formatMoney(res.remaining)}</Text></View>
          </View>
        </Card>

        <Card>
          <Text style={styles.section}>Articles réservés</Text>
          {res.items.map((it, idx) => (
            <View key={idx} style={styles.itemRow}>
              <Text style={styles.itemName}>{it.productName}</Text>
              <Text style={styles.itemSub}>{it.colorName} · {it.size} — {formatMoney(it.unitPrice)} × {it.quantity}</Text>
            </View>
          ))}
        </Card>

        <Card>
          <Text style={styles.section}>Versements</Text>
          {res.payments.length === 0 ? (
            <Text style={styles.muted}>Aucun versement.</Text>
          ) : res.payments.map((p, idx) => (
            <View key={idx} style={styles.payRow}>
              <Text style={styles.payMethod}>{METHODS.find((m) => m.key === p.method)?.label || p.method}</Text>
              <Text style={styles.paySub}>{formatDate(p.createdAt)}</Text>
              <Text style={styles.payAmt}>{formatMoney(p.amount)}</Text>
            </View>
          ))}
        </Card>

        {active && (
          <Card>
            <Text style={styles.section}>Ajouter un versement</Text>
            <View style={styles.methods}>
              {METHODS.map((m) => (
                <TouchableOpacity key={m.key} style={[styles.methodBtn, method === m.key && styles.methodActive]} onPress={() => setMethod(m.key)}>
                  <Text style={[styles.methodText, method === m.key && styles.methodTextActive]}>{m.label}</Text>
                </TouchableOpacity>
              ))}
            </View>
            <View style={{ height: 8 }} />
            <AppTextInput value={amount} onChangeText={setAmount} placeholder={`Montant (reste ${formatMoney(res.remaining)})`} keyboardType="decimal-pad" />
            <View style={{ height: 8 }} />
            <AppButton title={busy ? 'Traitement…' : 'Enregistrer le versement'} onPress={() => pay(amount)} loading={busy} disabled={busy} />
            <View style={{ height: 8 }} />
            <AppButton title={`Solder le reste (${formatMoney(res.remaining)}) — remettre au client`} variant="secondary" onPress={() => pay(res.remaining)} disabled={busy} />
            <View style={{ height: 8 }} />
            <AppButton title="Annuler la réservation" variant="danger" onPress={confirmCancel} disabled={busy} />
          </Card>
        )}

        {res.status === 'COMPLETED' && (
          <Card><Text style={styles.done}>✓ Réservation soldée — produit remis au client.</Text></Card>
        )}
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: colors.bg },
  headRow: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
  ref: { fontSize: 18, fontWeight: '800', color: colors.text },
  customer: { fontSize: 15, color: colors.text, marginTop: 4 },
  due: { fontSize: 13, color: colors.muted, marginTop: 4 },
  forfeit: { fontSize: 13, color: colors.danger, fontWeight: '600', marginTop: 4 },
  amounts: { flexDirection: 'row', marginTop: 12, gap: 8 },
  amountCell: { flex: 1, backgroundColor: colors.bg, borderRadius: 10, padding: 10, alignItems: 'center' },
  amtLabel: { fontSize: 12, color: colors.muted },
  amtVal: { fontSize: 16, fontWeight: '800', color: colors.text, marginTop: 2 },
  section: { fontSize: 15, fontWeight: '700', color: colors.text, marginBottom: 8 },
  itemRow: { paddingVertical: 6, borderBottomWidth: 1, borderBottomColor: colors.border },
  itemName: { fontSize: 15, fontWeight: '600', color: colors.text },
  itemSub: { fontSize: 13, color: colors.muted, marginTop: 2 },
  payRow: { flexDirection: 'row', alignItems: 'center', paddingVertical: 6, gap: 8 },
  payMethod: { fontSize: 14, fontWeight: '600', color: colors.text, flex: 1 },
  paySub: { fontSize: 12, color: colors.muted },
  payAmt: { fontSize: 15, fontWeight: '700', color: colors.success, minWidth: 90, textAlign: 'right' },
  muted: { color: colors.muted },
  methods: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },
  methodBtn: { flexGrow: 1, paddingVertical: 10, paddingHorizontal: 8, borderRadius: 10, borderWidth: 1, borderColor: colors.border, alignItems: 'center' },
  methodActive: { backgroundColor: colors.primary, borderColor: colors.primary },
  methodText: { color: colors.text, fontWeight: '600' },
  methodTextActive: { color: '#fff' },
  done: { fontSize: 15, fontWeight: '700', color: colors.success, textAlign: 'center' },
});
