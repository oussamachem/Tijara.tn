import { useState } from 'react';
import { View, Text, StyleSheet, TouchableOpacity, Linking, ActivityIndicator } from 'react-native';
import { CameraView, useCameraPermissions } from 'expo-camera';
import { useIsFocused } from '@react-navigation/native';
import { productsApi } from '../api/endpoints';
import { apiError } from '../api/client';
import { AppButton } from '../components';
import AddToCartSheet from '../widgets/AddToCartSheet';
import { colors } from '../theme';

export default function ScanScreen() {
  const [permission, requestPermission] = useCameraPermissions();
  const isFocused = useIsFocused();

  const [scanned, setScanned] = useState(false); // true = scan suspendu (anti-rebond)
  const [resolving, setResolving] = useState(false);
  const [product, setProduct] = useState(null);
  const [sheetVisible, setSheetVisible] = useState(false);
  const [message, setMessage] = useState('');

  // Le callback se déclenche en continu : on suspend dès le 1er code lu.
  const handleBarcode = async ({ data }) => {
    if (scanned || resolving) return;
    setScanned(true);
    setResolving(true);
    setMessage('');
    try {
      // data = la RÉFÉRENCE encodée dans le QR -> résolution serveur.
      const { data: resolved } = await productsApi.byQr(data);
      setProduct(resolved);
      setSheetVisible(true);
    } catch (err) {
      const msg = err?.response?.status === 404 ? `Produit introuvable pour « ${data} »` : apiError(err);
      setMessage(msg);
      // Pas de produit : on laisse l'utilisateur re-scanner via le bouton.
    } finally {
      setResolving(false);
    }
  };

  const rearm = () => {
    setSheetVisible(false);
    setProduct(null);
    setMessage('');
    setScanned(false); // réarme le scan
  };

  // --- Permissions ---
  if (!permission) {
    return (
      <View style={styles.center}>
        <ActivityIndicator color={colors.primary} />
      </View>
    );
  }
  if (!permission.granted) {
    return (
      <View style={styles.center}>
        <Text style={styles.permTitle}>Caméra requise</Text>
        <Text style={styles.permText}>
          L'application a besoin de la caméra pour scanner les QR codes produits.
        </Text>
        <View style={{ height: 16 }} />
        {permission.canAskAgain ? (
          <AppButton title="Autoriser la caméra" onPress={requestPermission} />
        ) : (
          <AppButton title="Ouvrir les réglages" onPress={() => Linking.openSettings()} />
        )}
      </View>
    );
  }

  return (
    <View style={styles.container}>
      {isFocused && (
        <CameraView
          style={StyleSheet.absoluteFill}
          facing="back"
          barcodeScannerSettings={{ barcodeTypes: ['qr'] }}
          onBarcodeScanned={scanned ? undefined : handleBarcode}
        />
      )}

      {/* Cadre de visée */}
      <View style={styles.overlay} pointerEvents="none">
        <View style={styles.frame} />
        <Text style={styles.hint}>Visez le QR code du produit</Text>
      </View>

      {/* Bandeau d'état (résolution / erreur / réarmement) */}
      {(resolving || message || (scanned && !sheetVisible)) && (
        <View style={styles.statusBar}>
          {resolving ? (
            <View style={styles.statusRow}>
              <ActivityIndicator color="#fff" />
              <Text style={styles.statusText}>Recherche du produit…</Text>
            </View>
          ) : (
            <>
              {message ? <Text style={styles.statusText}>{message}</Text> : null}
              {!sheetVisible && (
                <AppButton title="Scanner à nouveau" onPress={rearm} />
              )}
            </>
          )}
        </View>
      )}

      <AddToCartSheet product={product} visible={sheetVisible} onClose={rearm} />
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#000' },
  center: { flex: 1, alignItems: 'center', justifyContent: 'center', padding: 24, backgroundColor: colors.bg },
  permTitle: { fontSize: 18, fontWeight: '700', color: colors.text },
  permText: { textAlign: 'center', color: colors.muted, marginTop: 8 },
  overlay: { ...StyleSheet.absoluteFillObject, alignItems: 'center', justifyContent: 'center' },
  frame: { width: 240, height: 240, borderWidth: 3, borderColor: '#fff', borderRadius: 16, opacity: 0.9 },
  hint: { color: '#fff', marginTop: 16, fontSize: 16, fontWeight: '600' },
  statusBar: { position: 'absolute', left: 16, right: 16, bottom: 24, backgroundColor: 'rgba(15,23,42,0.92)', borderRadius: 12, padding: 16, gap: 10 },
  statusRow: { flexDirection: 'row', alignItems: 'center', gap: 12 },
  statusText: { color: '#fff', fontSize: 15, textAlign: 'center' },
});
