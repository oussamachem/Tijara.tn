import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// App CLIENT (marketplace), servie a la racine de son propre nginx (port host 8091).
export default defineConfig({
  plugins: [react()],
  server: { port: 5174 },
});
