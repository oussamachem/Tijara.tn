import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { useAuth } from '../auth/AuthContext';
import { Loading } from '../components';
import { colors } from '../theme';
import LoginScreen from '../screens/LoginScreen';
import MainTabs from './MainTabs';
import ReceiptScreen from '../screens/ReceiptScreen';
import ReturnSaleScreen from '../screens/ReturnSaleScreen';
import ReservationDetailScreen from '../screens/ReservationDetailScreen';

const Stack = createNativeStackNavigator();

export default function RootNavigator() {
  const { isAuthenticated, hydrating } = useAuth();

  if (hydrating) return <Loading text="Chargement…" />;

  return (
    <Stack.Navigator
      screenOptions={{
        headerStyle: { backgroundColor: colors.dark },
        headerTintColor: '#fff',
      }}
    >
      {!isAuthenticated ? (
        <Stack.Screen name="Login" component={LoginScreen} options={{ headerShown: false }} />
      ) : (
        <>
          <Stack.Screen name="Main" component={MainTabs} options={{ headerShown: false }} />
          <Stack.Screen
            name="Receipt"
            component={ReceiptScreen}
            options={{ title: 'Reçu', headerBackVisible: false, gestureEnabled: false }}
          />
          <Stack.Screen name="ReturnSale" component={ReturnSaleScreen} options={{ title: 'Retour produit' }} />
          <Stack.Screen name="ReservationDetail" component={ReservationDetailScreen} options={{ title: 'Réservation' }} />
        </>
      )}
    </Stack.Navigator>
  );
}
