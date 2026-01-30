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
  // 🚀 [추가] 브라우저에 없는 global 변수를 window로 연결하여 에러를 해결합니다.
  define: {
    global: 'window',
  },
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src'),
    },
  },
})