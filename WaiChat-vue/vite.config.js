// vite.config.js
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    proxy: {
      // 配置代理规则：将 /api 开头的请求转发到后端
      '/api': {
        target: 'http://localhost:8080', // 后端接口基础路径
        changeOrigin: true, // 改变请求源（让后端认为请求来自后端自身端口）
        rewrite: (path) => path.replace(/^\/api/, '') // 去掉 /api 前缀（后端接口没有 /api）
      }
    }
  }
})