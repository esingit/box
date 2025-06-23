// src/main.ts
import { createApp } from 'vue'
import App from './App.vue'
import router from './router/routes'
import { createPinia } from 'pinia'

// 引入 ECharts 注册模块
import { setupECharts } from './plugins/echarts'

// 引入全局样式
import './assets/main.css'

// 导入路由守卫初始化
import { setupRouterGuard } from './router/guard'

async function bootstrap() {
    const app = createApp(App)
    const pinia = createPinia()
    app.use(pinia)

    // 预加载用户状态，确保 hydrate 完成后再挂载，避免登录态闪烁和异常
    const { useUserStore } = await import('./store/userStore')
    const userStore = useUserStore()
    const initialized = await userStore.hydrate()

    app.use(router)

    // 挂载路由守卫，必须在 app.use(router) 之后调用
    setupRouterGuard(router)

    // 如果未登录，强制跳转登录页，防止首次加载页面空白
    if (!initialized) {
        router.replace('/home')
    }

    setupECharts(app)

    app.mount('#app')
}

bootstrap().then(() => {})
