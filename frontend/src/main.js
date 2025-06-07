import { createApp } from 'vue'
import App from '@/App.vue'
import router from '@/router'
import { createPinia } from 'pinia'
import { useUserStore } from '@/stores/userStore'

import 'normalize.css'

// 主题变量和基础样式
import './assets/theme.css'
import './assets/base.css'
import './assets/layout.css'
import './assets/forms.css'
import './assets/components.css'
import './assets/buttons.css'
import './assets/inputs.css'
import './assets/modal.css'
import './assets/table.css'
import './assets/toast.css'
import './assets/pagination.css'
import './assets/actions.css'

async function initApp() {
  const pinia = createPinia()
  const app = createApp(App)
  app.use(pinia)
  app.use(router)

  // 初始化store
  const userStore = useUserStore()
  await userStore.hydrate()

  app.mount('#app')
}

initApp()