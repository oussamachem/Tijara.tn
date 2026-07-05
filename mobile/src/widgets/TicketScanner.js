import { useState, useEffect } from 'react';
import { Modal, View, Text, StyleSheet, Linking } from 'react-native';
import { CameraView, useCameraPermissions } from 'expo-camera';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { AppButton } from '../components';
import { colors } from '../theme';

/**
 * Scanner un TICKET CADEAU : QR OU code-barres 1D (un cheque habillement porte les deux).
 * On ne decode pas le contenu proprietaire : on renvoie la chaine brute comme identifiant.
 */
export default function TicketScanner({ visible, onClose, onScanned }) {
  const [permission, requestPermission] = useCameraPermissions();
  const insets = useSafeAreaInsets();
  const [scanned, setScanned] = useState(false);

  useEffect(() => { if (visible) setScanned(false); }, [visible]);

  const handle = ({ data }) => {
    if (scanned) return;
    setScanned(true);
    onScanned(String(data || '').trim());
  };

  return (
    <Modal visible={visible} animationType="slide" onRequestClose={onClose}>
      <View style={styles.container}>
        {permission?.granted ? (
          <CameraView
            style={StyleSheet.absoluteFill}
            facing="back"
            barcodeScannerSettings={{
              barcodeTypes: ['qr', 'code128', 'code39', 'ean13', 'ean8', 'upc_a', 'upc_e', 'codabar', 'itf14'],
            }}
            onBarcodeScanned={scanned ? undefined : handle}
          />
        ) : (
          <View style={styles.center}>
            <Text style={styles.permTitle}>Caméra requise</Text>
            <Text style={styles.permText}>Autorisez la caméra pour scanner le ticket.</Text>
            <View style={{ height: 12 }} />
            {permission?.canAskAgain !== false ? (
              <AppButton title="Autoriser" onPress={requestPermission} />
            ) : (
              <AppButton title="Ouvrir les réglages" onPress={() => Linking.openSettings()} />
            )}
          </View>
        )}

        <View style={styles.overlay} pointerEvents="none">
          <View style={styles.frame} />
          <Text style={styles.hint}>Scannez le QR ou le code-barres du ticket</Text>
        </View>

        <View style={[styles.bottom, { paddingBottom: insets.bottom + 12 }]}>
          <AppButton title="Annuler" variant="secondary" onPress={onClose} />
        </View>
      </View>
    </Modal>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#000' },
  center: { flex: 1, alignItems: 'center', justifyContent: 'center', padding: 24, backgroundColor: colors.bg },
  permTitle: { fontSize: 18, fontWeight: '700', color: colors.text },
  permText: { textAlign: 'center', color: colors.muted, marginTop: 8 },
  overlay: { ...StyleSheet.absoluteFillObject, alignItems: 'center', justifyContent: 'center' },
  frame: { width: 260, height: 180, borderWidth: 3, borderColor: '#fff', borderRadius: 16, opacity: 0.9 },
  hint: { color: '#fff', marginTop: 16, fontSize: 15, fontWeight: '600', textAlign: 'center', paddingHorizontal: 24 },
  bottom: { position: 'absolute', left: 16, right: 16, bottom: 0 },
});
