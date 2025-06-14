import { App } from 'vue'
import ECharts from 'vue-echarts'
import { use } from 'echarts/core'

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

// 注册 echarts 组件
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

export function setupECharts(app: App) {
    app.component('v-chart', ECharts)
}
