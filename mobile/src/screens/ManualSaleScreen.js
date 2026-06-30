import { useState } from 'react';
import { View, Text, StyleSheet, FlatList, TouchableOpacity, Modal, ScrollView } from 'react-native';
import { SafeAreaView, useSafeAreaInsets } from 'react-native-safe-area-context';
import { productsApi } from '../api/endpoints';
import { apiError } from '../api/client';
import { AppButton, AppTextInput, Badge, ErrorBanner } from '../components';
import AddToCartSheet from '../widgets/AddToCartSheet';
import { colors } from '../theme';
import { formatMoney } from '../utils/format';

// Normalise (produit + variante) -> ligne de panier.
const toLine = (p, v) => ({
  variantId: v.id, variantReference: v.reference, productName: p.name, salePrice: p.salePrice,
  colorName: v.colorName, size: v.size, quantity: v.quantity, seuilAlerte: v.seuilAlerte,
});

export default function ManualSaleScreen() {
  const insets = useSafeAreaInsets();
  const [query, setQuery] = useState('');
  const [results, setResults] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [searched, setSearched] = useState(false);

  const [picker, setPicker] = useState(null);   // produit dont on choisit la variante
  const [variant, setVariant] = useState(null);  // variante choisie -> AddToCartSheet
  const [sheetVisible, setSheetVisible] = useState(false);

  const search = async () => {
    const q = query.trim();
    if (!q) return;
    setLoading(true); setError(''); setSearched(true);
    try {
      const byName = await productsApi.search({ name: q, size: 20 });
      let content = byName.data.content;
      if (content.length === 0) content = (await productsApi.search({ reference: q, size: 20 })).data.content;
      setResults(content);
    } catch (err) { setError(apiError(err)); }
    finally { setLoading(false); }
  };

  const pickVariant = (v) => {
    setVariant(toLine(picker, v));
    setPicker(null);
    setSheetVisible(true);
  };

  return (
    <SafeAreaView style={styles.safe} edges={['bottom']}>
      <View style={styles.searchBar}>
        <AppTextInput style={{ flex: 1 }} value={query} onChangeText={setQuery}
          placeholder="Nom ou référence du produit" autoCapitalize="none" returnKeyType="search" onSubmitEditing={search} />
        <AppButton title="Chercher" onPress={search} loading={loading} style={{ paddingHorizontal: 18 }} />
      </View>

      <ErrorBanner message={error} onRetry={search} />

      <FlatList
        data={results}
        keyExtractor={(item) => String(item.id)}
        keyboardShouldPersistTaps="handled"
        contentContainerStyle={{ padding: 16, gap: 10 }}
        renderItem={({ item }) => (
          <TouchableOpacity style={styles.row} onPress={() => setPicker(item)} activeOpacity={0.7}>
            <View style={{ flex: 1 }}>
              <Text style={styles.name}>{item.name}</Text>
              <Text style={styles.ref}>{item.reference} · {item.variants.length} variante(s)</Text>
            </View>
            <View style={{ alignItems: 'flex-end', gap: 4 }}>
              <Text style={styles.price}>{formatMoney(item.salePrice)}</Text>
              <Badge text={`Stock ${item.totalStock}`} color={item.lowStock ? colors.amber : colors.success} />
            </View>
          </TouchableOpacity>
        )}
        ListEmptyComponent={!loading && searched ? <Text style={styles.empty}>Aucun produit trouvé.</Text> : null}
      />

      {/* Sélecteur de variante */}
      <Modal visible={!!picker} transparent animationType="slide" onRequestClose={() => setPicker(null)}>
        <TouchableOpacity style={styles.backdrop} activeOpacity={1} onPress={() => setPicker(null)}>
          <TouchableOpacity activeOpacity={1} style={[styles.sheet, { paddingBottom: insets.bottom + 16 }]}>
            <Text style={styles.pickerTitle}>{picker?.name}</Text>
            <Text style={styles.pickerSub}>Choisissez la déclinaison</Text>
            {/* Scrollable : un produit peut avoir beaucoup de variantes sur petit écran. */}
            <ScrollView style={{ maxHeight: 360 }} contentContainerStyle={{ gap: 8 }}>
              {picker?.variants.map((v) => (
                <TouchableOpacity key={v.id} style={styles.variantRow} onPress={() => pickVariant(v)}
                  disabled={v.quantity <= 0} activeOpacity={0.7}>
                  <View style={[styles.swatch, { backgroundColor: v.colorHex || '#fff' }]} />
                  <Text style={styles.variantText}>{v.colorName} · {v.size}</Text>
                  {v.quantity <= 0
                    ? <Badge text="Rupture" color={colors.danger} />
                    : <Badge text={`Stock ${v.quantity}`} color={colors.success} />}
                </TouchableOpacity>
              ))}
            </ScrollView>
            <View style={{ height: 6 }} />
            <AppButton title="Fermer" variant="secondary" onPress={() => setPicker(null)} />
          </TouchableOpacity>
        </TouchableOpacity>
      </Modal>

      <AddToCartSheet variant={variant} visible={sheetVisible} onClose={() => { setSheetVisible(false); setVariant(null); }} />
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
  backdrop: { flex: 1, backgroundColor: 'rgba(0,0,0,0.4)', justifyContent: 'flex-end' },
  sheet: { backgroundColor: '#fff', borderTopLeftRadius: 20, borderTopRightRadius: 20, padding: 20, gap: 8 },
  pickerTitle: { fontSize: 18, fontWeight: '800', color: colors.text },
  pickerSub: { fontSize: 13, color: colors.muted, marginBottom: 6 },
  variantRow: { flexDirection: 'row', alignItems: 'center', gap: 10, borderWidth: 1, borderColor: colors.border, borderRadius: 10, padding: 12 },
  swatch: { width: 18, height: 18, borderRadius: 9, borderWidth: 1, borderColor: colors.border },
  variantText: { flex: 1, fontSize: 15, fontWeight: '600', color: colors.text },
});
