import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// /api → gateway (8080). JWT güvenliği açık; token'ı uygulama header'da gönderir.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5174,
    proxy: { '/api': 'http://localhost:8080' },
  },
})
