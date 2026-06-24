import { View, Text, StyleSheet, ScrollView } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useNavigation } from '@react-navigation/native';
import { AppButton } from '../components';
import { colors } from '../theme';
import { formatMoney, formatDate } from '../utils/format';

export default function ReceiptScreen({ route }) {
  const navigation = useNavigation();
  const sale = route.params?.sale;

  if (!sale) {
    return (
      <SafeAreaView style={styles.safe} edges={['bottom']}>
        <Text style={styles.muted}>Aucun reçu.</Text>
      </SafeAreaView>
    );
  }

  const newSale = () => {
    // Retour à l'onglet Scanner pour la prochaine vente.
    navigation.navigate('Main', { screen: 'Scanner' });
  };

  return (
    <SafeAreaView style={styles.safe} edges={['bottom']}>
      <ScrollView contentContainerStyle={{ padding: 16 }}>
        <View style={styles.successBox}>
          <Text style={styles.successIcon}>✅</Text>
          <Text style={styles.successText}>Vente enregistrée</Text>
          <Text style={styles.saleId}>Reçu #{sale.id}</Text>
        </View>

        <View style={styles.card}>
          <Row label="Date" value={formatDate(sale.saleDate)} />
          <Row label="Vendeur" value={sale.sellerName} />
          <Row label="Paiement" value={sale.paymentMethod} />
        </View>

        <View style={styles.card}>
          {sale.items.map((it) => (
            <View key={it.id} style={styles.itemRow}>
              <View style={{ flex: 1 }}>
                <Text style={styles.itemName}>{it.productName}</Text>
                <Text style={styles.itemSub}>
                  {it.quantity} × {formatMoney(it.unitPrice)}
                </Text>
              </View>
              <Text style={styles.itemTotal}>{formatMoney(it.totalPrice)}</Text>
            </View>
          ))}

          <View style={styles.divider} />
          <Row label="Sous-total" value={formatMoney(sale.subtotal)} />
          <Row label="Remise" value={`- ${formatMoney(sale.discount)}`} danger />
          <View style={styles.totalRow}>
            <Text style={styles.totalLabel}>Total payé</Text>
            <Text style={styles.totalValue}>{formatMoney(sale.totalAmount)}</Text>
          </View>
          <Text style={styles.serverNote}>Montants calculés et renvoyés par le serveur.</Text>
        </View>

        <AppButton title="Nouvelle vente" onPress={newSale} />
      </ScrollView>
    </SafeAreaView>
  );
}

function Row({ label, value, danger }) {
  return (
    <View style={styles.row}>
      <Text style={styles.rowLabel}>{label}</Text>
      <Text style={[styles.rowValue, danger && { color: colors.danger }]}>{value}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: colors.bg },
  muted: { color: colors.muted, textAlign: 'center', marginTop: 30 },
  successBox: { alignItems: 'center', marginBottom: 16 },
  successIcon: { fontSize: 44 },
  successText: { fontSize: 20, fontWeight: '800', color: colors.success, marginTop: 4 },
  saleId: { color: colors.muted },
  card: { backgroundColor: '#fff', borderRadius: 12, padding: 16, borderWidth: 1, borderColor: colors.border, marginBottom: 14 },
  row: { flexDirection: 'row', justifyContent: 'space-between', paddingVertical: 4 },
  rowLabel: { color: colors.muted },
  rowValue: { color: colors.text, fontWeight: '600' },
  itemRow: { flexDirection: 'row', alignItems: 'center', paddingVertical: 6 },
  itemName: { fontSize: 15, fontWeight: '700', color: colors.text },
  itemSub: { fontSize: 13, color: colors.muted },
  itemTotal: { fontSize: 15, fontWeight: '700', color: colors.text },
  divider: { height: 1, backgroundColor: colors.border, marginVertical: 10 },
  totalRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginTop: 6 },
  totalLabel: { fontSize: 17, fontWeight: '800', color: colors.text },
  totalValue: { fontSize: 22, fontWeight: '800', color: colors.success },
  serverNote: { fontSize: 11, color: colors.muted, fontStyle: 'italic', marginTop: 6 },
});
