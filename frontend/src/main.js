import {createApp} from 'vue'
import App from '@/App.vue'
import router from '@/router'
import {createPinia} from 'pinia'
import {useUserStore} from '@/stores/userStore'
import naive from 'naive-ui'
// 主题和基础样式
import './assets/theme.css'
import './assets/base.css'
// 布局
import './assets/layout.css'
// 组件样式
import './assets/components.css'
import './assets/buttons.css'
import './assets/inputs.css'
import './assets/forms.css'
import './assets/modal.css'
import './assets/table.css'
import './assets/toast.css'
import './assets/pagination.css'
import './assets/dashboard.css'
// 工具类
import './assets/utilities.css'

async function initApp() {
  const pinia = createPinia()
  const app = createApp(App)
  app.use(pinia)
  app.use(router)
  // 初始化store
  const userStore = useUserStore()
  await userStore.hydrate()
  app.use(naive)
  app.mount('#app')
}

initApp()