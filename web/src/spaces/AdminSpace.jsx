import { Routes, Route, Navigate } from 'react-router-dom';
import Layout from '../components/Layout.jsx';
import OwnerCaisse from './OwnerCaisse.jsx';
import Dashboard from '../pages/Dashboard.jsx';
import Products from '../pages/Products.jsx';
import Categories from '../pages/Categories.jsx';
import Colors from '../pages/Colors.jsx';
import Sizes from '../pages/Sizes.jsx';
import Sellers from '../pages/Sellers.jsx';
import SalesHistory from '../pages/SalesHistory.jsx';
import SaleDetail from '../pages/SaleDetail.jsx';
import Orders from '../pages/Orders.jsx';
import Credits from '../pages/Credits.jsx';
import CreditDetail from '../pages/CreditDetail.jsx';
import SupplierDebts from '../pages/SupplierDebts.jsx';
import DebtDetail from '../pages/DebtDetail.jsx';

/** Espace PROPRIÉTAIRE (OWNER) : back-office complet de la boutique active (X-Shop-Id). */
export default function AdminSpace() {
  return (
    <Routes>
      {/* Caisse propriétaire : plein écran, hors sidebar admin. */}
      <Route path="caisse/*" element={<OwnerCaisse />} />
      <Route element={<Layout />}>
        <Route index element={<Dashboard />} />
        <Route path="produits" element={<Products />} />
        <Route path="categories" element={<Categories />} />
        <Route path="couleurs" element={<Colors />} />
        <Route path="tailles" element={<Sizes />} />
        <Route path="vendeurs" element={<Sellers />} />
        <Route path="commandes" element={<Orders />} />
        <Route path="historique" element={<SalesHistory />} />
        <Route path="ventes/:id" element={<SaleDetail />} />
        <Route path="credits" element={<Credits />} />
        <Route path="credits/:id" element={<CreditDetail />} />
        <Route path="dettes" element={<SupplierDebts />} />
        <Route path="dettes/:id" element={<DebtDetail />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Route>
    </Routes>
  );
}
