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
    },
  },
})
