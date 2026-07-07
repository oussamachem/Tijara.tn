import { Routes, Route, Navigate } from 'react-router-dom';
import ProtectedRoute from './components/ProtectedRoute.jsx';
import Layout from './components/Layout.jsx';
import Login from './pages/Login.jsx';
import ForgotPassword from './pages/ForgotPassword.jsx';
import ResetPassword from './pages/ResetPassword.jsx';
import Dashboard from './pages/Dashboard.jsx';
import Products from './pages/Products.jsx';
import Categories from './pages/Categories.jsx';
import Colors from './pages/Colors.jsx';
import Sizes from './pages/Sizes.jsx';
import Sellers from './pages/Sellers.jsx';
import SalesHistory from './pages/SalesHistory.jsx';
import SaleDetail from './pages/SaleDetail.jsx';
import Orders from './pages/Orders.jsx';
import Credits from './pages/Credits.jsx';
import CreditDetail from './pages/CreditDetail.jsx';
import SupplierDebts from './pages/SupplierDebts.jsx';
import DebtDetail from './pages/DebtDetail.jsx';

export default function App() {
  return (
    <Routes>
      {/* Routes publiques */}
      <Route path="/login" element={<Login />} />
      <Route path="/forgot-password" element={<ForgotPassword />} />
      <Route path="/reset-password" element={<ResetPassword />} />

      {/* Routes protégées (admin) */}
      <Route
        element={
          <ProtectedRoute>
            <Layout />
          </ProtectedRoute>
        }
      >
        <Route path="/" element={<Dashboard />} />
        <Route path="/produits" element={<Products />} />
        <Route path="/categories" element={<Categories />} />
        <Route path="/couleurs" element={<Colors />} />
        <Route path="/tailles" element={<Sizes />} />
        <Route path="/vendeurs" element={<Sellers />} />
        <Route path="/commandes" element={<Orders />} />
        <Route path="/historique" element={<SalesHistory />} />
        <Route path="/ventes/:id" element={<SaleDetail />} />
        <Route path="/credits" element={<Credits />} />
        <Route path="/credits/:id" element={<CreditDetail />} />
        <Route path="/dettes" element={<SupplierDebts />} />
        <Route path="/dettes/:id" element={<DebtDetail />} />
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
