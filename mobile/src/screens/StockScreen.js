import { useState, useCallback } from 'react';
import { View, Text, StyleSheet, FlatList, RefreshControl } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useFocusEffect } from '@react-navigation/native';
import { productsApi } from '../api/endpoints';
import { apiError } from '../api/client';
import { AppButton, AppTextInput, Badge, ErrorBanner, Loading } from '../components';
import { colors } from '../theme';
import { formatMoney } from '../utils/format';

export default function StockScreen() {
  const [query, setQuery] = useState('');
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const load = useCallback(async (q = '') => {
    setError('');
    try {
      const params = { size: 50, sort: 'name,asc' };
      if (q.trim()) params.name = q.trim();
      const { data } = await productsApi.search(params);
      setItems(data.content);
    } catch (err) {
      setError(apiError(err));
    } finally {
      setLoading(false);
    }
  }, []);

  // Recharge à chaque focus de l'onglet (stock à jour).
  useFocusEffect(
    useCallback(() => {
      setLoading(true);
      load(query);
      // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [load])
  );

  if (loading) return <Loading text="Chargement du stock…" />;

  return (
    <SafeAreaView style={styles.safe} edges={['bottom']}>
      <View style={styles.searchBar}>
        <AppTextInput
          style={{ flex: 1 }}
          value={query}
          onChangeText={setQuery}
          placeholder="Filtrer par nom"
          autoCapitalize="none"
          returnKeyType="search"
          onSubmitEditing={() => load(query)}
        />
        <AppButton title="Filtrer" onPress={() => load(query)} style={{ paddingHorizontal: 18 }} />
      </View>

      <ErrorBanner message={error} onRetry={() => load(query)} />

      <FlatList
        data={items}
        keyExtractor={(item) => String(item.id)}
        contentContainerStyle={{ padding: 16, gap: 10 }}
        refreshControl={<RefreshControl refreshing={false} onRefresh={() => load(query)} />}
        renderItem={({ item }) => (
          <View style={styles.row}>
            <View style={styles.head}>
              <View style={{ flex: 1 }}>
                <Text style={styles.name}>{item.name}</Text>
                <Text style={styles.ref}>{item.reference} · {formatMoney(item.salePrice)}</Text>
              </View>
              <View style={{ alignItems: 'flex-end' }}>
                <Text style={styles.qty}>{item.totalStock}</Text>
                <Text style={styles.qtyLabel}>total</Text>
              </View>
            </View>
            <View style={styles.variants}>
              {item.variants.map((v) => (
                <View key={v.id} style={styles.vChip}>
                  <View style={[styles.swatch, { backgroundColor: v.colorHex || '#fff' }]} />
                  <Text style={styles.vText}>{v.colorName} · {v.size}</Text>
                  <Text style={[styles.vQty, v.quantity === 0 && { color: colors.danger }, v.lowStock && v.quantity > 0 && { color: colors.amber }]}>
                    {v.quantity}
                  </Text>
                </View>
              ))}
            </View>
          </View>
        )}
        ListEmptyComponent={<Text style={styles.empty}>Aucun produit.</Text>}
      />
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: colors.bg },
  searchBar: { flexDirection: 'row', gap: 10, padding: 16, alignItems: 'center' },
  row: { backgroundColor: '#fff', borderRadius: 12, padding: 14, borderWidth: 1, borderColor: colors.border, gap: 8 },
  head: { flexDirection: 'row', alignItems: 'center' },
  name: { fontSize: 16, fontWeight: '700', color: colors.text },
  ref: { fontSize: 13, color: colors.muted },
  qty: { fontSize: 20, fontWeight: '800', color: colors.text },
  qtyLabel: { fontSize: 11, color: colors.muted },
  variants: { flexDirection: 'row', flexWrap: 'wrap', gap: 6 },
  vChip: { flexDirection: 'row', alignItems: 'center', gap: 5, backgroundColor: colors.bg, borderRadius: 8, paddingHorizontal: 8, paddingVertical: 4 },
  swatch: { width: 12, height: 12, borderRadius: 6, borderWidth: 1, borderColor: colors.border },
  vText: { fontSize: 12, color: colors.text },
  vQty: { fontSize: 12, fontWeight: '700', color: colors.success },
  empty: { textAlign: 'center', color: colors.muted, marginTop: 30 },
});
