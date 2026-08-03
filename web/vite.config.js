import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// Serveur de dev sur le port 5173 (autorise par le CORS du backend).
// Preview : proxifie /api et /uploads vers le backend -> MEME ORIGINE (permet un tunnel HTTPS
// unique : la caméra du navigateur exige un contexte sécurisé).
const API_TARGET = process.env.SB_API_TARGET || 'http://localhost:8089';
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
  },
  preview: {
    // Démo via tunnel (Cloudflare) : autorise les Host externes (sinon Vite renvoie 403).
    allowedHosts: true,
    proxy: {
      '/api': { target: API_TARGET, changeOrigin: true },
      '/uploads': { target: API_TARGET, changeOrigin: true },
    },
  },
});
