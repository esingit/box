// src/utils/common.ts
import {computed, nextTick, ref} from 'vue'
import type {EChartsCoreOption, EChartsType} from 'echarts/core'
import * as echarts from 'echarts/core'

export function useDateRange() {
    const dateRange = ref('')

    const isDateRangeValid = computed(() => {
        if (!dateRange.value) return false
        const parts = dateRange.value.split('~').map(s => s.trim())
        return parts.length === 2 && parts[0] !== '' && parts[1] !== ''
    })

    const dateRangeDisplay = computed(() => {
        if (!isDateRangeValid.value) return '未设置'
        const {startDate, endDate} = parseDateRange(dateRange.value)
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

// 🔥 全局 patch，在模块加载时就执行
let isGlobalPatchApplied = false

function applyGlobalEventListenerPatch() {
    if (isGlobalPatchApplied) return

    const originalAddEventListener = EventTarget.prototype.addEventListener

    EventTarget.prototype.addEventListener = function (
        type: string,
        listener: EventListenerOrEventListenerObject,
        options?: boolean | AddEventListenerOptions
    ) {
        // 🔥 对滚轮相关事件强制添加 passive: true
        if (type === 'wheel' || type === 'mousewheel' || type === 'touchmove') {
            if (typeof options === 'boolean') {
                options = {
                    capture: options,
                    passive: true
                }
            } else if (options && typeof options === 'object') {
                options = {
                    ...options,
                    passive: true
                }
            } else {
                options = {
                    passive: true
                }
            }
        }

        return originalAddEventListener.call(this, type, listener, options)
    }

    isGlobalPatchApplied = true
    console.log('🟢 全局 passive event listener patch 已应用')
}

export function useChart() {
    const chartRef = ref<HTMLDivElement | null>(null)
    let resizeObserver: ResizeObserver | null = null
    let chartInstance: EChartsType | null = null

    async function initChart(options: EChartsCoreOption): Promise<EChartsType | null> {
        if (!chartRef.value) {
            console.warn('Chart container not found')
            return null
        }

        applyGlobalEventListenerPatch()
        await nextTick()
        destroyChart()

        try {
            chartInstance = echarts.init(chartRef.value, undefined, {
                devicePixelRatio: window.devicePixelRatio || 1,
                renderer: 'canvas',
                useDirtyRect: true,
            })

            chartInstance.setOption(options, true)
            chartInstance.resize()
            setupResizeObserver()

            return chartInstance
        } catch (error) {
            console.error('ECharts 初始化失败:', error)
            return null
        }
    }

    function setupResizeObserver() {
        if (!chartRef.value || !chartInstance) return

        // 清理之前的观察器
        if (resizeObserver) {
            resizeObserver.disconnect()
        }

        // 🔥 使用 ResizeObserver 监听容器大小变化
        if (typeof ResizeObserver !== 'undefined') {
            resizeObserver = new ResizeObserver((entries) => {
                for (const entry of entries) {
                    if (entry.target === chartRef.value && chartInstance) {
                        // 🔥 使用 requestAnimationFrame 优化性能
                        requestAnimationFrame(() => {
                            if (chartInstance && !chartInstance.isDisposed()) {
                                chartInstance.resize()
                            }
                        })
                    }
                }
            })

            resizeObserver.observe(chartRef.value)
        }
    }

    function destroyChart(): void {
        if (resizeObserver) {
            resizeObserver.disconnect()
            resizeObserver = null
        }

        if (chartInstance) {
            if (!chartInstance.isDisposed()) {
                chartInstance.dispose()
            }
            chartInstance = null
        }
    }

    function resizeChart(): void {
        if (chartInstance && !chartInstance.isDisposed()) {
            requestAnimationFrame(() => {
                if (chartInstance && !chartInstance.isDisposed()) {
                    chartInstance.resize()
                }
            })
        }
    }

    return {
        chartRef,
        initChart,
        destroyChart,
        resizeChart
    }
}