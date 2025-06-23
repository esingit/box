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

    // 预加载用户数据
    const { useUserStore } = await import('./store/userStore')
    const userStore = useUserStore()
    if (typeof userStore.hydrate === 'function') {
        await userStore.hydrate()
    }

    app.use(router)

    // 调用路由守卫初始化，务必在挂载前
    setupRouterGuard(router)

    setupECharts(app)

    app.mount('#app')
}

bootstrap()
