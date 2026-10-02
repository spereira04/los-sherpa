import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// En desarrollo el SPA corre en 5173 y las llamadas a /api se proxean al backend en 8080,
// asi que el navegador ve un mismo origen y la cookie de sesion viaja sin tocar CORS.
// En "produccion" local no hay proxy: Spring Boot sirve el build desde su propio origen.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: false,
      },
    },
  },
  build: {
    outDir: 'dist',
    sourcemap: false,
  },
})
