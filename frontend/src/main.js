import { createApp } from 'vue'
import App from '@/App.vue'
import router from '@/router'
import { createPinia } from 'pinia'

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

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.mount('#app')