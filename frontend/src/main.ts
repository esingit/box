import {createApp} from 'vue'
import App from './App.vue'
import router from './router/routes'
import {createPinia} from 'pinia'
import naive from 'naive-ui'

// 引入 echarts 注册模块
import {setupECharts} from './plugins/echarts'

// 全局样式
import './assets/styles/index.scss'

async function bootstrap() {
    const app = createApp(App)

    const pinia = createPinia()
    app.use(pinia)

    const {useUserStore} = await import('./store/userStore')
    const userStore = useUserStore()
    if (typeof userStore.hydrate === 'function') {
        await userStore.hydrate()
    }

    app.use(naive)
    app.use(router)

    setupECharts(app)

    app.mount('#app')
}

bootstrap()
