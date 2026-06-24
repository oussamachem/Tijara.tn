import { useState, useCallback } from 'react';
import { View, Text, StyleSheet, FlatList, TouchableOpacity, RefreshControl } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useFocusEffect, useNavigation } from '@react-navigation/native';
import { salesApi } from '../api/endpoints';
import { apiError } from '../api/client';
import { ErrorBanner, Loading } from '../components';
import { colors } from '../theme';
import { formatMoney, formatDate } from '../utils/format';

export default function ReturnsScreen() {
  const navigation = useNavigation();
  const [sales, setSales] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const load = useCallback(async () => {
    setError('');
    try {
      // "Mes ventes" : endpoint VENDOR filtré par le token (GET /api/sales/mine).
      const { data } = await salesApi.mine({ size: 50 });
      setSales(data.content);
    } catch (err) {
      setError(apiError(err));
    } finally {
      setLoading(false);
    }
  }, []);

  useFocusEffect(
    useCallback(() => {
      setLoading(true);
      load();
    }, [load])
  );

  if (loading) return <Loading text="Chargement de vos ventes…" />;

  return (
    <SafeAreaView style={styles.safe} edges={['bottom']}>
      <Text style={styles.header}>Sélectionnez la vente à retourner</Text>
      <ErrorBanner message={error} onRetry={load} />
      <FlatList
        data={sales}
        keyExtractor={(s) => String(s.id)}
        contentContainerStyle={{ padding: 16, gap: 10 }}
        refreshControl={<RefreshControl refreshing={false} onRefresh={load} />}
        renderItem={({ item }) => (
          <TouchableOpacity
            style={styles.row}
            activeOpacity={0.7}
            onPress={() => navigation.navigate('ReturnSale', { saleId: item.id })}
          >
            <View style={{ flex: 1 }}>
              <Text style={styles.title}>Vente #{item.id}</Text>
              <Text style={styles.sub}>{formatDate(item.saleDate)} · {item.itemCount} article(s)</Text>
            </View>
            <Text style={styles.total}>{formatMoney(item.totalAmount)}</Text>
          </TouchableOpacity>
        )}
        ListEmptyComponent={<Text style={styles.empty}>Aucune vente à votre nom.</Text>}
      />
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: colors.bg },
  header: { fontSize: 15, color: colors.muted, paddingHorizontal: 16, paddingTop: 14 },
  row: { flexDirection: 'row', backgroundColor: '#fff', borderRadius: 12, padding: 16, borderWidth: 1, borderColor: colors.border, alignItems: 'center' },
  title: { fontSize: 16, fontWeight: '700', color: colors.text },
  sub: { fontSize: 13, color: colors.muted },
  total: { fontSize: 16, fontWeight: '700', color: colors.primary },
  empty: { textAlign: 'center', color: colors.muted, marginTop: 30 },
});
