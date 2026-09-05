import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import App from './App.jsx';
import { AuthProvider } from './auth/AuthContext.jsx';
import { CartProvider } from './cart/CartContext.jsx';
import { initKeycloak, keycloak } from './auth/keycloak.js';
import './index.css';

// Init Keycloak (SSO check silencieux) AVANT le premier rendu : l'app connaît d'emblée l'état
// d'authentification. check-sso ne bloque pas : un visiteur anonyme démarre sur la marketplace.
initKeycloak().then(() => {
  keycloak.onTokenExpired = () => keycloak.updateToken(30).catch(() => {});

  ReactDOM.createRoot(document.getElementById('root')).render(
    <React.StrictMode>
      <BrowserRouter>
        <AuthProvider>
          <CartProvider>
            <App />
          </CartProvider>
        </AuthProvider>
      </BrowserRouter>
    </React.StrictMode>
  );
});
