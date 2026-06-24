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
            <View style={{ flex: 1 }}>
              <Text style={styles.name}>{item.name}</Text>
              <Text style={styles.ref}>{item.reference} · {formatMoney(item.salePrice)}</Text>
            </View>
            <View style={{ alignItems: 'flex-end' }}>
              <Text style={styles.qty}>{item.quantity}</Text>
              {item.quantity <= 0 ? (
                <Badge text="Rupture" color={colors.danger} />
              ) : item.lowStock ? (
                <Badge text="Sous seuil" color={colors.amber} />
              ) : (
                <Badge text="OK" color={colors.success} />
              )}
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
  row: { flexDirection: 'row', backgroundColor: '#fff', borderRadius: 12, padding: 14, borderWidth: 1, borderColor: colors.border, alignItems: 'center' },
  name: { fontSize: 16, fontWeight: '700', color: colors.text },
  ref: { fontSize: 13, color: colors.muted },
  qty: { fontSize: 20, fontWeight: '800', color: colors.text },
  empty: { textAlign: 'center', color: colors.muted, marginTop: 30 },
});
