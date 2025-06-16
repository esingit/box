// src/main.ts
import { createApp } from 'vue'
import App from './App.vue'
import router from './router/routes'
import { createPinia } from 'pinia'

// 引入 ECharts 注册模块
import { setupECharts } from './plugins/echarts'

// 引入全局样式
import './assets/main.css'

async function bootstrap() {
    const app = createApp(App)

    // 状态管理
    const pinia = createPinia()
    app.use(pinia)

    // 预加载用户数据
    const { useUserStore } = await import('./store/userStore')
    const userStore = useUserStore()
    if (typeof userStore.hydrate === 'function') {
        await userStore.hydrate()
    }

    // 路由
    app.use(router)

    // ECharts 注册
    setupECharts(app)

    app.mount('#app')
}

bootstrap()
