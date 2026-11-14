import react from '@vitejs/plugin-react';
import { defineConfig } from 'vite';

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    // Listen on the network too, so phones on the same Wi-Fi can open the app
    host: true,
    // Allow the temporary public demo link (Cloudflare quick tunnel)
    allowedHosts: ['.trycloudflare.com'],
    // During development, the browser calls /api/... on the frontend address and
    // Vite forwards it to the Spring Boot backend. One address = no CORS problems,
    // and it works the same on localhost, on a phone, or through a tunnel.
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        configure: (proxy) => {
          // The backend sees the request as coming from Vite itself, not a foreign site
          proxy.on('proxyReq', (proxyReq) => proxyReq.removeHeader('origin'));
        },
      },
      // Live price updates (WebSocket) go through the same address
      '/ws': {
        target: 'ws://localhost:8080',
        ws: true,
        configure: (proxy) => {
          proxy.on('proxyReqWs', (proxyReq) => proxyReq.removeHeader('origin'));
        },
      },
    },
  },
});
