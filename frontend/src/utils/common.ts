// src/utils/common.ts
import { ref, computed, nextTick } from 'vue'
import * as echarts from 'echarts'

export function useDateRange() {
    const dateRange = ref('')

    const isDateRangeValid = computed(() => {
        if (!dateRange.value) return false
        const parts = dateRange.value.split('~').map(s => s.trim())
        return parts.length === 2 && parts[0] !== '' && parts[1] !== ''
    })

    const dateRangeDisplay = computed(() => {
        if (!isDateRangeValid.value) return '未设置'
        const { startDate, endDate } = parseDateRange(dateRange.value)
        return `${startDate} 至 ${endDate}`
    })

    function getDefaultRange(days: number = 30): string {
        const end = new Date()
        const start = new Date()
        start.setDate(end.getDate() - days + 1)

        const formatDate = (date: Date) => date.toISOString().slice(0, 10)
        return `${formatDate(start)} ~ ${formatDate(end)}`
    }

    function parseDateRange(range: string): { startDate: string; endDate: string } {
        const parts = range.split('~').map(s => s.trim())
        return {
            startDate: parts[0] || '',
            endDate: parts[1] || ''
        }
    }

    return {
        dateRange,
        isDateRangeValid,
        dateRangeDisplay,
        getDefaultRange,
        parseDateRange
    }
}

export function useChart() {
    const chartRef = ref<HTMLDivElement | null>(null)
    let chartInstance: echarts.ECharts | null = null

    async function initChart(options: echarts.EChartsOption): Promise<void> {
        if (!chartRef.value) {
            console.warn('Chart container not found')
            return
        }

        await nextTick()

        // ✅ Patch wheel listeners to passive to silence DevTools warnings
        patchEChartsWheelListener(chartRef.value)

        destroyChart()
        chartInstance = echarts.init(chartRef.value)
        chartInstance.setOption(options, true)
        chartInstance.resize()
    }

    function destroyChart(): void {
        if (chartInstance) {
            chartInstance.dispose()
            chartInstance = null
        }
    }

    function resizeChart(): void {
        if (chartInstance) {
            chartInstance.resize()
        }
    }

    return {
        chartRef,
        initChart,
        destroyChart,
        resizeChart
    }
}

/**
 * 修复 ECharts 默认绑定 wheel/mousewheel 事件没有 passive:true 的性能警告
 */
function patchEChartsWheelListener(el: HTMLElement) {
    const rawAddEventListener = el.addEventListener

    el.addEventListener = function (
        type: string,
        listener: EventListenerOrEventListenerObject,
        options?: boolean | AddEventListenerOptions
    ) {
        if (type === 'wheel' || type === 'mousewheel') {
            if (typeof options === 'boolean') {
                options = { passive: true }
            } else {
                options = Object.assign({}, options, { passive: true })
            }
        }
        rawAddEventListener.call(this, type, listener, options)
    }
}
