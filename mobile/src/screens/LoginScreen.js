import { useState } from 'react';
import { View, Text, StyleSheet, KeyboardAvoidingView, Platform } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useAuth } from '../auth/AuthContext';
import { apiError } from '../api/client';
import { AppButton, AppTextInput, Label, ErrorBanner } from '../components';
import { colors } from '../theme';

export default function LoginScreen() {
  const { login } = useAuth();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const onSubmit = async () => {
    if (loading) return;
    setError('');
    setLoading(true);
    try {
      await login(email.trim(), password);
      // Succès : RootNavigator bascule automatiquement vers l'app (isAuthenticated).
    } catch (err) {
      // Login raté (401) : l'intercepteur n'intervient PAS sur /api/auth -> message affiché.
      setError(apiError(err, 'Identifiants invalides'));
    } finally {
      setLoading(false);
    }
  };

  return (
    <SafeAreaView style={styles.safe}>
      <KeyboardAvoidingView
        behavior={Platform.OS === 'ios' ? 'padding' : undefined}
        style={styles.container}
      >
        <View style={styles.header}>
          <Text style={styles.logo}>🛍️</Text>
          <Text style={styles.title}>Smart Boutique</Text>
          <Text style={styles.subtitle}>Application vendeur</Text>
        </View>

        <View style={styles.form}>
          <ErrorBanner message={error} />
          <Label>Email</Label>
          <AppTextInput
            value={email}
            onChangeText={setEmail}
            placeholder="vendeur@smartboutique.com"
            autoCapitalize="none"
            keyboardType="email-address"
            autoCorrect={false}
          />
          <View style={{ height: 14 }} />
          <Label>Mot de passe</Label>
          <AppTextInput value={password} onChangeText={setPassword} placeholder="••••••••" secureTextEntry />
          <View style={{ height: 22 }} />
          <AppButton title="Se connecter" onPress={onSubmit} loading={loading} />
        </View>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: colors.bg },
  container: { flex: 1, justifyContent: 'center', padding: 24 },
  header: { alignItems: 'center', marginBottom: 32 },
  logo: { fontSize: 48 },
  title: { fontSize: 24, fontWeight: '800', color: colors.text, marginTop: 8 },
  subtitle: { fontSize: 14, color: colors.muted },
  form: { backgroundColor: '#fff', borderRadius: 16, padding: 20, borderWidth: 1, borderColor: colors.border },
});
