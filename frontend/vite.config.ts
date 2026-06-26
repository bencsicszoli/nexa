import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// A backend API a 8080-as porton fut; a /api hívásokat odairányítjuk.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      // WebSocket/STOMP a valós idejű értesítéshez (#11) — a /ws kézfogás is a 8080-ra megy.
      '/ws': {
        target: 'http://localhost:8080',
        ws: true,
        changeOrigin: true,
      },
    },
  },
})
