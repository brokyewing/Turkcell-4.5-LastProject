import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// /api -> gateway (docker compose override portu 18090). JWT güvenliği açık;
// token'ı uygulama header'da gönderir.
export default defineConfig({
  // Yönetim paneliyle aynı desen: alt yol (ör. /demos/kitaplik/) için
  // VITE_BASE verilir; verilmezse '/' → mevcut davranış değişmez.
  base: process.env.VITE_BASE || '/',
  plugins: [react()],
  server: {
    port: 5174,
    proxy: { '/api': 'http://localhost:18090' },
  },
})
