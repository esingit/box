import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { NaiveUiResolver } from 'unplugin-vue-components/resolvers'
import { createSvgIconsPlugin } from 'vite-plugin-svg-icons'
import { viteMockServe } from 'vite-plugin-mock'

export default defineConfig(({ command, mode }) => {
  // 1. 预加载 env
  const env = loadEnv(mode, process.cwd())
  const isProd = mode === 'production'
  const API_BASE = env.VITE_API_BASE_URL
  const APP_PORT = Number(env.VITE_APP_PORT) || 3000
  const APP_TITLE = env.VITE_APP_TITLE

  return {
    // 2. 全局常量注入
    define: {
      __APP_TITLE__: JSON.stringify(APP_TITLE),
    },

    // 3. 别名映射
    resolve: {
      alias: {
        '@': path.resolve(__dirname, 'src'),
        'components': path.resolve(__dirname, 'src/components'),
      },
    },

    // 4. 本地开发服务器配置
    server: {
      port: APP_PORT,
      proxy: {
        '/api': {
          target: API_BASE,
          changeOrigin: true,
          secure: false,
          rewrite: (p) => p.replace(/^\/api/, ''),
        },
      },
    },

    // 5. 插件列表，生产环境不启用 mock 服务
    plugins: [
      vue(),
      AutoImport({
        imports: ['vue', 'vue-router', 'pinia', '@vueuse/core'],
        dts: 'src/auto-imports.d.ts',
      }),
      Components({
        resolvers: [NaiveUiResolver()],
        dts: 'src/components.d.ts',
      }),
      createSvgIconsPlugin({
        iconDirs: [path.resolve(process.cwd(), 'src/assets/style/icons')],
        symbolId: 'icon-[dir]-[name]',
      }),
      // mock 插件只在 dev 模式下启用
      !isProd && viteMockServe({
        mockPath: 'src/mock',
        enable: true,
      }),
    ].filter(Boolean),

    // 6. 生产构建配置
    base: './',
    build: {
      sourcemap: !isProd,
      rollupOptions: {
        output: {
          // 保持默认分包策略，按需拆分
        },
      },
    },
  }
})
