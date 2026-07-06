import { Text } from 'react-native';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { useCart } from '../cart/CartContext';
import { colors } from '../theme';
import ScanScreen from '../screens/ScanScreen';
import ManualSaleScreen from '../screens/ManualSaleScreen';
import CartScreen from '../screens/CartScreen';
import StockScreen from '../screens/StockScreen';
import ReturnsScreen from '../screens/ReturnsScreen';
import ReservationsScreen from '../screens/ReservationsScreen';

const Tab = createBottomTabNavigator();

const tabIcon = (emoji) => ({ color }) => <Text style={{ fontSize: 18, color }}>{emoji}</Text>;

export default function MainTabs() {
  const { count } = useCart();
  return (
    <Tab.Navigator
      screenOptions={{
        headerStyle: { backgroundColor: colors.dark },
        headerTintColor: '#fff',
        tabBarActiveTintColor: colors.primary,
        tabBarInactiveTintColor: colors.muted,
      }}
    >
      <Tab.Screen name="Scanner" component={ScanScreen} options={{ tabBarIcon: tabIcon('🔳') }} />
      <Tab.Screen name="Recherche" component={ManualSaleScreen} options={{ tabBarIcon: tabIcon('🔍') }} />
      <Tab.Screen
        name="Panier"
        component={CartScreen}
        options={{ tabBarIcon: tabIcon('🛒'), tabBarBadge: count > 0 ? count : undefined }}
      />
      <Tab.Screen name="Réservations" component={ReservationsScreen} options={{ tabBarIcon: tabIcon('📅') }} />
      <Tab.Screen name="Stock" component={StockScreen} options={{ tabBarIcon: tabIcon('📦') }} />
      <Tab.Screen name="Retours" component={ReturnsScreen} options={{ tabBarIcon: tabIcon('↩️') }} />
    </Tab.Navigator>
  );
}
