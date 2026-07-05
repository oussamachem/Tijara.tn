import { useState } from 'react';
import { View, Text, StyleSheet, FlatList, TouchableOpacity, Modal, ScrollView, Alert } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useNavigation } from '@react-navigation/native';
import { useCart } from '../cart/CartContext';
import { salesApi } from '../api/endpoints';
import { apiError } from '../api/client';
import { AppButton, AppTextInput, QtyStepper, ErrorBanner } from '../components';
import TicketScanner from '../widgets/TicketScanner';
import { colors } from '../theme';
import { formatMoney } from '../utils/format';

const PAYMENTS = [
  { key: 'ESPECES', label: 'Espèces' },
  { key: 'CARTE', label: 'Carte' },
  { key: 'MIXTE', label: 'Mixte' },
  { key: 'TICKET_CADEAU', label: 'Ticket cadeau' },
];
const ISSUERS = ['PLUXEE', 'JOKER', 'AUTRE'];
const DENOMINATIONS = [10, 20, 50]; // MVP (aligne le defaut serveur)
const maskCode = (c) => (c && c.length > 4 ? `••••${c.slice(-4)}` : c);
const isExpired = (d) => d && !Number.isNaN(Date.parse(d)) && new Date(d) < new Date(new Date().toDateString());

export default function CartScreen() {
  const navigation = useNavigation();
  const { items, setQuantity, remove, clear, subtotal, count } = useCart();
  const [payment, setPayment] = useState('ESPECES');
  const [discount, setDiscount] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  // --- Ticket cadeau ---
  const [tickets, setTickets] = useState([]); // {code, issuer, value, expiry}
  const [scanOpen, setScanOpen] = useState(false);
  const [form, setForm] = useState(null); // {code, issuer, value, expiry}
  const [remainderMethod, setRemainderMethod] = useState('ESPECES');

  const d = parseFloat(String(discount).replace(',', '.'));
  const disc = !Number.isNaN(d) && d > 0 ? d : 0;
  const total = Math.max(0, subtotal - disc);            // indicatif ; serveur autoritaire
  const ticketsTotal = tickets.reduce((s, t) => s + Number(t.value), 0);
  const reste = Math.max(0, total - ticketsTotal);
  const surplus = ticketsTotal > total;

  const onScanned = (code) => {
    setScanOpen(false);
    if (!code) return;
    if (tickets.some((t) => t.code === code)) {
      Alert.alert('Ticket déjà scanné', 'Ce ticket est déjà dans le panier.');
      return;
    }
    setForm({ code, issuer: 'PLUXEE', value: 20, expiry: '' });
  };
  const addTicket = () => {
    if (isExpired(form.expiry)) {
      Alert.alert('Ticket expiré', 'La date saisie est dépassée — le serveur refusera ce ticket.');
      return;
    }
    setTickets((ts) => [...ts, { ...form }]);
    setForm(null);
  };
  const removeTicket = (code) => setTickets((ts) => ts.filter((t) => t.code !== code));

  const validate = async () => {
    if (submitting || items.length === 0) return;
    if (payment === 'TICKET_CADEAU' && tickets.length === 0) {
      setError('Ajoutez au moins un ticket cadeau (ou changez de mode de paiement).');
      return;
    }
    setSubmitting(true);
    setError('');
    try {
      const payload = { items: items.map((i) => ({ variantId: i.variant.variantId, quantity: i.quantity })) };
      if (disc > 0) payload.discount = disc;

      if (payment === 'TICKET_CADEAU') {
        const payments = tickets.map((t) => ({
          method: 'TICKET_CADEAU', amount: Number(t.value), issuer: t.issuer,
          ticketCode: t.code, ticketExpiry: t.expiry || null,
        }));
        if (reste > 0) payments.push({ method: remainderMethod, amount: reste });
        payload.payments = payments;
      } else {
        payload.paymentMethod = payment;
      }

      const { data } = await salesApi.create(payload);
      clear();
      setTickets([]);
      navigation.navigate('Receipt', { sale: data });
    } catch (err) {
      // 409 = stock insuffisant OU ticket déjà utilisé ; 400 = ticket expiré / dénomination / insuffisant.
      setError(apiError(err));
    } finally {
      setSubmitting(false);
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

        {payment === 'TICKET_CADEAU' && (
          <View style={styles.ticketBox}>
            <AppButton title="＋ Scanner un ticket" variant="secondary" onPress={() => setScanOpen(true)} />
            {tickets.length > 0 && (
              <ScrollView style={{ maxHeight: 130 }} contentContainerStyle={{ gap: 6 }}>
                {tickets.map((t) => (
                  <View key={t.code} style={styles.ticketRow}>
                    <View style={{ flex: 1 }}>
                      <Text style={styles.ticketVal}>{t.issuer} · {formatMoney(t.value)} {isExpired(t.expiry) && '⚠️'}</Text>
                      <Text style={styles.ticketCode}>{maskCode(t.code)}{t.expiry ? ` · exp. ${t.expiry}` : ''}</Text>
                    </View>
                    <TouchableOpacity onPress={() => removeTicket(t.code)}><Text style={styles.removeLink}>Retirer</Text></TouchableOpacity>
                  </View>
                ))}
              </ScrollView>
            )}
            <View style={styles.ticketTotals}>
              <Text style={styles.ticketTotalTxt}>Total tickets : <Text style={{ fontWeight: '800' }}>{formatMoney(ticketsTotal)}</Text></Text>
              <Text style={styles.ticketTotalTxt}>Reste : <Text style={{ fontWeight: '800', color: reste > 0 ? colors.danger : colors.success }}>{formatMoney(reste)}</Text></Text>
            </View>
            {surplus && <Text style={styles.warn}>⚠️ Le total des tickets dépasse le montant : le surplus n'est pas remboursé.</Text>}
            {reste > 0 && (
              <View style={styles.remainderRow}>
                <Text style={styles.smallLabel}>Reste à payer en :</Text>
                {['ESPECES', 'CARTE'].map((m) => (
                  <TouchableOpacity key={m} style={[styles.remBtn, remainderMethod === m && styles.payBtnActive]} onPress={() => setRemainderMethod(m)}>
                    <Text style={[styles.payText, remainderMethod === m && styles.payTextActive]}>{m === 'ESPECES' ? 'Espèces' : 'Carte'}</Text>
                  </TouchableOpacity>
                ))}
              </View>
            )}
          </View>
        )}

        <Text style={styles.label}>Remise (montant, optionnel)</Text>
        <AppTextInput value={discount} onChangeText={setDiscount} placeholder="0.00" keyboardType="decimal-pad" />

        <View style={styles.totalRow}>
          <Text style={styles.totalLabel}>Sous-total indicatif ({count})</Text>
          <Text style={styles.totalValue}>{formatMoney(subtotal)}</Text>
        </View>
        <Text style={styles.note}>Le total définitif est calculé et renvoyé par le serveur.</Text>

        <AppButton title={submitting ? 'Validation…' : 'Valider la vente'} onPress={validate} loading={submitting} disabled={submitting} />
      </View>

      {/* Scanner ticket (QR ou code-barres) */}
      <TicketScanner visible={scanOpen} onClose={() => setScanOpen(false)} onScanned={onScanned} />

      {/* Modale : détails du ticket scanné */}
      <Modal visible={!!form} transparent animationType="slide" onRequestClose={() => setForm(null)}>
        <TouchableOpacity style={styles.backdrop} activeOpacity={1} onPress={() => setForm(null)}>
          <TouchableOpacity activeOpacity={1} style={styles.sheet}>
            <Text style={styles.sheetTitle}>Ticket cadeau</Text>
            <Text style={styles.ticketCode}>{form && maskCode(form.code)}</Text>

            <Text style={styles.smallLabel}>Émetteur</Text>
            <View style={styles.chips}>
              {ISSUERS.map((iss) => (
                <TouchableOpacity key={iss} style={[styles.chip, form?.issuer === iss && styles.payBtnActive]} onPress={() => setForm((f) => ({ ...f, issuer: iss }))}>
                  <Text style={[styles.payText, form?.issuer === iss && styles.payTextActive]}>{iss}</Text>
                </TouchableOpacity>
              ))}
            </View>

            <Text style={styles.smallLabel}>Valeur (DT)</Text>
            <View style={styles.chips}>
              {DENOMINATIONS.map((v) => (
                <TouchableOpacity key={v} style={[styles.chip, form?.value === v && styles.payBtnActive]} onPress={() => setForm((f) => ({ ...f, value: v }))}>
                  <Text style={[styles.payText, form?.value === v && styles.payTextActive]}>{v}</Text>
                </TouchableOpacity>
              ))}
            </View>

            <Text style={styles.smallLabel}>Expiration imprimée (AAAA-MM-JJ, optionnel)</Text>
            <AppTextInput value={form?.expiry} onChangeText={(v) => setForm((f) => ({ ...f, expiry: v }))} placeholder="2026-12-31" autoCapitalize="none" />
            {isExpired(form?.expiry) && <Text style={styles.warn}>⚠️ Date dépassée — le ticket sera refusé.</Text>}

            <View style={{ height: 8 }} />
            <AppButton title="Ajouter le ticket" onPress={addTicket} />
            <View style={{ height: 8 }} />
            <AppButton title="Annuler" variant="secondary" onPress={() => setForm(null)} />
          </TouchableOpacity>
        </TouchableOpacity>
      </Modal>
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
  smallLabel: { fontSize: 13, fontWeight: '600', color: colors.muted, marginTop: 4 },
  payments: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },
  payBtn: { flexGrow: 1, paddingVertical: 10, paddingHorizontal: 8, borderRadius: 10, borderWidth: 1, borderColor: colors.border, alignItems: 'center' },
  payBtnActive: { backgroundColor: colors.primary, borderColor: colors.primary },
  payText: { color: colors.text, fontWeight: '600' },
  payTextActive: { color: '#fff' },
  ticketBox: { backgroundColor: colors.bg, borderRadius: 12, padding: 10, gap: 8, borderWidth: 1, borderColor: colors.border },
  ticketRow: { flexDirection: 'row', alignItems: 'center', backgroundColor: '#fff', borderRadius: 10, padding: 10, borderWidth: 1, borderColor: colors.border },
  ticketVal: { fontSize: 14, fontWeight: '700', color: colors.text },
  ticketCode: { fontSize: 12, color: colors.muted, fontFamily: 'monospace' },
  ticketTotals: { flexDirection: 'row', justifyContent: 'space-between' },
  ticketTotalTxt: { fontSize: 14, color: colors.text },
  warn: { fontSize: 12, color: colors.danger, fontWeight: '600' },
  remainderRow: { flexDirection: 'row', alignItems: 'center', gap: 8, flexWrap: 'wrap' },
  remBtn: { paddingVertical: 8, paddingHorizontal: 16, borderRadius: 10, borderWidth: 1, borderColor: colors.border },
  totalRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginTop: 4 },
  totalLabel: { fontSize: 15, color: colors.muted },
  totalValue: { fontSize: 20, fontWeight: '800', color: colors.text },
  note: { fontSize: 12, color: colors.muted, fontStyle: 'italic' },
  backdrop: { flex: 1, backgroundColor: 'rgba(0,0,0,0.4)', justifyContent: 'flex-end' },
  sheet: { backgroundColor: '#fff', borderTopLeftRadius: 20, borderTopRightRadius: 20, padding: 20, gap: 6 },
  sheetTitle: { fontSize: 18, fontWeight: '800', color: colors.text },
  chips: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },
  chip: { paddingVertical: 8, paddingHorizontal: 16, borderRadius: 10, borderWidth: 1, borderColor: colors.border },
});
