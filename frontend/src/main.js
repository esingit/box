import { createApp } from 'vue'
import App from '@/App.vue'
import router from '@/router'
import { createPinia } from 'pinia'

import 'normalize.css'

// 主题变量和基础样式
import '@/assets/theme.css'
import '@/assets/base.css'

// 组件样式
import '@/assets/components/button.css'
import '@/assets/components/form.css'
import '@/assets/components/modal.css'
import '@/assets/components/table.css'
import '@/assets/components/profile.css'
import '@/assets/components/header.css'
import '@/assets/components/notification.css'

// 页面专用样式
import '@/assets/sidebar.css'
import '@/assets/asset.css'
import '@/assets/fitness.css'

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.mount('#app')