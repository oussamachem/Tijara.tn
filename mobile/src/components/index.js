import {
  Text,
  TextInput,
  TouchableOpacity,
  View,
  StyleSheet,
  ActivityIndicator,
} from 'react-native';
import { colors, radius } from '../theme';

export function AppButton({ title, onPress, disabled, loading, variant = 'primary', style }) {
  const bg =
    variant === 'danger' ? colors.danger : variant === 'secondary' ? '#fff' : colors.primary;
  const fg = variant === 'secondary' ? colors.text : '#fff';
  const borderStyle = variant === 'secondary' ? { borderWidth: 1, borderColor: colors.border } : null;
  return (
    <TouchableOpacity
      onPress={onPress}
      disabled={disabled || loading}
      activeOpacity={0.8}
      style={[styles.button, { backgroundColor: bg }, borderStyle, (disabled || loading) && { opacity: 0.5 }, style]}
    >
      {loading ? (
        <ActivityIndicator color={fg} />
      ) : (
        <Text style={[styles.buttonText, { color: fg }]}>{title}</Text>
      )}
    </TouchableOpacity>
  );
}

export function AppTextInput({ style, ...props }) {
  return <TextInput style={[styles.input, style]} placeholderTextColor={colors.muted} {...props} />;
}

export function Label({ children }) {
  return <Text style={styles.label}>{children}</Text>;
}

export function Card({ children, style }) {
  return <View style={[styles.card, style]}>{children}</View>;
}

export function Loading({ text }) {
  return (
    <View style={styles.center}>
      <ActivityIndicator size="large" color={colors.primary} />
      {text ? <Text style={styles.muted}>{text}</Text> : null}
    </View>
  );
}

export function ErrorBanner({ message, onRetry }) {
  if (!message) return null;
  return (
    <View style={styles.errorBanner}>
      <Text style={styles.errorText}>{message}</Text>
      {onRetry ? (
        <TouchableOpacity onPress={onRetry}>
          <Text style={styles.retry}>Réessayer</Text>
        </TouchableOpacity>
      ) : null}
    </View>
  );
}

export function Badge({ text, color = colors.muted }) {
  return (
    <View style={[styles.badge, { backgroundColor: color + '22' }]}>
      <Text style={[styles.badgeText, { color }]}>{text}</Text>
    </View>
  );
}

export function QtyStepper({ value, onChange, max }) {
  const dec = () => onChange(Math.max(1, value - 1));
  const inc = () => onChange(max ? Math.min(max, value + 1) : value + 1);
  return (
    <View style={styles.stepper}>
      <TouchableOpacity style={styles.stepBtn} onPress={dec}>
        <Text style={styles.stepBtnText}>−</Text>
      </TouchableOpacity>
      <Text style={styles.stepValue}>{value}</Text>
      <TouchableOpacity style={styles.stepBtn} onPress={inc}>
        <Text style={styles.stepBtnText}>+</Text>
      </TouchableOpacity>
    </View>
  );
}

export const styles = StyleSheet.create({
  button: { borderRadius: radius, paddingVertical: 14, alignItems: 'center', justifyContent: 'center' },
  buttonText: { fontSize: 16, fontWeight: '600' },
  input: {
    backgroundColor: '#fff',
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: radius,
    paddingHorizontal: 14,
    paddingVertical: 12,
    fontSize: 16,
    color: colors.text,
  },
  label: { fontSize: 14, fontWeight: '600', color: colors.text, marginBottom: 6 },
  card: { backgroundColor: colors.card, borderRadius: radius, padding: 16, borderWidth: 1, borderColor: colors.border },
  center: { flex: 1, alignItems: 'center', justifyContent: 'center', padding: 24 },
  muted: { color: colors.muted, marginTop: 8 },
  errorBanner: {
    backgroundColor: '#fee2e2',
    borderRadius: radius,
    padding: 12,
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  errorText: { color: colors.danger, flex: 1, marginRight: 8 },
  retry: { color: colors.danger, fontWeight: '700' },
  badge: { borderRadius: 999, paddingHorizontal: 10, paddingVertical: 3, alignSelf: 'flex-start' },
  badgeText: { fontSize: 12, fontWeight: '600' },
  stepper: { flexDirection: 'row', alignItems: 'center', gap: 14 },
  stepBtn: {
    width: 38,
    height: 38,
    borderRadius: 8,
    backgroundColor: colors.bg,
    borderWidth: 1,
    borderColor: colors.border,
    alignItems: 'center',
    justifyContent: 'center',
  },
  stepBtnText: { fontSize: 22, color: colors.text, lineHeight: 24 },
  stepValue: { fontSize: 18, fontWeight: '700', minWidth: 28, textAlign: 'center' },
});
