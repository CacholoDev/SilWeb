/* global process */
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

const backendTarget = process.env.VITE_BACKEND_URL || 'http://localhost:8080'

export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    port: 5173,
    strictPort: true,
    proxy: {
      '/api': { target: backendTarget, changeOrigin: true },
      '/actuator': { target: backendTarget, changeOrigin: true },
      '/v3/api-docs': { target: backendTarget, changeOrigin: true },
      '/swagger-ui': { target: backendTarget, changeOrigin: true },
    },
  },
})