import { lazy, Suspense } from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { useAuth } from './context/AuthContext.jsx';
import { useShop } from './context/ShopContext.jsx';
import Login from './pages/Login.jsx';
import Register from './pages/Register.jsx';
import ForgotPassword from './pages/ForgotPassword.jsx';
import ResetPassword from './pages/ResetPassword.jsx';
import Profile from './pages/Profile.jsx';

// Code-split : chaque espace est un bundle séparé, chargé à la demande selon le rôle actif.
// Un client ne télécharge jamais le code de l'admin, ni l'inverse.
const AdminSpace = lazy(() => import('./spaces/AdminSpace.jsx'));
const VendorSpace = lazy(() => import('./spaces/VendorSpace.jsx'));
const ClientSpace = lazy(() => import('./spaces/ClientSpace.jsx'));
const PlatformSpace = lazy(() => import('./spaces/PlatformSpace.jsx'));

function FullScreen({ children }) {
  return (
    <div className="flex h-full items-center justify-center bg-slate-50">
      <div className="flex flex-col items-center gap-3 text-slate-400">
        <span className="h-9 w-9 animate-spin rounded-full border-2 border-slate-200 border-t-brand-600" />
        {children && <span className="text-sm">{children}</span>}
      </div>
    </div>
  );
}

/** Aiguillage role-aware : choisit l'espace selon le contexte actif (ShopContext). */
function Workspace() {
  const { user } = useAuth();
  const { ready, mode, role, activeShopId } = useShop();

  if (!ready) return <FullScreen>Chargement…</FullScreen>;

  let space;
  if (!user) space = <ClientSpace />;                                   // marketplace anonyme (lien partagé)
  else if (mode === 'platform') space = <PlatformSpace />;
  else if (mode === 'shop' && role === 'OWNER') space = <AdminSpace />;
  else if (mode === 'shop' && role === 'VENDOR') space = <VendorSpace />;
  else space = <ClientSpace />;

  // La `key` force le remontage complet au changement de contexte -> état + données repartis à neuf.
  return (
    <Suspense fallback={<FullScreen>Chargement…</FullScreen>}>
      <div className="h-full" key={`${mode}:${activeShopId ?? '-'}`}>
        {space}
      </div>
    </Suspense>
  );
}

function ProfileRoute() {
  const { user } = useAuth();
  return user ? <Profile /> : <Navigate to="/login" replace />;
}

export default function App() {
  return (
    <Routes>
      {/* Public (identité) */}
      <Route path="/login" element={<Login />} />
      <Route path="/register" element={<Register />} />
      <Route path="/forgot-password" element={<ForgotPassword />} />
      <Route path="/reset-password" element={<ResetPassword />} />

      {/* Profil (partagé, tous rôles) — accessible authentifié, hors espaces. */}
      <Route path="/profile" element={<ProfileRoute />} />

      {/* Espace de travail role-aware (chaque espace gère son propre routage interne). */}
      <Route path="/*" element={<Workspace />} />
    </Routes>
  );
}
