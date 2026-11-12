import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// Vite dev sunucusu. Tarayıcı CORS kuralı yüzünden 5173'ten doğrudan
// gateway'e istek atamaz; bu yüzden /api ile başlayan her isteği gateway'e
// (docker compose override portu 18090) proxy'liyoruz.
//
// base: yerelde '/' (http://localhost:5173/), üretimde yönetim paneli ana
// uygulamanın altında tek origin'de yaşar → VITE_BASE=/admin/.
// Tek domain = tek TLS sertifikası + CORS yok.
export default defineConfig({
  base: process.env.VITE_BASE || '/',
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:18090',
    },
  },
})
