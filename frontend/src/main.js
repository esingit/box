import {createApp} from 'vue'
import App from '@/App.vue'
import router from '@/router'
import {createPinia} from 'pinia'
import {useUserStore} from '@/stores/userStore'
import naive from 'naive-ui'
import ECharts from 'vue-echarts'
import { use } from 'echarts/core'

// 按需引入 echarts 模块
import {
  CanvasRenderer
} from 'echarts/renderers'
import {
  BarChart,
  LineChart,
  PieChart
} from 'echarts/charts'
import {
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent
} from 'echarts/components'

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

  use([
    CanvasRenderer,
    BarChart,
    LineChart,
    PieChart,
    TitleComponent,
    TooltipComponent,
    LegendComponent,
    GridComponent
  ])
  app.component('v-chart', ECharts)

  app.mount('#app')
}

initApp()