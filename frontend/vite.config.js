import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import Components from 'unplugin-vue-components/vite'
import { VantResolver } from '@vant/auto-import-resolver'
import { fileURLToPath, URL } from 'node:url'
import fs from 'node:fs'

const keyPath = fileURLToPath(new URL('./key.pem', import.meta.url))
const certPath = fileURLToPath(new URL('./cert.pem', import.meta.url))

export default defineConfig({
  plugins: [
    vue(),
    // Auto-import Vant components — no manual imports needed
    Components({
      resolvers: [VantResolver()]
    })
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    https: fs.existsSync(keyPath) ? { key: keyPath, cert: certPath } : false,
    port: 5173,
    proxy: {
      '/api': {
        target: 'https://localhost:8080',
        changeOrigin: true,
        secure: false
      },
      '/uploads': {
        target: 'https://localhost:8080',
        changeOrigin: true,
        secure: false
      }
    }
  },
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: ['./src/__tests__/setup.js']
  }
})
