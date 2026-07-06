import { useState, useCallback } from 'react';
import { View, Text, StyleSheet, FlatList, TouchableOpacity, RefreshControl } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useFocusEffect, useNavigation } from '@react-navigation/native';
import { reservationsApi } from '../api/endpoints';
import { apiError } from '../api/client';
import { ErrorBanner, Loading, Badge } from '../components';
import { colors } from '../theme';
import { formatMoney } from '../utils/format';

const FILTERS = [
  { key: 'ACTIVE', label: 'En cours' },
  { key: 'COMPLETED', label: 'Soldées' },
  { key: 'EXPIRED', label: 'Expirées' },
  { key: 'CANCELLED', label: 'Annulées' },
  { key: '', label: 'Toutes' },
];

const STATUS_COLOR = {
  ACTIVE: colors.primary,
  COMPLETED: colors.success,
  EXPIRED: colors.danger,
  CANCELLED: colors.muted,
};
const STATUS_LABEL = {
  ACTIVE: 'En cours', COMPLETED: 'Soldée', EXPIRED: 'Expirée', CANCELLED: 'Annulée',
};

const daysLabel = (d) => (d < 0 ? `en retard de ${-d} j` : d === 0 ? "aujourd'hui" : `${d} j restant${d > 1 ? 's' : ''}`);

export default function ReservationsScreen() {
  const navigation = useNavigation();
  const [filter, setFilter] = useState('ACTIVE');
  const [rows, setRows] = useState([]);
  const [dueSoonCount, setDueSoonCount] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const load = useCallback(async () => {
    setError('');
    try {
      const [{ data }, ds] = await Promise.all([
        reservationsApi.list(filter),
        reservationsApi.dueSoon().catch(() => ({ data: [] })),
      ]);
      setRows(data);
      setDueSoonCount(ds.data.length);
    } catch (err) {
      setError(apiError(err));
    } finally {
      setLoading(false);
    }
  }, [filter]);

  useFocusEffect(
    useCallback(() => {
      setLoading(true);
      load();
    }, [load])
  );

  return (
    <SafeAreaView style={styles.safe} edges={['bottom']}>
      {dueSoonCount > 0 && (
        <View style={styles.alert}>
          <Text style={styles.alertText}>
            🔔 {dueSoonCount} réservation{dueSoonCount > 1 ? 's' : ''} à échéance proche — prévenir le/les client(s).
          </Text>
        </View>
      )}

      <View style={styles.filters}>
        {FILTERS.map((f) => (
          <TouchableOpacity
            key={f.key || 'ALL'}
            style={[styles.filterBtn, filter === f.key && styles.filterActive]}
            onPress={() => setFilter(f.key)}
          >
            <Text style={[styles.filterText, filter === f.key && styles.filterTextActive]}>{f.label}</Text>
          </TouchableOpacity>
        ))}
      </View>

      <ErrorBanner message={error} onRetry={load} />

      {loading ? (
        <Loading text="Chargement des réservations…" />
      ) : (
        <FlatList
          data={rows}
          keyExtractor={(r) => String(r.id)}
          contentContainerStyle={{ padding: 16, gap: 10 }}
          refreshControl={<RefreshControl refreshing={false} onRefresh={load} />}
          renderItem={({ item }) => (
            <TouchableOpacity
              style={[styles.row, item.dueSoon && styles.rowDue]}
              activeOpacity={0.7}
              onPress={() => navigation.navigate('ReservationDetail', { id: item.id })}
            >
              <View style={{ flex: 1 }}>
                <View style={styles.rowTop}>
                  <Text style={styles.ref}>{item.reference}</Text>
                  <Badge text={STATUS_LABEL[item.status] || item.status} color={STATUS_COLOR[item.status]} />
                </View>
                <Text style={styles.customer}>{item.customerName}</Text>
                {item.status === 'ACTIVE' && (
                  <Text style={[styles.days, item.dueSoon && { color: colors.danger, fontWeight: '700' }]}>
                    {item.dueSoon ? '⚠️ ' : ''}{daysLabel(item.daysRemaining)}
                  </Text>
                )}
              </View>
              <View style={{ alignItems: 'flex-end' }}>
                <Text style={styles.remainLabel}>Reste</Text>
                <Text style={[styles.remain, { color: Number(item.remaining) > 0 ? colors.text : colors.success }]}>
                  {formatMoney(item.remaining)}
                </Text>
                <Text style={styles.total}>/ {formatMoney(item.total)}</Text>
              </View>
            </TouchableOpacity>
          )}
          ListEmptyComponent={<Text style={styles.empty}>Aucune réservation.</Text>}
        />
      )}
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: colors.bg },
  alert: { backgroundColor: '#fef3c7', paddingHorizontal: 16, paddingVertical: 10, borderBottomWidth: 1, borderBottomColor: '#fde68a' },
  alertText: { color: colors.amber, fontWeight: '700', fontSize: 13 },
  filters: { flexDirection: 'row', flexWrap: 'wrap', gap: 6, paddingHorizontal: 12, paddingTop: 10 },
  filterBtn: { paddingVertical: 6, paddingHorizontal: 12, borderRadius: 999, borderWidth: 1, borderColor: colors.border, backgroundColor: '#fff' },
  filterActive: { backgroundColor: colors.primary, borderColor: colors.primary },
  filterText: { color: colors.text, fontWeight: '600', fontSize: 13 },
  filterTextActive: { color: '#fff' },
  row: { flexDirection: 'row', backgroundColor: '#fff', borderRadius: 12, padding: 14, borderWidth: 1, borderColor: colors.border, alignItems: 'center', gap: 12 },
  rowDue: { borderColor: colors.danger, borderWidth: 1.5 },
  rowTop: { flexDirection: 'row', alignItems: 'center', gap: 8 },
  ref: { fontSize: 15, fontWeight: '800', color: colors.text },
  customer: { fontSize: 14, color: colors.text, marginTop: 2 },
  days: { fontSize: 13, color: colors.muted, marginTop: 2 },
  remainLabel: { fontSize: 11, color: colors.muted },
  remain: { fontSize: 17, fontWeight: '800' },
  total: { fontSize: 12, color: colors.muted },
  empty: { textAlign: 'center', color: colors.muted, marginTop: 30 },
});
