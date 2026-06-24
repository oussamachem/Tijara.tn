import { useState } from 'react';
import { View, Text, StyleSheet, FlatList, TouchableOpacity } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { productsApi } from '../api/endpoints';
import { apiError } from '../api/client';
import { AppButton, AppTextInput, Badge, ErrorBanner } from '../components';
import AddToCartSheet from '../widgets/AddToCartSheet';
import { colors } from '../theme';
import { formatMoney } from '../utils/format';

export default function ManualSaleScreen() {
  const [query, setQuery] = useState('');
  const [results, setResults] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [searched, setSearched] = useState(false);
  const [product, setProduct] = useState(null);
  const [sheetVisible, setSheetVisible] = useState(false);

  const search = async () => {
    const q = query.trim();
    if (!q) return;
    setLoading(true);
    setError('');
    setSearched(true);
    try {
      // Recherche par nom, puis repli par référence (le backend filtre nom/réf séparément).
      const byName = await productsApi.search({ name: q, size: 20 });
      let content = byName.data.content;
      if (content.length === 0) {
        const byRef = await productsApi.search({ reference: q, size: 20 });
        content = byRef.data.content;
      }
      setResults(content);
    } catch (err) {
      setError(apiError(err));
    } finally {
      setLoading(false);
    }
  };

  const openProduct = (p) => {
    setProduct(p);
    setSheetVisible(true);
  };

  return (
    <SafeAreaView style={styles.safe} edges={['bottom']}>
      <View style={styles.searchBar}>
        <AppTextInput
          style={{ flex: 1 }}
          value={query}
          onChangeText={setQuery}
          placeholder="Nom ou référence du produit"
          autoCapitalize="none"
          returnKeyType="search"
          onSubmitEditing={search}
        />
        <AppButton title="Chercher" onPress={search} loading={loading} style={{ paddingHorizontal: 18 }} />
      </View>

      <ErrorBanner message={error} onRetry={search} />

      <FlatList
        data={results}
        keyExtractor={(item) => String(item.id)}
        contentContainerStyle={{ padding: 16, gap: 10 }}
        renderItem={({ item }) => (
          <TouchableOpacity style={styles.row} onPress={() => openProduct(item)} activeOpacity={0.7}>
            <View style={{ flex: 1 }}>
              <Text style={styles.name}>{item.name}</Text>
              <Text style={styles.ref}>{item.reference}</Text>
            </View>
            <View style={{ alignItems: 'flex-end', gap: 4 }}>
              <Text style={styles.price}>{formatMoney(item.salePrice)}</Text>
              {item.quantity <= 0 ? (
                <Badge text="Rupture" color={colors.danger} />
              ) : (
                <Badge text={`Stock ${item.quantity}`} color={item.lowStock ? colors.amber : colors.success} />
              )}
            </View>
          </TouchableOpacity>
        )}
        ListEmptyComponent={
          !loading && searched ? <Text style={styles.empty}>Aucun produit trouvé.</Text> : null
        }
      />

      <AddToCartSheet
        product={product}
        visible={sheetVisible}
        onClose={() => {
          setSheetVisible(false);
          setProduct(null);
        }}
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
  price: { fontSize: 16, fontWeight: '700', color: colors.primary },
  empty: { textAlign: 'center', color: colors.muted, marginTop: 30 },
});
