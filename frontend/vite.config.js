import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// vite-plugin-javascript-obfuscator 在 npm run build 时混淆输出产物
// 安装：npm install --save-dev javascript-obfuscator vite-plugin-javascript-obfuscator
let obfuscatorPlugin = null
try {
  const { default: jso } = await import('vite-plugin-javascript-obfuscator')
  obfuscatorPlugin = jso({
    apply: 'build',   // 仅生产构建生效，dev 模式不影响
    options: {
      // --- 安全参数（不破坏 React）---
      compact: true,
      identifierNamesGenerator: 'mangled',   // 变量名压缩
      stringArray: true,                     // 字符串提取到数组
      stringArrayRotate: true,
      stringArrayShuffle: true,
      stringArrayEncoding: ['base64'],       // 字符串 base64 编码
      rotateStringArray: true,
      splitStrings: true,
      splitStringsChunkLength: 8,
      // --- 不开启 controlFlowFlattening / deadCodeInjection ---
      // 这两个选项会破坏 React 的 hooks 调用顺序规则，不要开
      controlFlowFlattening: false,
      deadCodeInjection: false,
    },
  })
} catch {
  // 插件未安装时静默跳过（不影响 dev / CI）
}

export default defineConfig({
  plugins: [react(), ...(obfuscatorPlugin ? [obfuscatorPlugin] : [])],
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:8888',
        changeOrigin: true,
      },
      '/mobile-upload': {
        target: 'http://localhost:8888',
        changeOrigin: true,
      },
      '/ws': {
        target: 'ws://localhost:8888',
        ws: true,
      },
    },
  },
})
