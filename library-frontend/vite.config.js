import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// Vite dev sunucusu. Tarayıcı CORS kuralı yüzünden 5173'ten doğrudan
// 8080'e istek atamaz; bu yüzden /api ile başlayan her isteği gateway'e
// (localhost:8080) proxy'liyoruz. Böylece tarayıcı hep kendi origin'iyle konuşur.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})
