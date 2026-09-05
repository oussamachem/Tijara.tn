import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import App from './App.jsx';
import { AuthProvider } from './context/AuthContext.jsx';
import { ShopProvider } from './context/ShopContext.jsx';
import { initKeycloak, keycloak } from './auth/keycloak.js';
import './index.css';

// Enregistrement du service worker (PWA installable) — best-effort, hors dev.
if ('serviceWorker' in navigator && import.meta.env.PROD) {
  window.addEventListener('load', () => {
    navigator.serviceWorker.register('/sw.js').catch(() => {});
  });
}

// On initialise Keycloak (SSO check silencieux) AVANT le premier rendu : l'app connaît d'emblée
// l'état d'authentification (évite un flash « déconnecté » puis « connecté »). check-sso ne bloque
// pas : un visiteur anonyme démarre normalement sur la marketplace.
initKeycloak().then(() => {
  // Filet de sécurité : si le token expire, on tente un refresh silencieux (l'intercepteur API le
  // fait aussi avant chaque requête, mais ceci couvre les longues sessions inactives).
  keycloak.onTokenExpired = () => keycloak.updateToken(30).catch(() => {});

  ReactDOM.createRoot(document.getElementById('root')).render(
    <React.StrictMode>
      <BrowserRouter>
        <AuthProvider>
          <ShopProvider>
            <App />
          </ShopProvider>
        </AuthProvider>
      </BrowserRouter>
    </React.StrictMode>
  );
});
