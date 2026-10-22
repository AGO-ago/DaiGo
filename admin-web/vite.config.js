import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

/**
 * 开发期通过 Vite 代理转发 /api 到后端，避免跨域；
 * 生产环境由 Nginx 同源反代 /api。
 */
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    host: '0.0.0.0',
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
