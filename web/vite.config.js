import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// Serveur de dev sur le port 5173 (autorise par le CORS du backend).
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
  },
});
