import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { createSvgIconsPlugin } from 'vite-plugin-svg-icons'
import { viteMockServe } from 'vite-plugin-mock'
import tsconfigPaths from 'vite-tsconfig-paths'

export default defineConfig(({ command, mode }) => {
  const env = loadEnv(mode, process.cwd())

  const isDev = command === 'serve'
  const isProd = mode === 'production'

  const API_BASE = env.VITE_API_BASE_URL || ''
  const APP_PORT = Number(env.VITE_APP_PORT || 3000)
  const APP_TITLE = env.VITE_APP_TITLE || '企业后台'

  return {
    base: './',

    define: {
      __APP_TITLE__: JSON.stringify(APP_TITLE),
    },

    resolve: {
      alias: {
        '@': path.resolve(__dirname, 'src'),
        components: path.resolve(__dirname, 'src/components'),
      },
    },

    server: {
      port: APP_PORT,
      proxy: {
        '/api': {
          target: API_BASE,
          changeOrigin: true,
          secure: false,
        },
      },
    },

    plugins: [
      vue(),
      tsconfigPaths(),

      AutoImport({
        imports: ['vue', 'vue-router', 'pinia', '@vueuse/core'],
        dts: 'src/auto-imports.d.ts',
      }),

      Components({
        dts: 'src/components.d.ts',
      }),

      createSvgIconsPlugin({
        iconDirs: [path.resolve(process.cwd(), 'src/assets/style/icons')],
        symbolId: 'icon-[dir]-[name]',
      }),

      isDev &&
      viteMockServe({
        mockPath: 'src/mock',
        enable: true,
        watchFiles: true,
      }),
    ].filter(Boolean),

    build: {
      sourcemap: !isProd,
      rollupOptions: {
        output: {
          manualChunks: undefined,
        },
      },
    },
  }
})
