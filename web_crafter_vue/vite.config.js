import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

export default defineConfig({
  plugins: [vue()],
   server: {
    host: true,      // ← 중요 (0.0.0.0 의미)
    port: 5173,
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true,
      },
  },
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src'),
    },
  },
})